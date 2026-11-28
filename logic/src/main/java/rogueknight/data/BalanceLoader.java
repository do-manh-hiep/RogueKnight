package rogueknight.data;

import java.io.IOException;
import java.io.InputStream;
import rogueknight.data.dto.BalanceData;

public final class BalanceLoader {
    private final BalanceValidator validator = new BalanceValidator();

    public BalanceData load() throws IOException {
        InputStream input = BalanceLoader.class.getResourceAsStream("/data/balance.json");
        if (input == null) throw new IOException("Không tìm thấy resource: /data/balance.json");
        return load(input, "/data/balance.json");
    }

    BalanceData load(InputStream input, String resourceName) throws IOException {
        return JsonResourceLoader.read(input, resourceName, BalanceData.class, validator::validate);
    }
}
