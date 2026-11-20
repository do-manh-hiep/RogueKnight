package rogueknight.data.dto;

/**
 * Dữ liệu tọa độ đọc từ JSON.
 * Được kiểm tra trước khi chuyển sang trạng thái runtime.
 */
public final class PositionData {
    private final Integer column;
    private final Integer row;

    public PositionData(Integer column, Integer row){
        this.column = column;
        this.row = row;
    }
    public Integer column(){
        return column;
    }
    public Integer row(){
        return row;
    }
}
// read-only, khong setters