package rogueknight.core.view;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable view model representing a read-only snapshot of the battle board.
 * Used exclusively by UI / rendering to display the current game state without modifying it.
 */
public record BattleViewSnapshot(
    int globalTurn,
    BoardPosition knightPosition,
    boolean knightAlive,
    Set<BoardPosition> rocks,
    Map<BoardPosition, String> enemyPieces,
    Set<BoardPosition> legalMoves,
    String statusMessage,
    boolean isFixtureDemo
) {
    public BattleViewSnapshot {
        Objects.requireNonNull(knightPosition, "knightPosition must not be null");
        rocks = Set.copyOf(rocks != null ? rocks : Set.of());
        enemyPieces = Map.copyOf(enemyPieces != null ? enemyPieces : Map.of());
        legalMoves = Set.copyOf(legalMoves != null ? legalMoves : Set.of());
        statusMessage = statusMessage != null ? statusMessage : "";
    }

    /** Creates a copy with updated display text while preserving battle state. */
    public BattleViewSnapshot withMessage(String newMessage) {
        return new BattleViewSnapshot(
            globalTurn,
            knightPosition,
            knightAlive,
            rocks,
            enemyPieces,
            legalMoves,
            newMessage,
            isFixtureDemo
        );
    }
}
