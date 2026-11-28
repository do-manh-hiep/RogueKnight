package rogueknight.core.view;

import rogueknight.board.Position;
import java.util.Optional;

/**
 * Pure mathematical adapter converting world coordinates (x, y) to board squares (column, row).
 * In world space:
 * - originX, originY defines the bottom-left corner of square a1 (0, 0).
 * - tileSize is the width and height of each square in world units.
 * - Y axis points UP (standard Cartesian / LibGDX world coordinates).
 */
public final class BoardProjection {
    private final float originX;
    private final float originY;
    private final float tileSize;

    public BoardProjection(float originX, float originY, float tileSize) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("tileSize must be positive: " + tileSize);
        }
        this.originX = originX;
        this.originY = originY;
        this.tileSize = tileSize;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
        return originY;
    }

    public float getTileSize() {
        return tileSize;
    }

    public float getBoardWidth() {
        return tileSize * Position.BOARD_SIZE;
    }

    public float getBoardHeight() {
        return tileSize * Position.BOARD_SIZE;
    }

    /**
     * Converts world (x, y) coordinates to a Position if it falls inside the 8x8 board.
     * Returns Optional.empty() if coordinates fall outside the board.
     */
    public Optional<Position> worldToBoard(float worldX, float worldY) {
        float dx = worldX - originX;
        float dy = worldY - originY;

        if (dx < 0 || dy < 0) {
            return Optional.empty();
        }

        int col = (int) Math.floor(dx / tileSize);
        int row = (int) Math.floor(dy / tileSize);

        if (!Position.isInBounds(col, row)) {
            return Optional.empty();
        }

        return Optional.of(new Position(col, row));
    }

    /** Returns the world X coordinate of the left edge of a column. */
    public float getCellLeft(int column) {
        return originX + column * tileSize;
    }

    /** Returns the world Y coordinate of the bottom edge of a row. */
    public float getCellBottom(int row) {
        return originY + row * tileSize;
    }

    /** Returns the world X coordinate of the center of a column. */
    public float getCellCenterX(int column) {
        return originX + (column + 0.5f) * tileSize;
    }

    /** Returns the world Y coordinate of the center of a row. */
    public float getCellCenterY(int row) {
        return originY + (row + 0.5f) * tileSize;
    }

}
