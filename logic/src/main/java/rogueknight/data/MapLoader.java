package rogueknight.data;

import com.google.gson.Gson;
import rogueknight.data.dto.MapData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class MapLoader {
    private final Gson gson;
    private final MapValidator validator;

    public MapLoader() {
        this.gson = new Gson();
        this.validator = new MapValidator();
    }

    public MapData loadMap01() throws IOException {
        InputStream input =
                MapLoader.class.getResourceAsStream("/data/map01.json");    
                //tìm và mở tài nguyên JSON

        if (input == null) {
            throw new IOException(
                    "Không tìm thấy resource: /data/map01.json"
            );
        }

        try (Reader reader = new InputStreamReader(
                input,
                StandardCharsets.UTF_8
        )) {
            MapData map = gson.fromJson(reader, MapData.class);
            validator.validate(map);
            return map;
        }
    }
}
