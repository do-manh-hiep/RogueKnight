package rogueknight.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Shared JSON decoding and validation boundary for map and balance resources. */
final class JsonResourceLoader {
    private static final Gson GSON = new Gson();

    private JsonResourceLoader() { }

    static <T> T read(InputStream input, String resourceName, Class<T> type, Consumer<T> validate)
            throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            T value;
            try {
                value = GSON.fromJson(reader, type);
            } catch (JsonParseException exception) {
                throw new IOException("JSON " + resourceName + " không hợp lệ", exception);
            }
            try {
                validate.accept(value);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Dữ liệu " + resourceName + " không hợp lệ: "
                        + exception.getMessage(), exception);
            }
            return value;
        }
    }
}
