package rogueknight.data;

import rogueknight.data.dto.BalanceData;
import rogueknight.data.dto.PieceStatsData;

import java.util.HashMap;
import java.util.Map;

public final class PieceCatalog {

    private final Map<String, PieceSpec> specs;

    public PieceCatalog(BalanceData balance) {
        new BalanceValidator().validate(balance);

        Map<String, PieceSpec> built = new HashMap<>();

        for (String type : BalanceValidator.REQUIRED_TYPES) {
            PieceStatsData stats = balance.pieces().get(type);
            built.put(type, PieceSpec.from(stats));
        }

        this.specs = Map.copyOf(built);
    }

    public PieceSpec get(String type) {
        PieceSpec spec = type == null ? null : specs.get(type);

        if (spec == null) {
            throw new IllegalArgumentException(
                    "Không có PieceSpec cho loại quân: " + type
            );
        }

        return spec;
    }
}