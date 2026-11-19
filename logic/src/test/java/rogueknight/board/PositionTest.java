package rogueknight.board;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {
    @Test void acceptsBoardCorners() {
        assertEquals(new Position(0, 0), Position.fromNotation("a1"));
        assertEquals(new Position(7, 7), Position.fromNotation("h8"));
    }
    @Test void notationRoundTrips() {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Position p = new Position(col, row);
                assertEquals(p, Position.fromNotation(p.toNotation()));
            }
    }
    @Test void rejectsOutOfBoundsCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 8));
    }
    @Test void rejectsMalformedNotation() {
        for (String value : new String[]{null, "", "a", "a10", "A1", "i1", "a0", "z9"})
            assertThrows(IllegalArgumentException.class, () -> Position.fromNotation(value), String.valueOf(value));
    }
}
