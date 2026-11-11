package rogueknight.core.view;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardProjectionTest {
    private final float originX = 128f;
    private final float originY = 48f;
    private final float tileSize = 48f;
    private final BoardProjection projection = new BoardProjection(originX, originY, tileSize);

    @Test
    void mapsSquareCentersAccurately() {
        // a1 center: x = 128 + 24 = 152, y = 48 + 24 = 72
        float a1CenterX = projection.getCellCenterX(0);
        float a1CenterY = projection.getCellCenterY(0);
        assertEquals(152f, a1CenterX);
        assertEquals(72f, a1CenterY);

        Optional<BoardPosition> a1 = projection.worldToBoard(a1CenterX, a1CenterY);
        assertTrue(a1.isPresent());
        assertEquals(new BoardPosition(0, 0), a1.get());
        assertEquals("a1", a1.get().notation());

        // h8 center: x = 128 + 7.5 * 48 = 488, y = 48 + 7.5 * 48 = 408
        float h8CenterX = projection.getCellCenterX(7);
        float h8CenterY = projection.getCellCenterY(7);
        assertEquals(488f, h8CenterX);
        assertEquals(408f, h8CenterY);

        Optional<BoardPosition> h8 = projection.worldToBoard(h8CenterX, h8CenterY);
        assertTrue(h8.isPresent());
        assertEquals(new BoardPosition(7, 7), h8.get());
        assertEquals("h8", h8.get().notation());

        // d4 center: col 3, row 3
        float d4CenterX = projection.getCellCenterX(3);
        float d4CenterY = projection.getCellCenterY(3);
        Optional<BoardPosition> d4 = projection.worldToBoard(d4CenterX, d4CenterY);
        assertTrue(d4.isPresent());
        assertEquals(new BoardPosition(3, 3), d4.get());
        assertEquals("d4", d4.get().notation());
    }

    @Test
    void mapsSquareCornersAccurately() {
        // Bottom-left of a1 (just inside)
        Optional<BoardPosition> bl = projection.worldToBoard(128.01f, 48.01f);
        assertTrue(bl.isPresent());
        assertEquals(new BoardPosition(0, 0), bl.get());

        // Top-right of h8 (just inside)
        float maxX = originX + 8 * tileSize - 0.01f;
        float maxY = originY + 8 * tileSize - 0.01f;
        Optional<BoardPosition> tr = projection.worldToBoard(maxX, maxY);
        assertTrue(tr.isPresent());
        assertEquals(new BoardPosition(7, 7), tr.get());
    }

    @Test
    void returnsEmptyForPointsOutsideBoard() {
        // Left of board
        assertFalse(projection.worldToBoard(120f, 100f).isPresent());
        assertFalse(projection.worldToBoard(-10f, 100f).isPresent());

        // Below board
        assertFalse(projection.worldToBoard(200f, 30f).isPresent());
        assertFalse(projection.worldToBoard(200f, -5f).isPresent());

        // Right of board (>= 128 + 384 = 512)
        assertFalse(projection.worldToBoard(512f, 200f).isPresent());
        assertFalse(projection.worldToBoard(600f, 200f).isPresent());

        // Above board (>= 48 + 384 = 432)
        assertFalse(projection.worldToBoard(200f, 432f).isPresent());
        assertFalse(projection.worldToBoard(200f, 450f).isPresent());
    }

    @Test
    void cellBoundariesMatchFormulas() {
        assertEquals(128f, projection.getCellLeft(0));
        assertEquals(176f, projection.getCellLeft(1));
        assertEquals(48f, projection.getCellBottom(0));
        assertEquals(96f, projection.getCellBottom(1));
    }
}
