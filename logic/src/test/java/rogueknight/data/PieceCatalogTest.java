package rogueknight.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PieceCatalogTest {

    @Test
    void looksUpSpecsByPieceType() throws IOException {
        PieceCatalog catalog = new PieceCatalog(new BalanceLoader().load());

        PieceSpec pawn = catalog.get("PAWN");
        PieceSpec queen = catalog.get("QUEEN");

        assertEquals(1, pawn.hp());
        assertEquals(Integer.valueOf(2), pawn.cycle());
        assertEquals(2, queen.hp());
        assertEquals(Integer.valueOf(4), queen.cycle());
    }

    @Test
    void rejectsUnknownPieceType() throws IOException {
        PieceCatalog catalog = new PieceCatalog(new BalanceLoader().load());

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.get("DRAGON")
        );
    }
}