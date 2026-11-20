package rogueknight.data.dto;

public class PieceData {
    private final String id, type;
    private final Integer column, row, spawnOrder;

    public PieceData(String id, String type, Integer column, Integer row, Integer spawnOrder){
        this.id = id;
        this.type = type;
        this.column = column;
        this.row = row;
        this.spawnOrder = spawnOrder;
    }
    public String id(){ return id;}
    public String type(){ return type;}
    public Integer column(){ return column;}
    public Integer row(){ return row;}
    public Integer spawnOrder(){ return spawnOrder;}
}
