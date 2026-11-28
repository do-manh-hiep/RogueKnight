package rogueknight.core.view;

import java.util.Map;
import rogueknight.battle.PieceType;
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
    Set<BoardPosition> legalMoves,
    Map<String, PresentationPiece> piecesById
) {
    public BattleViewSnapshot {
        Objects.requireNonNull(knightPosition, "knightPosition must not be null");
        rocks = Set.copyOf(rocks != null ? rocks : Set.of());
        legalMoves = Set.copyOf(legalMoves != null ? legalMoves : Set.of());
        piecesById = Map.copyOf(piecesById != null ? piecesById : Map.of());
    }

    /** Returns whether an enemy occupies the given tile, using the canonical piece map. */
    public boolean hasEnemyAt(BoardPosition position) {
        return position != null && piecesById.values().stream()
            .anyMatch(piece -> piece.type() != PieceType.KNIGHT && piece.position().equals(position));
    }
}
