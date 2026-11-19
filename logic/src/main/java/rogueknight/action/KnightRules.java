package rogueknight.action;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/*
 * KnightRules.java  |  T-02.01  |  TD: 2.3, 4.7-4.8
 *
 * Luật Knight Move, chỉ đọc state (không mutate):
 * - Hình L: |dx|,|dy| = (1,2) hoặc (2,1); nhảy qua vật cản trung gian.
 * - Hoặc một ô kề theo tám hướng.
 * - Ô đích phải trống: không Rock, không quân. Normal Move không thành Capture.
 * Thứ tự kiểm tra: actor -> biên -> geometry -> Rock -> quân. Geometry trước occupancy
 * để ô hiện tại của Knight và ô xa báo INVALID_GEOMETRY nhất quán.
 * legalMoveTargets() và validateMove() dùng chung isMoveGeometry/occupancyError,
 * nên highlight UI luôn khớp validation.
 * Từ khóa tìm kiếm: isMoveGeometry, legalMoveTargets, validateMove, occupancyError.
 */
public final class KnightRules {

    private KnightRules() {
    }

    public static boolean isMoveGeometry(Position from, Position to) {
        int dx = Math.abs(to.column() - from.column());
        int dy = Math.abs(to.row() - from.row());
        boolean lShape = (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        boolean adjacent = Math.max(dx, dy) == 1;
        return lShape || adjacent;
    }

    /** Tập ô đích hợp lệ cho Move của actorId; rỗng nếu actor không phải Knight còn trên Board. */
    public static Set<Position> legalMoveTargets(BattleState state, String actorId) {
        Optional<Position> from = knightPosition(state, actorId);
        if (from.isEmpty()) {
            return Set.of();
        }
        Set<Position> targets = new HashSet<>();
        for (int row = 0; row < Position.BOARD_SIZE; row++) {
            for (int column = 0; column < Position.BOARD_SIZE; column++) {
                Position to = new Position(column, row);
                if (isMoveGeometry(from.get(), to) && occupancyError(state, to).isEmpty()) {
                    targets.add(to);
                }
            }
        }
        return Set.copyOf(targets);
    }

    static ValidationOutcome validateMove(BattleState state, ActionRequest request) {
        Optional<PieceState> actor = request.actorId() == null
                ? Optional.empty() : state.pieceState(request.actorId());
        if (actor.isEmpty()) {
            return ValidationOutcome.invalid(ActionError.UNKNOWN_ACTOR);
        }
        if (actor.get().type() != PieceType.KNIGHT) {
            return ValidationOutcome.invalid(ActionError.NOT_PLAYER_ACTOR);
        }
        if (!request.targetInBounds()) {
            return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);
        }
        Position from = state.positionOf(request.actorId()).orElseThrow();
        Position to = request.target();
        if (!isMoveGeometry(from, to)) {
            return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        }
        Optional<ActionError> occupied = occupancyError(state, to);
        if (occupied.isPresent()) {
            return ValidationOutcome.invalid(occupied.get());
        }
        return ValidationOutcome.valid(new MovePlan(request.actorId(), from, to));
    }

    private static Optional<ActionError> occupancyError(BattleState state, Position to) {
        if (state.isRock(to)) {
            return Optional.of(ActionError.OCCUPIED_BY_ROCK);
        }
        if (state.pieceAt(to).isPresent()) {
            return Optional.of(ActionError.OCCUPIED_BY_PIECE);
        }
        return Optional.empty();
    }

    private static Optional<Position> knightPosition(BattleState state, String actorId) {
        if (actorId == null) {
            return Optional.empty();
        }
        return state.pieceState(actorId)
                .filter(piece -> piece.type() == PieceType.KNIGHT)
                .flatMap(piece -> state.positionOf(actorId));
    }
}
