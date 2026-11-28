package rogueknight.battle;


import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PawnAttackRuleTest {

    private static Inventory loadout() {
        return Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
    }

    @Test
    @DisplayName("Pawn facing SOUTH has attack targets SOUTH, SOUTH_WEST, SOUTH_EAST")
    void attackTargetsSouth() {
        Position pos = new Position(3, 4); // d5
        List<Position> targets = PawnAttackRule.attackTargets(pos, Direction.SOUTH);

        assertEquals(3, targets.size());
        assertTrue(targets.contains(new Position(3, 3))); // SOUTH (d4)
        assertTrue(targets.contains(new Position(2, 3))); // SOUTH_WEST (c4)
        assertTrue(targets.contains(new Position(4, 3))); // SOUTH_EAST (e4)
    }

    @Test
    @DisplayName("Pawn at board edge clamps out-of-bounds targets")
    void attackTargetsClampedAtEdge() {
        Position pos = new Position(0, 4); // a5, facing WEST
        List<Position> targets = PawnAttackRule.attackTargets(pos, Direction.WEST);

        // WEST (-1, 0) is out of bounds, NORTH_WEST (-1, 1) out, SOUTH_WEST (-1, -1) out
        assertEquals(0, targets.size());
    }

    @Test
    @DisplayName("canAttackKnight returns true when Knight is in one of the 3 forward tiles")
    void canAttackKnightWhenInRange() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(3, 3));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 4));

        assertTrue(PawnAttackRule.canAttackKnight(state, "pawn-1"));
    }

    @Test
    @DisplayName("canAttackKnight returns false when Knight is outside attack range")
    void canAttackKnightWhenOutOfRange() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(3, 1));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 4));

        assertFalse(PawnAttackRule.canAttackKnight(state, "pawn-1"));
    }

    @Test
    @DisplayName("canAttackKnight finds the Knight by type, not by the fixed ID \"knight\"")
    void canAttackKnightWithAnyKnightId() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("K1", PieceType.KNIGHT, 5, 0, 0, 0), new Position(2, 3));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 4));

        assertTrue(PawnAttackRule.canAttackKnight(state, "pawn-1"));
    }

    // The Pawn attack itself (damage, Knight death, DEFEAT) now goes through EnemyRules + ActionResolver +
    // BattleController like every Enemy Action; see rogueknight.ai.PawnTurnTest.
}
