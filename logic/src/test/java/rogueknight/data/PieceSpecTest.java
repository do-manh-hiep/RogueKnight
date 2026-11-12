package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.data.dto.PieceStatsData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PieceSpecTest {

    @Test
    void convertsPawnStats() {
        PieceStatsData stats = new PieceStatsData(1, 1, 2, null, null);

        PieceSpec spec = PieceSpec.from(stats);

        assertEquals(1, spec.hp());
        assertEquals(Integer.valueOf(1), spec.damage());
        assertEquals(Integer.valueOf(2), spec.cycle());
    }

    @Test
    void convertsKnightStatsWithoutCycle() {
        PieceStatsData stats = new PieceStatsData(5, null, null, 1, 2);

        PieceSpec spec = PieceSpec.from(stats);

        assertEquals(5, spec.hp());
        assertNull(spec.cycle());
        assertEquals(Integer.valueOf(1), spec.swordDamage());
        assertEquals(Integer.valueOf(2), spec.captureDamage());
    }
}