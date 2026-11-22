package rogueknight.core.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.action.BattleController;
import rogueknight.action.EnemyTurnRunner;
import rogueknight.ai.EnemyAI;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.core.view.BoardPosition;

/** T-03.01/T-03.02 UI slice: the adapter survives a Defeat caused by the real enemy turn. */
class BattleControllerAdapterEnemyTurnTest {

    @Test
    void knightKilledByEnemyTurnShowsDefeatWithoutCrashing() {
        BattleState state = new BattleState(Set.of(),
            Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 1, 0, 0, 0), Position.fromNotation("d4"));
        state.addPiece(new PieceState("rook-1", PieceType.ROOK, 2, 3, 2, 1), Position.fromNotation("d6"));
        state.addPiece(new PieceState("king-1", PieceType.KING, 5, 5, 5, 2), Position.fromNotation("h8"));
        BattleControllerAdapter adapter = new BattleControllerAdapter(
            new BattleController(state, new EnemyTurnRunner(new EnemyAI())));

        // Knight d4 -> d3; the Rook (due at Turn 2) moves d6 -> d4 and attacks d3: HP 1 -> 0.
        MoveResult result = adapter.submitAction("knight", ActionMode.MOVE, BoardPosition.fromNotation("d3"));

        assertTrue(result.accepted());
        assertEquals(BoardPosition.fromNotation("d3"), result.after().knightPosition());
        assertTrue(result.after().statusMessage().contains("DEAD"));
        assertTrue(result.after().statusMessage().contains("DEFEAT"));
        assertTrue(result.after().legalMoves().isEmpty());
        assertDoesNotThrow(adapter::snapshot);
    }
}
