package rogueknight.data.dto;

import java.util.List;

public class MapData {
    private final Integer width, height;
    private final List<PositionData> rocks;
    private final List<PieceData> pieces;

    public MapData(Integer width, Integer height, List<PositionData> rocks, List<PieceData> pieces){
        this.width = width;
        this.height = height;
        this.rocks = rocks;
        this.pieces = pieces;
    }

    public Integer width(){ return width;}
    public Integer height(){ return height;}
    public List<PositionData> rocks(){ return rocks;}
    public List<PieceData> pieces(){ return pieces;}
}
