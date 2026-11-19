package rogueknight.battle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PieceStateTest {
    @Test void validatesIdentityAndNonNegativeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new PieceState(" ", PieceType.KNIGHT, 5, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PieceState("K1", null, 5, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PieceState("K1", PieceType.KNIGHT, -1, 0, 0, 0));
    }
    @Test void hpUpdateReturnsNewValueAndClampsAtZero() {
        PieceState original = new PieceState("K1", PieceType.KNIGHT, 5, 0, 0, 0);
        assertEquals(2, original.withHp(2).hp());
        assertEquals(0, original.withHp(-10).hp());
        assertEquals(5, original.hp());
    }
}
