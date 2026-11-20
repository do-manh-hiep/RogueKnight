package rogueknight.data.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BalanceDataTest {

    @Test
    void readsStatsByPieceType() {
        String json = """
                {
                    "pieces": {
                        "PAWN": {"hp": 1, "damage": 1, "cycle": 2},
                        "KNIGHT": {"hp": 5, "swordDamage": 1, "captureDamage": 2}
                    }
                }
                """;

        BalanceData balance = new Gson().fromJson(json, BalanceData.class);

        PieceStatsData pawn = balance.pieces().get("PAWN");
        PieceStatsData knight = balance.pieces().get("KNIGHT");

        assertEquals(Integer.valueOf(2), pawn.cycle());
        assertEquals(Integer.valueOf(5), knight.hp());
        assertNull(knight.cycle());
    }
}