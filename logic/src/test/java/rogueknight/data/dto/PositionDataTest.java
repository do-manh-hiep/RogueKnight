package rogueknight.data.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PositionDataTest {

    @Test //Gradle gọi JUnit; JUnit tìm phương thức có @Test
    void readsCoordinatesFromJson() {
        String json = """
                {
                    "column": 2,
                    "row": 3
                }
                """;
        Gson gson = new Gson();  //Ta tạo object Gson để sử dụng chức năng đọc JSON.

        PositionData position =
                gson.fromJson(json, PositionData.class);

        assertEquals(Integer.valueOf(2), position.column());
        assertEquals(Integer.valueOf(3), position.row());
    }
    @Test
    void readNullColumn(){
        String json = """
        {
            "row": 3
        }
        """;
        Gson gson = new Gson();
        PositionData position = gson.fromJson(json, PositionData.class);
        assertNull(position.column()); 
        assertEquals(Integer.valueOf(3), position.row());
    }
}