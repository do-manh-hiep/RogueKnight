package rogueknight.core.input;

import rogueknight.battle.PieceType;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import rogueknight.core.view.PresentationPiece;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fixture implementation of BattleControllerPort used for UI development (T-02.02).
 * Labeled clearly as [FIXTURE UI DEMO - T-02.02] awaiting T-01.02 and T-02.01 integration.
 * <p>
 * Simulates Knight's movement rules:
 * - L-shape jump: (|dx|==1 && |dy|==2) || (|dx|==2 && |dy|==1)
 * - 1-step adjacent (8 directions): max(|dx|, |dy|) == 1
 * - Target must be empty (not occupied by Rock or another piece)
 * - Valid moves increment globalTurn by 1; invalid moves keep state unchanged.
 */
public final class FixtureBattleController implements BattleControllerPort {
    public static final String DEMO_LABEL = "[FIXTURE UI DEMO - T-02.02]";

    private int globalTurn = 1;
    private BoardPosition knightPosition;
    private final Set<BoardPosition> rocks = new HashSet<>();
    private final Map<BoardPosition, String> enemyPieces = new HashMap<>();

    public FixtureBattleController() {
        this(new BoardPosition(3, 3)); // Default d4
    }

    public FixtureBattleController(BoardPosition initialKnightPos) {
        this.knightPosition = Objects.requireNonNull(initialKnightPos, "initialKnightPos must not be null");

        // Map01 rock obstacles: c4, c5, d3, e6, f4, f5
        rocks.add(BoardPosition.fromNotation("c4"));
        rocks.add(BoardPosition.fromNotation("c5"));
        rocks.add(BoardPosition.fromNotation("d3"));
        rocks.add(BoardPosition.fromNotation("e6"));
        rocks.add(BoardPosition.fromNotation("f4"));
        rocks.add(BoardPosition.fromNotation("f5"));

        // Sample enemy pieces: Pawn at d7, King at d8, Rook at h8
        enemyPieces.put(BoardPosition.fromNotation("d7"), "Pawn");
        enemyPieces.put(BoardPosition.fromNotation("d8"), "King");
        enemyPieces.put(BoardPosition.fromNotation("h8"), "Rook");
    }

    @Override
    public BattleViewSnapshot snapshot() {
        Map<String, PresentationPiece> piecesById = new HashMap<>();
        piecesById.put("knight", new PresentationPiece("knight", PieceType.KNIGHT, 5, knightPosition));
        piecesById.put("fixture-pawn", new PresentationPiece(
            "fixture-pawn", PieceType.PAWN, 1, BoardPosition.fromNotation("d7")));
        piecesById.put("fixture-king", new PresentationPiece(
            "fixture-king", PieceType.KING, 5, BoardPosition.fromNotation("d8")));
        piecesById.put("fixture-rook", new PresentationPiece(
            "fixture-rook", PieceType.ROOK, 3, BoardPosition.fromNotation("h8")));

        return new BattleViewSnapshot(
            globalTurn,
            knightPosition,
            true,
            rocks,
            enemyPieces,
            legalMoves("knight"),
            "Turn " + globalTurn + " | Knight tại " + knightPosition.notation(),
            true,
            piecesById
        );
    }

    @Override
    public Set<BoardPosition> legalMoves(String actorId) {
        if (!"knight".equals(actorId)) return Set.of();

        Set<BoardPosition> legals = new HashSet<>();
        for (int col = 0; col < BoardPosition.BOARD_SIZE; col++) {
            for (int row = 0; row < BoardPosition.BOARD_SIZE; row++) {
                BoardPosition candidate = new BoardPosition(col, row);
                if (isValidKnightMove(knightPosition, candidate) == null) legals.add(candidate);
            }
        }
        return Collections.unmodifiableSet(legals);
    }

    @Override
    public MoveResult submitMove(String actorId, BoardPosition target) {
        var before = snapshot();
        if (actorId == null || !"knight".equals(actorId)) {
            return MoveResult.rejected("UNKNOWN_ACTOR", before);
        }
        if (target == null || !target.isValid()) {
            return MoveResult.rejected("OUT_OF_BOUNDS", before);
        }

        String errorReason = isValidKnightMove(knightPosition, target);
        if (errorReason != null) return MoveResult.rejected(errorReason, before);

        knightPosition = target;
        globalTurn++;
        return MoveResult.success(before, snapshot(), java.util.List.of());
    }

    @Override
    public java.util.List<ActionMode> availableModes() {
        return java.util.List.of(ActionMode.WAIT, ActionMode.MOVE);
    }

    @Override
    public MoveResult submitWait(String actorId) {
        var before = snapshot();
        if (!"knight".equals(actorId)) return MoveResult.rejected("UNKNOWN_ACTOR", before);
        globalTurn++;
        return MoveResult.success(before, snapshot(), java.util.List.of());
    }

    /** Returns null for a valid move, or an error reason for an invalid target. */
    private String isValidKnightMove(BoardPosition from, BoardPosition to) {
        if (!to.isValid()) return "OUT_OF_BOUNDS";
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
