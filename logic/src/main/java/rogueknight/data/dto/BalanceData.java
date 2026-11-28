package rogueknight.data.dto;

import java.util.Map;

/** JSON balance root. Record components map directly to the JSON properties. */
public record BalanceData(Map<String, PieceStatsData> pieces) { }
