package rogueknight.data.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MapDataTest {

    @Test
    void readsMapWithRocksAndPieces() {
        String json = """
                {
                    "width": 8,
                    "height": 8,
                    "rocks": [
                        {
                            "column": 2,
                            "row": 3
                        }
                    ],
                    "pieces": [
                        {
                            "id": "knight",
                            "type": "KNIGHT",
                            "column": 3,
                            "row": 0,
                            "spawnOrder": 0
                        }
                    ]
                }
                """;

        Gson gson = new Gson();
        MapData map = gson.fromJson(json, MapData.class);

        assertNotNull(map);
        assertEquals(Integer.valueOf(8), map.width());
        assertEquals(Integer.valueOf(8), map.height());

        assertNotNull(map.rocks());
        assertEquals(1, map.rocks().size());

        PositionData firstRock = map.rocks().get(0);
        assertNotNull(firstRock);
        assertEquals(Integer.valueOf(2), firstRock.column());
        assertEquals(Integer.valueOf(3), firstRock.row());

        assertNotNull(map.pieces());
        assertEquals(1, map.pieces().size());

        PieceData firstPiece = map.pieces().get(0);
        assertNotNull(firstPiece);
        assertEquals("knight", firstPiece.id());
        assertEquals("KNIGHT", firstPiece.type());
        assertEquals(Integer.valueOf(3), firstPiece.column());
        assertEquals(Integer.valueOf(0), firstPiece.row());
        assertEquals(Integer.valueOf(0), firstPiece.spawnOrder());
    }
}