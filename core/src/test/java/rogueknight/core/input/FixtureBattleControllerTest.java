package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.core.view.BoardPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureBattleControllerTest {

    @Test
    void validKnightLMoveAdvancesTurnAndUpdatesPosition() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var before = controller.snapshot();
        assertEquals(1, before.globalTurn());
        assertEquals(new BoardPosition(3, 3), before.knightPosition());

        // d4 -> e6 (dx=1, dy=2) has a Rock on e6 in Map01!
        // So d4 -> e6 is blocked by Rock!
        // Instead, d4 -> c6 (col 2, row 5): dx=1, dy=2, empty!
        BoardPosition c6 = BoardPosition.fromNotation("c6");
        MoveResult result = controller.submitMove("knight", c6);

        assertTrue(result.accepted());
        assertNull(result.errorReason());
        assertEquals(1, result.before().globalTurn());
        assertEquals(2, result.after().globalTurn());
        assertEquals(c6, result.after().knightPosition());
        assertEquals(2, controller.snapshot().globalTurn());
        assertEquals(c6, controller.snapshot().knightPosition());
    }

    @Test
    void validKnightAdjacentMoveAdvancesTurn() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        // d4 -> e5 (col 4, row 4): dx=1, dy=1 diagonal adjacent, empty!
        BoardPosition e5 = BoardPosition.fromNotation("e5");
        MoveResult result = controller.submitMove("knight", e5);

        assertTrue(result.accepted());
        assertNull(result.errorReason());
        assertEquals(2, result.after().globalTurn());
        assertEquals(e5, result.after().knightPosition());
    }

    @Test
    void rockObstacleBlocksMoveAndPreservesState() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var before = controller.snapshot();

        // e6 has a rock in Map01!
        BoardPosition e6 = BoardPosition.fromNotation("e6");
        MoveResult result = controller.submitMove("knight", e6);

        assertFalse(result.accepted());
        assertEquals("OCCUPIED_BY_ROCK", result.errorReason());
        assertEquals(before, result.before());
        assertEquals(before, result.after());
        assertEquals(before, controller.snapshot());
    }

    @Test
    void occupiedByPieceBlocksMoveAndPreservesState() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var before = controller.snapshot();

        // d7 has an enemy Pawn!
        BoardPosition d7 = BoardPosition.fromNotation("d7");
        MoveResult result = controller.submitMove("knight", d7);

        assertFalse(result.accepted());
        assertEquals("OCCUPIED_BY_PIECE", result.errorReason());
        assertEquals(before, result.before());
        assertEquals(before, result.after());
        assertEquals(before, controller.snapshot());
    }

    @Test
    void sameSquareMoveRejected() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        MoveResult result = controller.submitMove("knight", new BoardPosition(3, 3));

        assertFalse(result.accepted());
        assertEquals("SAME_SQUARE", result.errorReason());
        assertEquals(1, controller.snapshot().globalTurn());
    }

    @Test
    void invalidGeometryRejected() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        // d4 -> d6 (dx=0, dy=2) is orthogonal 2 steps, neither L nor 1-step adjacent
        BoardPosition d6 = BoardPosition.fromNotation("d6");
        MoveResult result = controller.submitMove("knight", d6);

        assertFalse(result.accepted());
        assertEquals("INVALID_GEOMETRY", result.errorReason());
        assertEquals(1, controller.snapshot().globalTurn());
    }

    @Test
    void unknownActorRejected() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        MoveResult result = controller.submitMove("enemy", BoardPosition.fromNotation("c6"));

        assertFalse(result.accepted());
        assertEquals("UNKNOWN_ACTOR", result.errorReason());
    }

    @Test
    void legalMovesOnlyContainValidSquares() {
        var controller = new FixtureBattleController(new BoardPosition(3, 3)); // d4
        var legals = controller.legalMoves("knight");

        assertNotNull(legals);
        assertTrue(legals.contains(BoardPosition.fromNotation("c6"))); // valid L
        assertTrue(legals.contains(BoardPosition.fromNotation("e5"))); // valid adjacent
        assertFalse(legals.contains(BoardPosition.fromNotation("e6"))); // rock!
        assertFalse(legals.contains(BoardPosition.fromNotation("d7"))); // pawn!
        assertFalse(legals.contains(BoardPosition.fromNotation("d6"))); // invalid geometry!
    }
}
