package rogueknight.action;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleState;
import rogueknight.battle.Direction;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PawnEnemyPhaseTest {

    private final PawnEnemyPhase enemyPhase = new PawnEnemyPhase();

    private static Inventory loadout() {
        return Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
    }

    @Test
    @DisplayName("Pawn moves 1 step and updates nextActivationTurn by cycle when due")
    void pawnMovesWhenDue() {
        BattleState state = new BattleState(Set.of(), loadout());
        // Global turn advanced to 2
        state.advanceTurn();
        assertEquals(2, state.globalTurn());

        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(3, 0));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 6));

        List<BattleEvent> events = enemyPhase.run(state);

        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof BattleEvent.PieceMoved);
        BattleEvent.PieceMoved moved = (BattleEvent.PieceMoved) events.get(0);
        assertEquals("pawn-1", moved.pieceId());
        assertEquals(new Position(3, 6), moved.from());
        assertEquals(new Position(3, 5), moved.to());

        PieceState updatedPawn = state.pieceState("pawn-1").orElseThrow();
        assertEquals(4, updatedPawn.nextActivationTurn()); // 2 + 2 = 4
        assertEquals(Direction.SOUTH, updatedPawn.currentDirection());
    }

    @Test
    @DisplayName("Pawn attacks Knight when in range, does not move, and updates schedule")
    void pawnAttacksKnightWithoutMoving() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.advanceTurn(); // Turn 2

        // Pawn at (3, 2) facing SOUTH, Knight at (3, 1)
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(3, 1));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 2));

        List<BattleEvent> events = enemyPhase.run(state);

        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof BattleEvent.PieceDamaged);
        assertEquals(4, state.pieceState("knight").orElseThrow().hp());
        assertEquals(new Position(3, 2), state.positionOf("pawn-1").orElseThrow()); // Still at (3, 2)
        assertEquals(4, state.pieceState("pawn-1").orElseThrow().nextActivationTurn());
    }

    @Test
    @DisplayName("Pawn does not attack after moving even if landing adjacent to Knight")
    void pawnDoesNotAttackAfterMove() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.advanceTurn(); // Turn 2

        // Knight at (2, 4). Pawn at (3, 6) facing SOUTH.
        // If Pawn moves to (3, 5), Knight at (2, 4) is now in Pawn's attack cone (SOUTH_WEST).
        // But Pawn MUST NOT attack in the same turn it moved!
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(2, 4));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 6));

        List<BattleEvent> events = enemyPhase.run(state);

        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof BattleEvent.PieceMoved);
        // Knight still at full 5 HP
        assertEquals(5, state.pieceState("knight").orElseThrow().hp());
    }

    @Test
    @DisplayName("Pawn reaching row 0 promotes to Queen with nextActivationTurn = currentTurn + 5 and no attack")
    void pawnPromotesAtRow0WithCorrectTimeline() {
        BattleState state = new BattleState(Set.of(), loadout());
        // Set turn to 6
        for (int i = 1; i < 6; i++) {
            state.advanceTurn();
        }
        assertEquals(6, state.globalTurn());

        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(7, 0));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 6, 1, Direction.SOUTH), new Position(3, 1));

        List<BattleEvent> events = enemyPhase.run(state);

        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof BattleEvent.PieceMoved);
        assertTrue(events.get(1) instanceof BattleEvent.PiecePromoted);

        PieceState queen = state.pieceState("pawn-1").orElseThrow();
        assertEquals(PieceType.QUEEN, queen.type());
        assertEquals(2, queen.hp());
        assertEquals(4, queen.cycle());
        assertEquals(11, queen.nextActivationTurn()); // Turn 6 + 5 = 11 (DEC-05)
        assertNull(queen.currentDirection());

        // Knight takes no damage
        assertEquals(5, state.pieceState("knight").orElseThrow().hp());
    }

    @Test
    @DisplayName("Killing Knight ends battle with DEFEAT and stops subsequent enemies")
    void killingKnightEndsGameAndStopsPhase() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.advanceTurn(); // Turn 2

        // Knight at (3, 1) with 1 HP
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 1, 0, 0, 0), new Position(3, 1));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 2));
        state.addPiece(new PieceState("pawn-2", PieceType.PAWN, 1, 2, 2, 2, Direction.SOUTH), new Position(4, 6));

        List<BattleEvent> events = enemyPhase.run(state);

        assertEquals(BattleResult.DEFEAT, state.result());
        assertTrue(state.pieceState("knight").isEmpty());

        // pawn-2 should NOT have moved because game already ended
        assertEquals(new Position(4, 6), state.positionOf("pawn-2").orElseThrow());
    }
}
