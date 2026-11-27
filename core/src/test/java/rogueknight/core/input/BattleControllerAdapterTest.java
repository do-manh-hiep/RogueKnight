package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.action.BattleController;
import rogueknight.battle.BattleState;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
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

    @Test
    void handlesKnightDeathGracefullyWithoutCrashing() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );
        // Reduce knight HP to 1 and attack with Pawn
        battle.replacePiece(battle.pieceState("knight").orElseThrow().withHp(1));
        BattleController controller = new BattleController(battle,
            new rogueknight.action.EnemyTurnRunner(new rogueknight.ai.EnemyAI()));
        BattleControllerAdapter adapter = new BattleControllerAdapter(controller);

        // Knight moves near pawn so Pawn attacks Knight to 0 HP
        // Knight at d1 (3,0). Move to e3 (4,2).
        adapter.submitMove("knight", new BoardPosition(4, 2));

        // When knight reaches 0 HP, snapshot should indicate DEAD without throwing exception
        battle.removePiece("knight");
        battle.finish(rogueknight.battle.BattleResult.DEFEAT);

        BattleViewSnapshot snapshot = adapter.snapshot();
        assertTrue(snapshot.statusMessage().contains("DEAD"));
        assertTrue(snapshot.statusMessage().contains("DEFEAT"));
        assertTrue(snapshot.legalMoves().isEmpty());
    }

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
        BattleState state = new BattleState(java.util.Set.of(),
            Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 3, 0, 0, 0), Position.fromNotation("d4"));
        state.addPiece(new PieceState("rook", PieceType.ROOK, 2, 3, 3, 1), Position.fromNotation("e5"));
        state.addPiece(new PieceState("king", PieceType.KING, 5, 5, 5, 2), Position.fromNotation("h8"));
        BattleController controller = new BattleController(state);
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
        BattleState state = new BattleState(java.util.Set.of(), Inventory.initial(
            EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), Position.fromNotation("d4"));
        state.addPiece(new PieceState("king", PieceType.KING, 5, 5, 5, 2), Position.fromNotation("h8"));
        state.addPiece(new PieceState("rook", PieceType.ROOK, 3, 3, 2, 1), Position.fromNotation("e4"));
        BattleControllerAdapter adapter = new BattleControllerAdapter(new BattleController(
            state, new rogueknight.action.EnemyTurnRunner(new rogueknight.ai.EnemyAI())
        ));

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
