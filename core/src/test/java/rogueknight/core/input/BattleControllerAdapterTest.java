package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.action.BattleController;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import rogueknight.data.BattleFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T-04.01 / T-04.02 UI slice: equipment modes go through the same port and rules as Move. */
class BattleControllerAdapterEquipmentTest {

    private BattleControllerAdapter createAdapter() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );
        return new BattleControllerAdapter(new BattleController(battle));
    }

    @Test
    void cannonMovementTargetsOnMap01_matchOpening() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        assertEquals(
            java.util.Set.of(
                BoardPosition.fromNotation("d4"),
                BoardPosition.fromNotation("d5"),
                BoardPosition.fromNotation("d6"),
                BoardPosition.fromNotation("d7")
            ),
            adapter.legalTargets("knight", ActionMode.CANNON_MOVEMENT)
        );
    }

    @Test
    void cannonCaptureOnD7_acceptedAndConsumesChargeOnce() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        MoveResult result = adapter.submitAction(
            "knight", ActionMode.CANNON_MOVEMENT, BoardPosition.fromNotation("d7"));

        assertTrue(result.accepted());
        assertEquals(BoardPosition.fromNotation("d7"), result.after().knightPosition());
        assertTrue(adapter.legalTargets("knight", ActionMode.CANNON_MOVEMENT).isEmpty());
    }

    @Test
    void invalidCannonTargetD8_rejectedAndStateUnchanged() throws IOException {
        BattleControllerAdapter adapter = createAdapter();
        BattleViewSnapshot before = adapter.snapshot();

        MoveResult result = adapter.submitAction(
            "knight", ActionMode.CANNON_MOVEMENT, BoardPosition.fromNotation("d8"));

        assertFalse(result.accepted());
        assertEquals(before, adapter.snapshot());
    }

    @Test
    void unownedCannonAttack_rejectedWithEquipmentNotOwned() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        MoveResult result = adapter.submitAction(
            "knight", ActionMode.CANNON_ATTACK, BoardPosition.fromNotation("d7"));

        assertFalse(result.accepted());
        assertEquals("EQUIPMENT_NOT_OWNED", result.errorReason());
    }

    @Test
    void handlerHighlightsFollowSelectedMode_andResetAfterAcceptedAction() throws IOException {
        BattleControllerAdapter adapter = createAdapter();
        var projection = new rogueknight.core.view.BoardProjection(0f, 0f, 48f);
        BoardInputHandler handler = new BoardInputHandler(projection, adapter);

        handler.selectMode(ActionMode.CANNON_MOVEMENT);
        assertEquals(adapter.legalTargets("knight", ActionMode.CANNON_MOVEMENT), handler.getHighlightTargets());
        assertNotEquals(handler.getCurrentSnapshot().legalMoves(), handler.getHighlightTargets());

        handler.handleWorldClick(projection.getCellCenterX(3), projection.getCellCenterY(3)); // d4
        assertEquals(ActionMode.MOVE, handler.getMode());
        assertEquals(BoardPosition.fromNotation("d4"), handler.getCurrentSnapshot().knightPosition());
    }

    @Test
    void availableModes_listOwnedEquipment_andDropUsedOnes() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        assertEquals(
            java.util.List.of(ActionMode.MOVE, ActionMode.CANNON_MOVEMENT, ActionMode.DIAGONAL_SKEWER),
            adapter.availableModes()
        );

        adapter.submitAction("knight", ActionMode.CANNON_MOVEMENT, BoardPosition.fromNotation("d4"));

        assertEquals(
            java.util.List.of(ActionMode.MOVE, ActionMode.DIAGONAL_SKEWER),
            adapter.availableModes()
        );
    }

    @Test
    void handlerIgnoresUsedOrUnownedMode() throws IOException {
        BattleControllerAdapter adapter = createAdapter();
        var projection = new rogueknight.core.view.BoardProjection(0f, 0f, 48f);
        BoardInputHandler handler = new BoardInputHandler(projection, adapter);

        handler.selectMode(ActionMode.CANNON_ATTACK);
        assertEquals(ActionMode.MOVE, handler.getMode());

        handler.selectMode(ActionMode.CANNON_MOVEMENT);
        handler.handleWorldClick(projection.getCellCenterX(3), projection.getCellCenterY(3)); // d4
        handler.selectMode(ActionMode.CANNON_MOVEMENT);
        assertEquals(ActionMode.MOVE, handler.getMode());
    }
}
