package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.data.dto.BalanceData;
import rogueknight.data.dto.PieceStatsData;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BalanceLoaderTest {

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