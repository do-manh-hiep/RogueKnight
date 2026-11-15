package rogueknight.core.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardPositionTest {

    @Test
    void validPositionsWithinBounds() {
        assertTrue(new BoardPosition(0, 0).isValid()); // a1
        assertTrue(new BoardPosition(7, 7).isValid()); // h8
        assertTrue(new BoardPosition(3, 3).isValid()); // d4
        assertTrue(new BoardPosition(4, 5).isValid()); // e6
    }

    @Test
    void invalidPositionsOutOfBounds() {
        assertFalse(new BoardPosition(-1, 0).isValid());
        assertFalse(new BoardPosition(0, -1).isValid());
        assertFalse(new BoardPosition(8, 0).isValid());
        assertFalse(new BoardPosition(0, 8).isValid());
        assertFalse(new BoardPosition(10, 10).isValid());
    }

    @Test
    void notationConversionCorrect() {
        assertEquals("a1", new BoardPosition(0, 0).notation());
        assertEquals("h8", new BoardPosition(7, 7).notation());
        assertEquals("d1", new BoardPosition(3, 0).notation());
        assertEquals("d4", new BoardPosition(3, 3).notation());
        assertEquals("e6", new BoardPosition(4, 5).notation());
    }

    @Test
    void fromNotationParsingCorrect() {
        assertEquals(new BoardPosition(0, 0), BoardPosition.fromNotation("a1"));
        assertEquals(new BoardPosition(7, 7), BoardPosition.fromNotation("h8"));
        assertEquals(new BoardPosition(3, 0), BoardPosition.fromNotation("d1"));
        assertEquals(new BoardPosition(3, 3), BoardPosition.fromNotation("d4"));
        assertEquals(new BoardPosition(4, 5), BoardPosition.fromNotation("e6"));
    }

    @Test
    void invalidNotationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> BoardPosition.fromNotation("z9"));
        assertThrows(IllegalArgumentException.class, () -> BoardPosition.fromNotation(""));
        assertThrows(IllegalArgumentException.class, () -> BoardPosition.fromNotation("a"));
        assertThrows(IllegalArgumentException.class, () -> BoardPosition.fromNotation("a10"));
    }
}
