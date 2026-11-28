package rogueknight.battle;

import rogueknight.action.ActionError;
import rogueknight.action.ActionKind;
import rogueknight.action.ActionRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;
import rogueknight.board.Position;

/*
 * EnemyRules.java  |  T-03.02  |  TD: 2.4, 4.7 (C-01, C-02), R-E02..R-E04, R-B03..R-B04
 *
 * Luật Move/Attack của mọi Enemy và validate request của Enemy (Player dùng KnightRules).
 * - Rook trượt orthogonal, Bishop trượt diagonal, Queen = Rook + Bishop; dừng trước blocker đầu tiên
 *   (Rock, Enemy khác hoặc Knight). King đi một ô tám hướng. Đích Move luôn là ô trống.
 * - Attack chỉ ô kề: Rook kề orthogonal, Bishop kề diagonal, King/Queen kề tám hướng; damage 1.
 * - Pawn (T-03.03): Move một ô tám hướng (DEC-01; BFS trong PawnPathfinder chọn ô nào), Attack ba ô
 *   theo currentDirection và ±45° (PawnAttackRule). Hướng và phong cấp do ActionResolver áp dụng.
 * - Thuần đọc trên StateView, nên AI dùng được cùng bộ luật trên HypotheticalView (R-TECH02).
 * - Hình học khai báo bằng dữ liệu (bảng GEOMETRY), không switch(type) rải rác.
 * Từ khóa tìm kiếm: moveDestinations, attackCells, canAttackKnight, knightPosition, validate.
 */
public final class EnemyRules {

    /** balance.json: every Enemy type has damage = 1 in the baseline. */
    public static final int ENEMY_DAMAGE = 1;

