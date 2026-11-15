package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.core.view.BoardPosition;
import rogueknight.core.view.BoardProjection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardInputHandlerTest {
    private final BoardProjection projection = new BoardProjection(128f, 48f, 48f);

    @Test
    void clickInsideValidSquareInvokesSubmitExactlyOnceAndUpdatesState() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var handler = new BoardInputHandler(projection, controller);

        assertEquals(0, handler.getSubmitInvocationCount());

        // Click center of c6 (col 2, row 5):
        float c6CenterX = projection.getCellCenterX(2);
        float c6CenterY = projection.getCellCenterY(5);

        boolean handled = handler.handleWorldClick(c6CenterX, c6CenterY);

        assertTrue(handled);
        assertEquals(1, handler.getSubmitInvocationCount());
        assertEquals(2, handler.getCurrentSnapshot().globalTurn());
        assertEquals(BoardPosition.fromNotation("c6"), handler.getCurrentSnapshot().knightPosition());
        assertTrue(handler.getFeedbackMessage().contains("Di chuyển Knight"));
    }

    @Test
    void clickOutsideBoardDoesNotInvokeSubmitAndUpdatesMessage() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3));
        var handler = new BoardInputHandler(projection, controller);
        var initialSnapshot = handler.getCurrentSnapshot();

        // Click out of bounds (10, 10)
        boolean handled = handler.handleWorldClick(10f, 10f);

        assertFalse(handled);
        assertEquals(0, handler.getSubmitInvocationCount());
        assertEquals(initialSnapshot, handler.getCurrentSnapshot());
        assertEquals("Click ngoài bàn cờ", handler.getFeedbackMessage());
    }

    @Test
    void rejectedMovePreservesSnapshotAndDoesNotCreateFakeState() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var handler = new BoardInputHandler(projection, controller);
        var initialSnapshot = handler.getCurrentSnapshot();

        // Click center of e6 (col 4, row 5) which is a Rock in Map01
        float e6CenterX = projection.getCellCenterX(4);
        float e6CenterY = projection.getCellCenterY(5);

        boolean handled = handler.handleWorldClick(e6CenterX, e6CenterY);

        assertTrue(handled);
        assertEquals(1, handler.getSubmitInvocationCount());
        // Verify snapshot is strictly identical to initialSnapshot
        assertEquals(initialSnapshot, handler.getCurrentSnapshot());
        assertEquals(1, handler.getCurrentSnapshot().globalTurn());
        assertEquals(new BoardPosition(3, 3), handler.getCurrentSnapshot().knightPosition());
        assertTrue(handler.getFeedbackMessage().contains("Nước đi không hợp lệ"));
        assertTrue(handler.getFeedbackMessage().contains("OCCUPIED_BY_ROCK"));
    }

    @Test
    void clickingKnightItselfSelectsWithoutSubmittingMove() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var handler = new BoardInputHandler(projection, controller);

        // Click center of d4 (where Knight is)
        float d4CenterX = projection.getCellCenterX(3);
        float d4CenterY = projection.getCellCenterY(3);

        boolean handled = handler.handleWorldClick(d4CenterX, d4CenterY);

        assertTrue(handled);
        assertEquals(0, handler.getSubmitInvocationCount());
        assertEquals(new BoardPosition(3, 3), handler.getSelectedPosition());
        assertTrue(handler.getFeedbackMessage().contains("Đã chọn Knight"));
    }
}
