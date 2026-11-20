package rogueknight.data.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PieceDataTest {
    @Test
    void pieceDataTest(){
        String json = """
            {
                "id": "knight",
                "type": "KNIGHT",
                "column": 2,
                "row": 3,
                "spawnOrder": 0 
            }
            """;
        Gson gson = new Gson();
        PieceData piece = gson.fromJson(json, PieceData.class);
        
        assertEquals("knight",piece.id());
        assertEquals("KNIGHT",piece.type());
        assertEquals(Integer.valueOf(2),piece.column());
        assertEquals(Integer.valueOf(3),piece.row());
        assertEquals(Integer.valueOf(0),piece.spawnOrder());
    }
}
