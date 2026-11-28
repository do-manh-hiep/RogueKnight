package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.battle.BattleController;
import rogueknight.ai.EnemyAI;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.Inventory;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.PieceType;
import rogueknight.battle.Direction;
import rogueknight.board.Position;
import java.util.List;
import java.util.Set;
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
 private static BattleController controller(BattleSnapshot.PieceView... pieces){return BattleController.fromSnapshot(new BattleSnapshot(1,BattlePhase.PLAYER_SELECT,BattleResult.ONGOING,Set.of(),List.of(pieces),Inventory.initial(EquipmentType.CANNON_MOVEMENT,EquipmentType.DIAGONAL_SKEWER)),new EnemyAI());}


    @Test
    void handlesKnightDeathGracefullyWithoutCrashing() throws IOException {
        BattleController controller = controller(new BattleSnapshot.PieceView("knight", PieceType.KNIGHT, 1, 0, 0, 0, Position.fromNotation("c1")),new BattleSnapshot.PieceView("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Position.fromNotation("c7"), Direction.SOUTH),new BattleSnapshot.PieceView("king-1", PieceType.KING, 5, 5, 5, 2, Position.fromNotation("h8")),new BattleSnapshot.PieceView("rook-1", PieceType.ROOK, 2, 3, 2, 3, Position.fromNotation("d6")));
        BattleControllerAdapter adapter = new BattleControllerAdapter(controller);
        adapter.snapshot();
        MoveResult action = adapter.submitMove("knight", BoardPosition.fromNotation("d3"));
        assertTrue(action.accepted());

        BattleViewSnapshot snapshot = adapter.snapshot();
        assertFalse(snapshot.piecesById().containsKey("knight"));
        assertTrue(snapshot.legalMoves().isEmpty());
    }

    private BattleControllerAdapter createAdapter() throws IOException {
        return new BattleControllerAdapter(new BattleFactory().createMap01Controller(
            EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER,
            rogueknight.action.EnemyPolicy.NONE));
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
            java.util.List.of(ActionMode.WAIT, ActionMode.MOVE, ActionMode.CANNON_MOVEMENT, ActionMode.DIAGONAL_SKEWER),
            adapter.availableModes()
        );

        adapter.submitAction("knight", ActionMode.CANNON_MOVEMENT, BoardPosition.fromNotation("d4"));

        assertEquals(
            java.util.List.of(ActionMode.WAIT, ActionMode.MOVE, ActionMode.DIAGONAL_SKEWER),
            adapter.availableModes()
        );
        assertEquals(ActionMode.DIAGONAL_SKEWER, adapter.availableModes().get(2));
    }

    @Test
    void waitIsAlwaysFirstAndSubmitsWithoutTarget() throws IOException {
        BattleControllerAdapter adapter = createAdapter();
        BattleViewSnapshot before = adapter.snapshot();

        assertEquals(ActionMode.WAIT, adapter.availableModes().get(0));
        MoveResult result = adapter.submitWait("knight");

        assertTrue(result.accepted());
        assertEquals(before.globalTurn() + 1, result.after().globalTurn());
        assertEquals(before.knightPosition(), result.after().knightPosition());
    }

    @Test
    void defaultMoveAttackClickOnAdjacentEnemySubmitsSword() {
        BattleController controller = controller(new BattleSnapshot.PieceView("knight", PieceType.KNIGHT, 3, 0, 0, 0, Position.fromNotation("d4")),new BattleSnapshot.PieceView("rook", PieceType.ROOK, 2, 3, 3, 1, Position.fromNotation("e5")),new BattleSnapshot.PieceView("king", PieceType.KING, 5, 5, 5, 2, Position.fromNotation("h8")));
        BattleControllerAdapter adapter = new BattleControllerAdapter(controller);
        var projection = new rogueknight.core.view.BoardProjection(0f, 0f, 48f);
        BoardInputHandler handler = new BoardInputHandler(projection, adapter);

        assertTrue(handler.getHighlightTargets().contains(BoardPosition.fromNotation("e5")));
        handler.handleWorldClick(projection.getCellCenterX(4), projection.getCellCenterY(4));

        assertEquals(1, controller.snapshot().pieceById("rook").orElseThrow().hp());
        assertTrue(handler.getFeedbackMessage().contains("Sword Attack"));
        assertEquals(ActionMode.MOVE, handler.getMode());
    }

    @Test
    void defaultMoveModeHighlightsAdjacentEnemySquaresForSwordAttack() throws IOException {
        BattleControllerAdapter adapter = createAdapter();
        var targets = adapter.legalTargets("knight", ActionMode.MOVE);
        // Opening board has enemies, and the combined default mode exposes all legal target types.
        assertTrue(targets.containsAll(adapter.legalMoves("knight")));
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

    @Test
    void waitResultExposesOrderedEnemyEventsToPresentationLayer() {
        BattleControllerAdapter adapter = new BattleControllerAdapter(controller(new BattleSnapshot.PieceView("knight", PieceType.KNIGHT, 5, 0, 0, 0, Position.fromNotation("d4")),new BattleSnapshot.PieceView("king", PieceType.KING, 5, 5, 5, 2, Position.fromNotation("h8")),new BattleSnapshot.PieceView("rook", PieceType.ROOK, 3, 3, 2, 1, Position.fromNotation("e4"))));

        MoveResult result = adapter.submitWait("knight");

        assertTrue(result.accepted());
        assertEquals(
            java.util.List.of(
                new rogueknight.action.BattleEvent.ActionPerformed(
                    rogueknight.action.ActionKind.WAIT, "knight"),
                new rogueknight.action.BattleEvent.TurnAdvanced(1, 2),
                new rogueknight.action.BattleEvent.ActorActivated("rook", Position.fromNotation("e4"), 2),
                new rogueknight.action.BattleEvent.ActionPerformed(
                    rogueknight.action.ActionKind.ATTACK, "rook"),
                new rogueknight.action.BattleEvent.PieceDamaged("knight", 1, 4)
            ),
            result.events()
        );
    }
}
