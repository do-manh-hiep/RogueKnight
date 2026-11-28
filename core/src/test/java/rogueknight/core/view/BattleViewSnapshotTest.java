package rogueknight.core.view;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleViewSnapshotTest {

    @Test
    void withMessagePreservesBattleProjection() {
        BoardPosition position = BoardPosition.fromNotation("d4");
        BoardPosition rock = BoardPosition.fromNotation("a1");
        BoardPosition enemy = BoardPosition.fromNotation("h8");
        BoardPosition legalMove = BoardPosition.fromNotation("e6");

        BattleViewSnapshot alive = new BattleViewSnapshot(
            1, position, true, Set.of(rock), Map.of(enemy, "ROOK"), Set.of(legalMove), "alive", Map.of()
        );
        BattleViewSnapshot dead = new BattleViewSnapshot(
            2, position, false, Set.of(), Map.of(), Set.of(), "dead", Map.of()
        );

        BattleViewSnapshot updatedAlive = alive.withMessage("updated");
        assertTrue(updatedAlive.knightAlive());
        assertTrue(updatedAlive.rocks().contains(rock));
        assertTrue(updatedAlive.enemyPieces().containsKey(enemy));
        assertTrue(updatedAlive.legalMoves().contains(legalMove));
        assertFalse(dead.withMessage("updated").knightAlive());
    }

    @Test
    void constructorDefensivelyCopiesAndExposesImmutableCollections() {
        BoardPosition position = BoardPosition.fromNotation("d4");
        BoardPosition rock = BoardPosition.fromNotation("a1");
        BoardPosition enemy = BoardPosition.fromNotation("h8");
        BoardPosition legalMove = BoardPosition.fromNotation("e6");
        Set<BoardPosition> rocks = new HashSet<>(Set.of(rock));
        Map<BoardPosition, String> enemies = new HashMap<>(Map.of(enemy, "ROOK"));
        Set<BoardPosition> legalMoves = new HashSet<>(Set.of(legalMove));

        BattleViewSnapshot snapshot = new BattleViewSnapshot(
            1, position, true, rocks, enemies, legalMoves, "", Map.of()
        );

        rocks.clear();
        enemies.clear();
        legalMoves.clear();

        assertTrue(snapshot.rocks().contains(rock));
        assertTrue(snapshot.enemyPieces().containsKey(enemy));
        assertTrue(snapshot.legalMoves().contains(legalMove));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rocks().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.enemyPieces().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.legalMoves().clear());
    }
}