    private static final List<Direction> ORTHOGONAL =
            List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final List<Direction> DIAGONAL =
            List.of(Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST, Direction.NORTH_WEST);
    private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());

    /**
     * slide: rays until a blocker; step: exactly one square; attack: adjacent squares in these directions.
     * {@code attackFollowsFacing}: the attack cone comes from the piece's currentDirection (Pawn) instead.
     */
    private record Geometry(List<Direction> slide, List<Direction> step, List<Direction> attack,
                            boolean attackFollowsFacing) {
        Geometry(List<Direction> slide, List<Direction> step, List<Direction> attack) {
            this(slide, step, attack, false);
        }
    }

    private static final Map<PieceType, Geometry> GEOMETRY = geometryTable();

    private static final Comparator<Position> BOARD_ORDER =
            Comparator.comparingInt(Position::column).thenComparingInt(Position::row);

    private EnemyRules() { }

    private static Map<PieceType, Geometry> geometryTable() {
        Map<PieceType, Geometry> table = new EnumMap<>(PieceType.class);
        table.put(PieceType.ROOK, new Geometry(ORTHOGONAL, List.of(), ORTHOGONAL));
        table.put(PieceType.BISHOP, new Geometry(DIAGONAL, List.of(), DIAGONAL));
        table.put(PieceType.QUEEN, new Geometry(ALL_DIRECTIONS, List.of(), ALL_DIRECTIONS));
        table.put(PieceType.KING, new Geometry(List.of(), ALL_DIRECTIONS, ALL_DIRECTIONS));
        table.put(PieceType.PAWN, new Geometry(List.of(), ALL_DIRECTIONS, List.of(), true));
        return table;
    }

    /** Rook, Bishop, King and Queen follow the generic Attack / Move-then-Attack flow (TD 5.4). */
    public static boolean isGenericEnemy(PieceType type) {
        return type != null && type != PieceType.PAWN && GEOMETRY.containsKey(type);
    }

    /** Empty squares the Enemy may move to, sorted by column then row. Empty list if it cannot move. */
    public static List<Position> moveDestinations(StateView view, String enemyId) {
        Optional<Geometry> geometry = geometryOf(view, enemyId);
        Optional<Position> from = view.positionOf(enemyId);
        if (geometry.isEmpty() || from.isEmpty()) return List.of();

        List<Position> destinations = new ArrayList<>();
        for (Direction direction : geometry.get().slide()) {
            Position current = from.get();
            while (true) {
                Optional<Position> next = step(current, direction);
                if (next.isEmpty() || !view.isEmpty(next.get())) break;
                destinations.add(next.get());
                current = next.get();
            }
        }
        for (Direction direction : geometry.get().step()) {
            step(from.get(), direction).filter(view::isEmpty).ifPresent(destinations::add);
        }
        destinations.sort(BOARD_ORDER);
        return List.copyOf(destinations);
    }

    /** Current attack area, including empty squares (tooltip range); sorted by column then row. */
    public static List<Position> attackCells(StateView view, String enemyId) {
        Optional<Geometry> geometry = geometryOf(view, enemyId);
        Optional<Position> from = view.positionOf(enemyId);
        if (geometry.isEmpty() || from.isEmpty()) return List.of();

        List<Position> cells = new ArrayList<>();
        if (geometry.get().attackFollowsFacing()) {
            Direction facing = view.pieceState(enemyId).orElseThrow().currentDirection();
            cells.addAll(PawnAttackRule.attackTargets(from.get(), facing));
        } else {
            for (Direction direction : geometry.get().attack()) {
                step(from.get(), direction).ifPresent(cells::add);
            }
        }
        cells.sort(BOARD_ORDER);
        return List.copyOf(cells);
    }

    /** Position of the live Knight, if it is still on the Board. */
    public static Optional<Position> knightPosition(StateView view) {
        return view.livePieces().stream()
                .filter(piece -> piece.type() == PieceType.KNIGHT)
                .findFirst()
                .flatMap(knight -> view.positionOf(knight.id()));
    }

    /** Whether the Enemy can attack the Knight from where it stands right now. */
    public static boolean canAttackKnight(StateView view, String enemyId) {
        Optional<Position> knight = knightPosition(view);
        return knight.isPresent() && attackCells(view, enemyId).contains(knight.get());
    }

    /** Validates one Enemy MOVE or ATTACK on the current state; read-only (C-02). */
    static ValidationOutcome validate(StateView view, ActionRequest request) {
        if (request == null || request.actorId() == null || request.kind() == null) {
            return ValidationOutcome.invalid(ActionError.INVALID_PAYLOAD);
        }
        Optional<PieceState> actor = view.pieceState(request.actorId());
        Optional<Position> from = view.positionOf(request.actorId());
        if (actor.isEmpty() || from.isEmpty()) return ValidationOutcome.invalid(ActionError.UNKNOWN_ACTOR);
        if (actor.get().type() == PieceType.KNIGHT) return ValidationOutcome.invalid(ActionError.NOT_ENEMY_ACTOR);
        if (!GEOMETRY.containsKey(actor.get().type())) return ValidationOutcome.invalid(ActionError.UNSUPPORTED_ACTION);
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);

        Position target = request.target();
        return switch (request.kind()) {
            case MOVE -> validateMove(view, request.actorId(), from.get(), target);
            case ATTACK -> validateAttack(view, request.actorId(), from.get(), target);
            default -> ValidationOutcome.invalid(ActionError.UNSUPPORTED_ACTION);
        };
    }

    private static ValidationOutcome validateMove(StateView view, String actorId, Position from, Position target) {
        if (view.isRock(target)) return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_ROCK);
        if (view.pieceAt(target).isPresent()) return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_PIECE);
        if (!moveDestinations(view, actorId).contains(target)) {
            return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        }
        return ValidationOutcome.valid(new ActionPlan(ActionKind.MOVE, actorId, from, target, null));
    }

    private static ValidationOutcome validateAttack(StateView view, String actorId, Position from, Position target) {
        if (!attackCells(view, actorId).contains(target)) return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        Optional<String> victimId = view.pieceAt(target);
        if (victimId.isEmpty()) return ValidationOutcome.invalid(ActionError.NO_TARGET_PIECE);
        PieceState victim = view.pieceState(victimId.get()).orElseThrow();
        if (victim.type() != PieceType.KNIGHT) return ValidationOutcome.invalid(ActionError.NOT_ENEMY_TARGET);
        return ValidationOutcome.valid(new ActionPlan(ActionKind.ATTACK, actorId, from, target, victim.id()));
    }

    private static Optional<Geometry> geometryOf(StateView view, String enemyId) {
        if (enemyId == null) return Optional.empty();
        return view.pieceState(enemyId).map(piece -> GEOMETRY.get(piece.type()));
    }

    private static Optional<Position> step(Position from, Direction direction) {
        int column = from.column() + direction.deltaColumn();
        int row = from.row() + direction.deltaRow();
        return Position.isInBounds(column, row) ? Optional.of(new Position(column, row)) : Optional.empty();
    }
}
