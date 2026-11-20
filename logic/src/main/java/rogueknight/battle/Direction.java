package rogueknight.battle;

public enum Direction {
    NORTH(0, 1),
    NORTH_EAST(1, 1),
    EAST(1, 0),
    SOUTH_EAST(1, -1),
    SOUTH(0, -1),
    SOUTH_WEST(-1, -1),
    WEST(-1, 0),
    NORTH_WEST(-1, 1);

    private final int deltaColumn;
    private final int deltaRow;

    Direction(int deltaColumn, int deltaRow) {
        this.deltaColumn = deltaColumn;
        this.deltaRow = deltaRow;
    }

    public int deltaColumn() {
        return deltaColumn;
    }

    public int deltaRow() {
        return deltaRow;
    }
}