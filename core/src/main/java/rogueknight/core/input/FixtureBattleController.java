package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;

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
        return new BattleViewSnapshot(
            globalTurn,
            knightPosition,
            Collections.unmodifiableSet(new HashSet<>(rocks)),
            Collections.unmodifiableMap(new HashMap<>(enemyPieces)),
            legalMoves("knight"),
            "Turn " + globalTurn + " | Knight tại " + knightPosition.notation(),
            true
        );
    }

    @Override
    public Set<BoardPosition> legalMoves(String actorId) {
        if (!"knight".equals(actorId)) {
            return Set.of();
        }
        Set<BoardPosition> legals = new HashSet<>();
        for (int col = 0; col < BoardPosition.BOARD_SIZE; col++) {
            for (int row = 0; row < BoardPosition.BOARD_SIZE; row++) {
                BoardPosition candidate = new BoardPosition(col, row);
                if (isValidKnightMove(knightPosition, candidate) == null) {
                    legals.add(candidate);
                }
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
        if (errorReason != null) {
            return MoveResult.rejected(errorReason, before);
        }

        // Apply mutation
        this.knightPosition = target;
        this.globalTurn++;

        var after = snapshot();
        return MoveResult.success(before, after);
    }

    /**
     * Validates whether moving from `from` to `to` is a valid Knight move.
     * Returns null if valid, or an error reason string if invalid.
     */
    private String isValidKnightMove(BoardPosition from, BoardPosition to) {
        if (!to.isValid()) {
            return "OUT_OF_BOUNDS";
        }
        if (from.equals(to)) {
            return "SAME_SQUARE";
        }
        if (rocks.contains(to)) {
            return "OCCUPIED_BY_ROCK";
        }
        if (enemyPieces.containsKey(to)) {
            return "OCCUPIED_BY_PIECE";
        }

        int dx = Math.abs(to.column() - from.column());
        int dy = Math.abs(to.row() - from.row());

        boolean isLMove = (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        boolean isAdjacent = (dx <= 1 && dy <= 1) && (dx != 0 || dy != 0);

        if (!isLMove && !isAdjacent) {
            return "INVALID_GEOMETRY";
        }

        return null;
    }
}
