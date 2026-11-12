package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.data.dto.BalanceData;
import rogueknight.data.dto.PieceStatsData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceValidatorTest {

    @Test
    void acceptsBalanceFromResources() throws IOException {
        BalanceData balance = new BalanceLoader().load();

        assertDoesNotThrow(() -> new BalanceValidator().validate(balance));
    }

    @Test
    void rejectsPawnWithZeroCycle() throws IOException {
        BalanceData original = new BalanceLoader().load();

        Map<String, PieceStatsData> pieces = new HashMap<>(original.pieces());
        pieces.put("PAWN", new PieceStatsData(1, 1, 0, null, null));

        BalanceData invalid = new BalanceData(pieces);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new BalanceValidator().validate(invalid)
        );

        assertTrue(
                error.getMessage().contains("PAWN.cycle"),
                "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test
    void rejectsPawnWithoutDamage() throws IOException {
        BalanceData original = new BalanceLoader().load();
        Map<String, PieceStatsData> pieces = new HashMap<>(original.pieces());

        pieces.put("PAWN", new PieceStatsData(1, null, 2, null, null));
        BalanceData invalid = new BalanceData(pieces);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new BalanceValidator().validate(invalid)
        );

        assertTrue(
                error.getMessage().contains("PAWN.damage"),
                "Thông báo thực tế: " + error.getMessage()
        );
    }
}