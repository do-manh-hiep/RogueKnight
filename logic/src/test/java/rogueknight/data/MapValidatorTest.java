package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PositionData;
import rogueknight.data.dto.PieceData;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapValidatorTest {

    @Test
    void acceptsEightByEightBoard() {
        MapData map = new MapData(8, 8, List.of(), List.of(                       
                new PieceData("knight", "KNIGHT", 3, 0, 0),
                new PieceData("king-1", "KING", 3, 7, 1)
        ));
        MapValidator validator = new MapValidator();

        assertDoesNotThrow(() -> validator.validate(map));
    }

    @Test
    void rejectsNineByEightBoard() {
        MapData map = new MapData(9, 8, List.of(), List.of());
        MapValidator validator = new MapValidator();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(map)
        );

        assertTrue(error.getMessage().contains("9x8"));
    }

    @Test 
    void acceptRock(){
        MapData map = new MapData(8,8, List.of(new PositionData(7,0)), List.of(                      
                new PieceData("knight", "KNIGHT", 3, 0, 0),
                new PieceData("king-1", "KING", 3, 7, 1)
        ));
        MapValidator validator = new MapValidator();
        assertDoesNotThrow(()-> validator.validate(map));
    }

    @Test 
    void rejectRock(){
        MapData map = new MapData(8, 8, List.of(new PositionData(8,0)), List.of());
        MapValidator validator = new MapValidator();

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(map)
        );

        assertTrue(
            error.getMessage().contains("rocks[0]"),
            "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test 
    void rejectsPieceOnRock() {
        MapData map = new MapData(
            8, 8,
            List.of(new PositionData(2,3)),
            List.of(new PieceData("knight", "KNIGHT", 2, 3, 0))
        );

        MapValidator validator = new MapValidator();

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(map)
        );

        assertTrue(
            error.getMessage().contains("chồng ô tại (2, 3)"),
            "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test
    void rejectsDupId(){
        MapData map = new MapData(
            8, 8,
            List.of(),
            List.of(
                new PieceData("pawn-1","PAWN", 2, 3, 1),
                new PieceData("pawn-1", "PAWN", 1, 2, 2)
            )
        );

        MapValidator validator = new MapValidator();

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(map)
        );

        assertTrue(
            error.getMessage().contains("có id bị trùng: pawn-1"),
            "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test
    void rejectsMisspelling(){
        MapData map = new MapData(
            8, 8,
            List.of(),
            List.of(
                new PieceData("pawn-1","PAWNN", 2, 3, 1)
            )
        );

        MapValidator validator = new MapValidator();

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(map)
        );

        assertTrue(
                error.getMessage().contains("type không hợp lệ: PAWNN"),
                "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test
    void rejectsPieceWithoutSpawnOrder() {
        MapData map = new MapData(
                8, 8,
                List.of(),
                List.of(new PieceData("knight", "KNIGHT", 3, 0, null))
        );

        MapValidator validator = new MapValidator();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(map)
        );

        assertTrue(
                error.getMessage().contains("pieces[0] thiếu spawnOrder"),
                "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test
    void rejectsMapWithoutKnight() {
        MapData map = new MapData(
                8, 8,
                List.of(),
                List.of(new PieceData("king-1", "KING", 3, 7, 1))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new MapValidator().validate(map)
        );

        assertTrue(
                error.getMessage().contains("thiếu KNIGHT"),
                "Thông báo thực tế: " + error.getMessage()
        );
    }

    @Test
    void rejectsMapWithoutKing() {
        MapData map = new MapData(
                8, 8,
                List.of(),
                List.of(new PieceData("knight", "KNIGHT", 3, 0, 0))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new MapValidator().validate(map)
        );

        assertTrue(
                error.getMessage().contains("thiếu KING"),
                "Thông báo thực tế: " + error.getMessage()
        );
    }
}