package rogueknight.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import rogueknight.data.dto.BalanceData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class BalanceLoader {
    private final Gson gson = new Gson();
    private final BalanceValidator validator = new BalanceValidator();

    public BalanceData load() throws IOException {
        InputStream input = BalanceLoader.class.getResourceAsStream("/data/balance.json");

        if (input == null){
            throw new IOException("Không tìm thấy resource: /data/balance.json");
        }

        return load(input, "/data/balance.json");
    }

    BalanceData load(InputStream input, String resourceName) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)){
            BalanceData balance;
            try {
                balance = gson.fromJson(reader, BalanceData.class);
            } catch (JsonParseException exception) {
                throw new IOException("JSON " + resourceName + " không hợp lệ", exception);
            }
            try {
                validator.validate(balance);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Dữ liệu " + resourceName + " không hợp lệ: "
                    + exception.getMessage(), exception);
            }
            return balance;
        }
    }
}
