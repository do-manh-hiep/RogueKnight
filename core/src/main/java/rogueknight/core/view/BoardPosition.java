package rogueknight.core.view;

import java.util.Objects;

/**
 * Immutable board coordinate on an 8x8 chessboard.
 * Column: 0..7 represents columns 'a'..'h'.
 * Row: 0..7 represents rows '1'..'8'.
 * Coordinates conform to: a1 = (0, 0), d1 = (3, 0), d4 = (3, 3), e6 = (4, 5), h8 = (7, 7).
 */
public record BoardPosition(int column, int row) {
    public static final int BOARD_SIZE = 8;

    public BoardPosition {
        // Records allow storing coordinates even outside 0..7 for bounds validation checks,
        // but isValid() must be checked before board lookups.
    }

    /**
     * Checks if this position is within the 8x8 board boundaries.
     */
    public boolean isValid() {
        return column >= 0 && column < BOARD_SIZE && row >= 0 && row < BOARD_SIZE;
    }

    /**
     * Converts to standard chess algebraic notation (e.g. "a1", "d4", "e6", "h8").
     */
    public String notation() {
        if (!isValid()) {
            return "(" + column + "," + row + ")";
        }
        char colChar = (char) ('a' + column);
        int rowNumber = row + 1;
        return "" + colChar + rowNumber;
    }

    /**
     * Parses standard chess algebraic notation (e.g. "a1", "e6") into a BoardPosition.
     */
    public static BoardPosition fromNotation(String notation) {
        Objects.requireNonNull(notation, "notation must not be null");
        if (notation.length() != 2) {
            throw new IllegalArgumentException("Invalid notation length: " + notation);
        }
        char colChar = Character.toLowerCase(notation.charAt(0));
        char rowChar = notation.charAt(1);

        int col = colChar - 'a';
        int row = rowChar - '1';

        BoardPosition pos = new BoardPosition(col, row);
        if (!pos.isValid()) {
            throw new IllegalArgumentException("Notation out of bounds: " + notation);
        }
        return pos;
    }

    /**
     * Calculates Chebyshev distance (max(|dx|, |dy|)).
     */
    public int chebyshevDistance(BoardPosition other) {
        return Math.max(Math.abs(this.column - other.column), Math.abs(this.row - other.row));
    }

    /**
     * Calculates Manhattan distance (|dx| + |dy|).
     */
    public int manhattanDistance(BoardPosition other) {
        return Math.abs(this.column - other.column) + Math.abs(this.row - other.row);
    }
}
