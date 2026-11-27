package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import rogueknight.core.view.BoardProjection;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardInputHandlerTest {
    private final BoardProjection projection = new BoardProjection(128f, 48f, 48f);

    @Test
    void clickInsideValidSquareSubmitsExactlyOnceAndUpdatesState() {
        var controller = new CountingController(new FixtureBattleController(new BoardPosition(3, 3)));
        var handler = new BoardInputHandler(projection, controller);

        // Click center of c6 (col 2, row 5):
        float c6CenterX = projection.getCellCenterX(2);
        float c6CenterY = projection.getCellCenterY(5);

        boolean handled = handler.handleWorldClick(c6CenterX, c6CenterY);

        assertTrue(handled);
        assertEquals(1, controller.submissionCount());
        assertEquals(2, handler.getCurrentSnapshot().globalTurn());
        assertEquals(BoardPosition.fromNotation("c6"), handler.getCurrentSnapshot().knightPosition());
        assertTrue(handler.getFeedbackMessage().contains("Di chuyển Knight"));
    }

    @Test
    void clickOutsideBoardDoesNotSubmitAndUpdatesMessage() {
        var controller = new CountingController(new FixtureBattleController(new BoardPosition(3, 3)));
        var handler = new BoardInputHandler(projection, controller);
        var initialSnapshot = handler.getCurrentSnapshot();

        boolean handled = handler.handleWorldClick(10f, 10f);

        assertFalse(handled);
        assertEquals(0, controller.submissionCount());
        assertEquals(initialSnapshot, handler.getCurrentSnapshot());
        assertEquals("Click ngoài bàn cờ", handler.getFeedbackMessage());
    }

    @Test
    void rejectedMovePreservesSnapshotAndDoesNotCreateFakeState() {
        var controller = new CountingController(new FixtureBattleController(new BoardPosition(3, 3)));
        var handler = new BoardInputHandler(projection, controller);
        var initialSnapshot = handler.getCurrentSnapshot();

        // Click center of e6 (col 4, row 5) which is a Rock in Map01
        float e6CenterX = projection.getCellCenterX(4);
        float e6CenterY = projection.getCellCenterY(5);

        boolean handled = handler.handleWorldClick(e6CenterX, e6CenterY);

        assertTrue(handled);
        assertEquals(1, controller.submissionCount());
        assertEquals(initialSnapshot, handler.getCurrentSnapshot());
        assertEquals(1, handler.getCurrentSnapshot().globalTurn());
        assertEquals(new BoardPosition(3, 3), handler.getCurrentSnapshot().knightPosition());
        assertTrue(handler.getFeedbackMessage().contains("Nước đi không hợp lệ"));
        assertTrue(handler.getFeedbackMessage().contains("OCCUPIED_BY_ROCK"));
    }

    @Test
    void waitActionIsFirstAndExecutesImmediatelyWithoutBoardClick() {
        var controller = new CountingController(new FixtureBattleController(new BoardPosition(3, 3)));
        var handler = new BoardInputHandler(projection, controller);
        int beforeTurn = handler.getCurrentSnapshot().globalTurn();

        assertEquals(List.of(ActionMode.WAIT, ActionMode.MOVE), handler.getAvailableModes());
        handler.selectMode(ActionMode.WAIT);

        assertEquals(beforeTurn + 1, handler.getCurrentSnapshot().globalTurn());
        assertEquals(ActionMode.MOVE, handler.getMode());
        assertEquals(1, controller.submissionCount());
    }

    @Test
    void clickingKnightItselfSelectsWithoutSubmittingMove() {
        var controller = new CountingController(new FixtureBattleController(new BoardPosition(3, 3)));
        var handler = new BoardInputHandler(projection, controller);

        float d4CenterX = projection.getCellCenterX(3);
        float d4CenterY = projection.getCellCenterY(3);

        boolean handled = handler.handleWorldClick(d4CenterX, d4CenterY);

        assertTrue(handled);
        assertEquals(0, controller.submissionCount());
        assertEquals(new BoardPosition(3, 3), handler.getCurrentSnapshot().knightPosition());
        assertTrue(handler.getFeedbackMessage().contains("Đã chọn Knight"));
    }

    @Test
    void selectingByCurrentIndexUsesCompactedEquipmentList() throws Exception {
        var battle = new rogueknight.data.BattleFactory().createMap01(
            rogueknight.battle.EquipmentType.CANNON_MOVEMENT,
            rogueknight.battle.EquipmentType.DIAGONAL_SKEWER
        );
        var adapter = new BattleControllerAdapter(new rogueknight.action.BattleController(battle));
        var handler = new BoardInputHandler(projection, adapter);

        assertEquals(List.of(
            ActionMode.WAIT, ActionMode.MOVE, ActionMode.CANNON_MOVEMENT, ActionMode.DIAGONAL_SKEWER
        ), handler.getAvailableModes());

        var cannonResult = adapter.submitAction(
            "knight", ActionMode.CANNON_MOVEMENT, BoardPosition.fromNotation("d4"));
        assertTrue(cannonResult.accepted());
        assertEquals(List.of(
            ActionMode.WAIT, ActionMode.MOVE, ActionMode.DIAGONAL_SKEWER
        ), handler.getAvailableModes());

        handler.selectAction(2);
        assertEquals(ActionMode.DIAGONAL_SKEWER, handler.getMode());
        handler.selectAction(2);
        assertEquals(ActionMode.MOVE, handler.getMode());
    }

    @Test
    void selectingOutOfRangeActionIndexDoesNotChangeSelectedMode() {
        var handler = new BoardInputHandler(
            projection,
            new FixtureBattleController(new BoardPosition(3, 3))
        );

        handler.selectAction(2);

        assertEquals(ActionMode.MOVE, handler.getMode());
        assertTrue(handler.getFeedbackMessage().contains("không khả dụng"));
    }

    private static final class CountingController implements BattleControllerPort {
        private final BattleControllerPort delegate;
        private int submissions;

        private CountingController(BattleControllerPort delegate) {
            this.delegate = delegate;
        }

        int submissionCount() {
            return submissions;
        }

        @Override
        public BattleViewSnapshot snapshot() {
            return delegate.snapshot();
        }

        @Override
        public Set<BoardPosition> legalMoves(String actorId) {
            return delegate.legalMoves(actorId);
        }

        @Override
        public MoveResult submitMove(String actorId, BoardPosition target) {
            submissions++;
            return delegate.submitMove(actorId, target);
        }

        @Override
        public List<ActionMode> availableModes() {
            return delegate.availableModes();
        }

        @Override
        public Set<BoardPosition> legalTargets(String actorId, ActionMode mode) {
            return delegate.legalTargets(actorId, mode);
        }

        @Override
        public MoveResult submitAction(String actorId, ActionMode mode, BoardPosition target) {
            submissions++;
            return delegate.submitAction(actorId, mode, target);
        }

        @Override
        public MoveResult submitWait(String actorId) {
            submissions++;
            return delegate.submitWait(actorId);
        }
    }

    @Test
    void waitEventsCanBeDrainedOnceInDomainOrder() {
        var battle = new rogueknight.battle.BattleState(
            java.util.Set.of(),
            rogueknight.battle.Inventory.initial(
                rogueknight.battle.EquipmentType.CANNON_MOVEMENT,
                rogueknight.battle.EquipmentType.DIAGONAL_SKEWER
            )
        );
        battle.addPiece(new rogueknight.battle.PieceState(
            "knight", rogueknight.battle.PieceType.KNIGHT, 5, 0, 0, 0
        ), rogueknight.board.Position.fromNotation("d4"));
        battle.addPiece(new rogueknight.battle.PieceState(
            "king", rogueknight.battle.PieceType.KING, 5, 5, 5, 2
        ), rogueknight.board.Position.fromNotation("h8"));
        battle.addPiece(new rogueknight.battle.PieceState(
            "rook", rogueknight.battle.PieceType.ROOK, 3, 3, 2, 1
        ), rogueknight.board.Position.fromNotation("e4"));
        var adapter = new BattleControllerAdapter(new rogueknight.action.BattleController(
            battle, new rogueknight.action.EnemyTurnRunner(new rogueknight.ai.EnemyAI())
        ));
        var handler = new BoardInputHandler(projection, adapter);

        handler.selectMode(ActionMode.WAIT);

        var events = handler.drainEvents();
        assertTrue(events.stream().anyMatch(event ->
            event instanceof rogueknight.action.BattleEvent.ActorActivated activated
                && activated.actorId().equals("rook")));
        assertTrue(events.stream().anyMatch(event ->
            event instanceof rogueknight.action.BattleEvent.PieceDamaged damaged
                && damaged.pieceId().equals("knight")));
        assertTrue(handler.drainEvents().isEmpty());
    }

}
