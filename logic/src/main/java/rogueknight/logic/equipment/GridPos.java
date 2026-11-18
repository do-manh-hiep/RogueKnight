package rogueknight.logic.equipment;

import java.util.Objects;

public record GridPos(int column, int row) {
    public static final int BOARD_SIZE = 8;

    public boolean isValid() {
        return column >= 0 && column < BOARD_SIZE && row >= 0 && row < BOARD_SIZE;
    }

    public GridPos step(int dCol, int dRow) {
        return new GridPos(column + dCol, row + dRow);
    }

    public String notation() {
        if (!isValid()) {
            return "(" + column + "," + row + ")";
        }
        return "" + (char) ('a' + column) + (row + 1);
    }

    public static GridPos fromNotation(String notation) {
        Objects.requireNonNull(notation, "notation must not be null");
        if (notation.length() != 2) {
            throw new IllegalArgumentException("Invalid notation: " + notation);
        }
        int col = Character.toLowerCase(notation.charAt(0)) - 'a';
        int row = notation.charAt(1) - '1';
        GridPos pos = new GridPos(col, row);
        if (!pos.isValid()) {
            throw new IllegalArgumentException("Out of bounds: " + notation);
        }
        return pos;
    }
}