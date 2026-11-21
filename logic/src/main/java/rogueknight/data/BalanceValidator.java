package rogueknight.data;

import rogueknight.data.dto.BalanceData;
import rogueknight.data.dto.PieceStatsData;

import java.util.List;

public final class BalanceValidator {

    static final List<String> REQUIRED_TYPES = List.of(
            "KNIGHT", "PAWN", "ROOK", "BISHOP", "KING", "QUEEN"
    );

    public void validate(BalanceData balance) {
        if (balance == null || balance.pieces() == null) {
            throw new IllegalArgumentException(
                    "balance.json phải có bảng pieces."
            );
        }

        for (String type : REQUIRED_TYPES) {
            PieceStatsData stats = balance.pieces().get(type);

            if (stats == null) {
                throw new IllegalArgumentException("Thiếu thông số " + type + ".");
            }

            requirePositive(stats.hp(), type + ".hp");

            if ("KNIGHT".equals(type)) {
                requireNonNegative(stats.swordDamage(), type + ".swordDamage");
                requireNonNegative(stats.captureDamage(), type + ".captureDamage");
            } else {
                requirePositive(stats.cycle(), type + ".cycle");
                requireNonNegative(stats.damage(), type + ".damage");
            }
        }
    }

    private void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    field + " phải lớn hơn 0, nhận được: " + value + "."
            );
        }
    }

    private void requireNonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(
                field + " phải có giá trị >= 0, nhận được: " + value + "."
            );
        }
    }
}