package rogueknight.battle;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rogueknight.action.BattleEvent;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PawnPromotionApplierTest {

    private static Inventory loadout() {
        return Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
    }

    @Test
    @DisplayName("Promotes Pawn at row 0 to Queen with HP=2, cycle=4, nextActivationTurn=T+5, keeping id and spawnOrder")
    void promotesPawnAtRow0() {
        BattleState state = new BattleState(Set.of(), loadout());
        PieceState pawn = new PieceState("pawn-1", PieceType.PAWN, 1, 2, 6, 3, Direction.SOUTH);
        Position pos = new Position(3, 0); // d1 (row 0)
        state.addPiece(pawn, pos);

        int currentTurn = 6;
        BattleEvent.PiecePromoted event = PawnPromotionApplier.promote(state, "pawn-1", currentTurn);

        assertEquals("pawn-1", event.pieceId());
        assertEquals(PieceType.PAWN, event.fromType());
        assertEquals(PieceType.QUEEN, event.toType());
        assertEquals(pos, event.at());

        PieceState queen = state.pieceState("pawn-1").orElseThrow();
        assertEquals(PieceType.QUEEN, queen.type());
        assertEquals(2, queen.hp());
        assertEquals(4, queen.cycle());
        assertEquals(11, queen.nextActivationTurn()); // 6 + 5 = 11 (DEC-05)
        assertEquals(3, queen.spawnOrder());
        assertNull(queen.currentDirection());
    }

    @Test
    @DisplayName("Throws exception when attempting to promote Pawn not at row 0")
    void rejectsPromotionNotAtRow0() {
        BattleState state = new BattleState(Set.of(), loadout());
        PieceState pawn = new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH);
        state.addPiece(pawn, new Position(3, 1)); // row 1, not row 0

        assertThrows(IllegalStateException.class, () ->
                PawnPromotionApplier.promote(state, "pawn-1", 2)
        );
    }
}
