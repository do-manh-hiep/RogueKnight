package rogueknight.core.input;

import rogueknight.board.Position;
import rogueknight.battle.PieceType;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.PresentationPiece;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test fixture for isolating BoardInputHandler behavior from the production battle controller.
 * Simulates a small fixed board and the subset of Knight actions needed by input tests.
 * <p>
 * Simulates Knight's movement rules:
 * - L-shape jump: (|dx|==1 && |dy|==2) || (|dx|==2 && |dy|==1)
 * - 1-step adjacent (8 directions): max(|dx|, |dy|) == 1
 * - Target must be empty (not occupied by Rock or another piece)
 * - Valid moves increment globalTurn by 1; invalid moves keep state unchanged.
 */
public final class StubBattleController implements BattleControllerPort {
    private int globalTurn = 1;
    private Position knightPosition;
    private final Set<Position> rocks = new HashSet<>();
    private final Map<Position, String> enemyPieces = new HashMap<>();

    public StubBattleController() {
        this(new Position(3, 3)); // Default d4
    }

    public StubBattleController(Position initialKnightPos) {
        this.knightPosition = Objects.requireNonNull(initialKnightPos, "initialKnightPos must not be null");

        // Map01 rock obstacles: c4, c5, d3, e6, f4, f5
        rocks.add(Position.fromNotation("c4"));
        rocks.add(Position.fromNotation("c5"));
        rocks.add(Position.fromNotation("d3"));
        rocks.add(Position.fromNotation("e6"));
        rocks.add(Position.fromNotation("f4"));
        rocks.add(Position.fromNotation("f5"));

        // Sample enemy pieces: Pawn at d7, King at d8, Rook at h8
        enemyPieces.put(Position.fromNotation("d7"), "Pawn");
        enemyPieces.put(Position.fromNotation("d8"), "King");
        enemyPieces.put(Position.fromNotation("h8"), "Rook");
    }

    @Override
    public BattleViewSnapshot snapshot() {
        Map<String, PresentationPiece> piecesById = new HashMap<>();
        piecesById.put("knight", new PresentationPiece("knight", PieceType.KNIGHT, 5, knightPosition));
        piecesById.put("fixture-pawn", new PresentationPiece(
            "fixture-pawn", PieceType.PAWN, 1, Position.fromNotation("d7")));
        piecesById.put("fixture-king", new PresentationPiece(
            "fixture-king", PieceType.KING, 5, Position.fromNotation("d8")));
        piecesById.put("fixture-rook", new PresentationPiece(
            "fixture-rook", PieceType.ROOK, 3, Position.fromNotation("h8")));

        return new BattleViewSnapshot(
            globalTurn,
            knightPosition,
            rocks,
            legalMoves("knight"),
            piecesById
        );
    }

    @Override
    public Set<Position> legalMoves(String actorId) {
        if (!"knight".equals(actorId)) return Set.of();

        Set<Position> legals = new HashSet<>();
        for (int col = 0; col < Position.BOARD_SIZE; col++) {
            for (int row = 0; row < Position.BOARD_SIZE; row++) {
                Position candidate = new Position(col, row);
                if (isValidKnightMove(knightPosition, candidate) == null) legals.add(candidate);
            }
        }
        return Collections.unmodifiableSet(legals);
    }

    @Override
    public Set<Position> legalTargets(String actorId, ActionMode mode) {
        return mode == ActionMode.MOVE ? legalMoves(actorId) : Set.of();
    }

    @Override
    public UiActionResult submitAction(String actorId, ActionMode mode, Position target) {
        if (mode == ActionMode.WAIT) return submitWait(actorId);
        if (mode != ActionMode.MOVE) return UiActionResult.rejected("UNSUPPORTED_ACTION", snapshot());
        var before = snapshot();
        if (actorId == null || !"knight".equals(actorId)) {
            return UiActionResult.rejected("UNKNOWN_ACTOR", before);
        }
        if (target == null) {
            return UiActionResult.rejected("OUT_OF_BOUNDS", before);
        }
        String errorReason = isValidKnightMove(knightPosition, target);
        if (errorReason != null) return UiActionResult.rejected(errorReason, before);
        knightPosition = target;
        globalTurn++;
        return UiActionResult.success(before, snapshot(), java.util.List.of());
    }

    @Override
    public java.util.List<ActionMode> availableModes() {
        return java.util.List.of(ActionMode.WAIT, ActionMode.MOVE);
    }

    @Override
    public UiActionResult submitWait(String actorId) {
        var before = snapshot();
        if (!"knight".equals(actorId)) return UiActionResult.rejected("UNKNOWN_ACTOR", before);
        globalTurn++;
        return UiActionResult.success(before, snapshot(), java.util.List.of());
    }

    /** Returns null for a valid move, or an error reason for an invalid target. */
    private String isValidKnightMove(Position from, Position to) {
        if (to == null) return "OUT_OF_BOUNDS";
        if (from.equals(to)) return "SAME_SQUARE";
        if (rocks.contains(to)) return "OCCUPIED_BY_ROCK";
        if (enemyPieces.containsKey(to)) return "OCCUPIED_BY_PIECE";

        int dx = Math.abs(to.column() - from.column());
        int dy = Math.abs(to.row() - from.row());
        boolean isLMove = (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        boolean isAdjacent = (dx <= 1 && dy <= 1) && (dx != 0 || dy != 0);
        return isLMove || isAdjacent ? null : "INVALID_GEOMETRY";
    }
}
