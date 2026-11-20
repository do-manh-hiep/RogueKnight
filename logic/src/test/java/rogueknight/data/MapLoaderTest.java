package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PieceData;
import rogueknight.data.dto.PositionData;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MapLoaderTest {

    @Test
    void loadsMap01FromResources() throws IOException {
        MapData map = new MapLoader().loadMap01();

        assertNotNull(map);
        assertEquals(Integer.valueOf(8), map.width());
        assertEquals(Integer.valueOf(8), map.height());

        assertNotNull(map.rocks());
        assertEquals(6, map.rocks().size());

        assertNotNull(map.pieces());
        assertEquals(6, map.pieces().size());
    }

    @Test
    void loadsCorrectStartingPositions() throws IOException {
        MapData map = new MapLoader().loadMap01();

        Set<String> actualRocks = new HashSet<>();
        for (PositionData rock : map.rocks()) {
            actualRocks.add(rock.column() + "," + rock.row());
        }

        Set<String> expectedRocks = Set.of(
                "2,3", "2,4", "3,2", "4,5", "5,3", "5,4"
        );

        assertEquals(expectedRocks, actualRocks, "Tọa độ các tảng đá");

        Set<String> actualPieces = new HashSet<>();
        for (PieceData piece : map.pieces()) {
            actualPieces.add(
                    piece.id() + ":" + piece.type() + ":" + piece.column() + "," + piece.row()
            );
        }

        Set<String> expectedPieces = Set.of(
            "knight:KNIGHT:3,0",
            "pawn-1:PAWN:2,6",
            "pawn-2:PAWN:3,6",
            "rook-1:ROOK:7,7",
            "bishop-1:BISHOP:2,7",
            "king-1:KING:3,7"
        );

        assertEquals(expectedPieces, actualPieces, "Vị trí các quân ban đầu");
    }

    
}