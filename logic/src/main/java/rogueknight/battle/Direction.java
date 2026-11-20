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

    public static Direction fromDelta(int deltaColumn, int deltaRow) {
        for (Direction direction : Direction.values()) {
            if (direction.deltaColumn == deltaColumn
                    && direction.deltaRow == deltaRow) {
                return direction;
            }
        }
        throw new IllegalArgumentException(
                "Invalid direction delta: (" + deltaColumn + ", " + deltaRow + ")"
        );
    }

    public Direction rotate45Clockwise() {
        return values()[(ordinal() + 1) % 8];
    }

    public Direction rotate45CounterClockwise() {
        return values()[(ordinal() + 7) % 8];
    }
}
