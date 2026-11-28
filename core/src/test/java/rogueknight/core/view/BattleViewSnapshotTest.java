package rogueknight.core.view;

import org.junit.jupiter.api.Test;
import rogueknight.battle.PieceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleViewSnapshotTest {

    @Test
    void derivesKnightPresenceAndEnemyOccupancyFromCanonicalPieceMap() {
        BoardPosition knightPosition = BoardPosition.fromNotation("d4");
        BoardPosition enemyPosition = BoardPosition.fromNotation("h8");
        PresentationPiece knight = new PresentationPiece(
            "knight", PieceType.KNIGHT, 5, knightPosition);
        PresentationPiece enemy = new PresentationPiece(
            "rook-1", PieceType.ROOK, 3, enemyPosition);

        BattleViewSnapshot alive = new BattleViewSnapshot(
            1, knightPosition, Set.of(), Set.of(),
            Map.of("knight", knight, "rook-1", enemy)
        );
        BattleViewSnapshot dead = new BattleViewSnapshot(
            2, knightPosition, Set.of(), Set.of(),
            Map.of("rook-1", enemy)
        );

        assertTrue(alive.piecesById().containsKey("knight"));
        assertFalse(dead.piecesById().containsKey("knight"));
        assertTrue(alive.rocks().isEmpty());
        assertTrue(alive.hasEnemyAt(enemyPosition));
        assertFalse(alive.hasEnemyAt(knightPosition));
        assertFalse(dead.hasEnemyAt(null));
    }

    @Test
    void constructorDefensivelyCopiesAndExposesImmutableCollections() {
        BoardPosition position = BoardPosition.fromNotation("d4");
        BoardPosition rock = BoardPosition.fromNotation("a1");
        BoardPosition legalMove = BoardPosition.fromNotation("e6");
        PresentationPiece knight = new PresentationPiece(
            "knight", PieceType.KNIGHT, 5, position);
        Set<BoardPosition> rocks = new HashSet<>(Set.of(rock));
        Set<BoardPosition> legalMoves = new HashSet<>(Set.of(legalMove));
        Map<String, PresentationPiece> pieces = new HashMap<>(Map.of("knight", knight));

        BattleViewSnapshot snapshot = new BattleViewSnapshot(
            1, position, rocks, legalMoves, pieces
        );

        rocks.clear();
        legalMoves.clear();
        pieces.clear();

        assertTrue(snapshot.rocks().contains(rock));
        assertTrue(snapshot.piecesById().containsKey("knight"));
        assertTrue(snapshot.legalMoves().contains(legalMove));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rocks().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.piecesById().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.legalMoves().clear());
    }
}
