package rogueknight.logic;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonInfrastructureTest {
    private record Sample(String name, int value) {}

    @Test
    void readsUtf8JsonFromClasspathWithoutLibGdx() {
        var stream = Objects.requireNonNull(
            getClass().getResourceAsStream("/fixtures/sample.json"),
            "Missing JSON fixture"
        );

        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var sample = new Gson().fromJson(reader, Sample.class);

            assertEquals("Hiệp sĩ", sample.name());
            assertEquals(8, sample.value());
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
