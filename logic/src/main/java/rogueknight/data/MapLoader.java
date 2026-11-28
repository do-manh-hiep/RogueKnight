package rogueknight.data;

import java.io.IOException;
import java.io.InputStream;
import rogueknight.data.dto.MapData;

public final class MapLoader {
    private final MapValidator validator = new MapValidator();

    public MapData loadMap01() throws IOException {
        InputStream input = MapLoader.class.getResourceAsStream("/data/map01.json");
        if (input == null) throw new IOException("Không tìm thấy resource: /data/map01.json");
        return loadMap(input, "/data/map01.json");
    }

    MapData loadMap(InputStream input, String resourceName) throws IOException {
        return JsonResourceLoader.read(input, resourceName, MapData.class, validator::validate);
    }
}
