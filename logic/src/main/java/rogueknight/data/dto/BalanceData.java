package rogueknight.data.dto;

import java.util.Map;

public class BalanceData {
    private final Map<String, PieceStatsData> pieces;
    
    public BalanceData(Map<String, PieceStatsData> pieces){
        this.pieces = pieces;
    }

    public Map<String, PieceStatsData> pieces(){ return pieces;}
}
