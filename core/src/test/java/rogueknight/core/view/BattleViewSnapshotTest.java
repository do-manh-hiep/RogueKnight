package rogueknight.core.view;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleViewSnapshotTest {

    @Test
    void withMessagePreservesKnightAliveState() {
        BoardPosition position = BoardPosition.fromNotation("d4");
        BattleViewSnapshot alive = new BattleViewSnapshot(
            1, position, true, Set.of(), Map.of(), Set.of(), "alive", false
        );
        BattleViewSnapshot dead = new BattleViewSnapshot(
            2, position, false, Set.of(), Map.of(), Set.of(), "dead", false
        );

        assertTrue(alive.withMessage("updated").knightAlive());
        assertFalse(dead.withMessage("updated").knightAlive());
    }
}
