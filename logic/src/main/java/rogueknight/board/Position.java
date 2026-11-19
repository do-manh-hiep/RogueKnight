package rogueknight.board;

/*
 * Position.java  |  T-01.02  |  TD: 4.4, R-B01, TC-B01
 *
 * Tọa độ một ô trên Board 8x8, giá trị bất biến (record).
 * - column: 0..7 tương ứng a..h; row: 0..7 tương ứng 1..8.
 * - a1 = (0,0); d1 = (3,0); d4 = (3,3); d7 = (3,6); h8 = (7,7). Hướng Bắc tăng row.
 * - Tọa độ ngoài biên bị từ chối ngay tại constructor (IllegalArgumentException),
 *   nên mọi Position tồn tại đều hợp lệ.
 * - Rules cần kiểm tra biên trước khi tạo Position dùng isInBounds(column, row).
 * - equals/hashCode theo hai chỉ số (do record sinh).
 * - Ký hiệu cờ ("d4") chỉ dùng cho fixture, test và log; UI tự đổi pixel <-> Position.
 * Từ khóa tìm kiếm: BOARD_SIZE, isInBounds, fromNotation, toNotation.
 */
public record Position(int column, int row) {

    public static final int BOARD_SIZE = 8;

    public Position {
        if (!isInBounds(column, row)) {
            throw new IllegalArgumentException("Position out of bounds: (" + column + "," + row + ")");
        }
    }

    public static boolean isInBounds(int column, int row) {
        return column >= 0 && column < BOARD_SIZE && row >= 0 && row < BOARD_SIZE;
    }

    public static Position fromNotation(String notation) {
        if (notation == null || notation.length() != 2) {
            throw new IllegalArgumentException("Invalid notation: " + notation);
        }
        return new Position(notation.charAt(0) - 'a', notation.charAt(1) - '1');
    }

    public String toNotation() {
        return "" + (char) ('a' + column) + (char) ('1' + row);
    }

    @Override
    public String toString() {
        return toNotation();
    }
}
