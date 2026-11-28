package rogueknight.data;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import rogueknight.data.dto.BalanceData;
import rogueknight.data.dto.PieceStatsData;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceLoaderTest {

    @Test
    void malformedJsonIsReportedAsSetupIOException() {
        byte[] json = "{not-json".getBytes(StandardCharsets.UTF_8);
        IOException error = assertThrows(IOException.class, () ->
            new BalanceLoader().load(new ByteArrayInputStream(json), "/data/balance.json"));
        assertTrue(error.getMessage().contains("/data/balance.json"));
        assertTrue(error.getCause() instanceof com.google.gson.JsonParseException);
    }

    @Test
    void invalidBalanceIsReportedAsSetupIOException() {
        byte[] json = "{\"pieces\":{}}".getBytes(StandardCharsets.UTF_8);
        IOException error = assertThrows(IOException.class, () ->
            new BalanceLoader().load(new ByteArrayInputStream(json), "/data/balance.json"));
        assertTrue(error.getCause() instanceof IllegalArgumentException);
        assertTrue(error.getMessage().contains("KNIGHT"));
    }

    @Test
    void loadsBalanceFromResources() throws IOException {
        BalanceData balance = new BalanceLoader().load();

        assertNotNull(balance);
        assertNotNull(balance.pieces(), "Thiếu bảng pieces trong balance.json");

        PieceStatsData pawn = balance.pieces().get("PAWN");
        PieceStatsData knight = balance.pieces().get("KNIGHT");

        assertNotNull(pawn, "Thiếu thông số PAWN");
        assertNotNull(knight, "Thiếu thông số KNIGHT");

        assertEquals(Integer.valueOf(2), pawn.cycle());
        assertEquals(Integer.valueOf(5), knight.hp());
    }
}