package rogueknight.core.view;

import java.util.Collections;
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
    Set<BoardPosition> rocks,
    Map<BoardPosition, String> enemyPieces,
    Set<BoardPosition> legalMoves,
    String statusMessage,
    boolean isFixtureDemo
) {
    public BattleViewSnapshot {
        Objects.requireNonNull(knightPosition, "knightPosition must not be null");
        rocks = Collections.unmodifiableSet(Set.copyOf(rocks != null ? rocks : Set.of()));
        enemyPieces = Collections.unmodifiableMap(Map.copyOf(enemyPieces != null ? enemyPieces : Map.of()));
        legalMoves = Collections.unmodifiableSet(Set.copyOf(legalMoves != null ? legalMoves : Set.of()));
        statusMessage = statusMessage != null ? statusMessage : "";
    }

    /**
     * Helper to create a new snapshot with an updated status message.
     */
    public BattleViewSnapshot withMessage(String newMessage) {
        return new BattleViewSnapshot(
            globalTurn,
            knightPosition,
            rocks,
            enemyPieces,
            legalMoves,
            newMessage,
            isFixtureDemo
        );
    }
}
