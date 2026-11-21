package rogueknight.logic.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CannonRayScanner {

    public static final int[][] ORTHOGONAL_DIRS = {
        {0, 1},   // Up (North)
        {0, -1},  // Down (South)
        {-1, 0},  // Left (West)
        {1, 0}    // Right (East)
    };

    public record RayResult(
        int dCol,
        int dRow,
        GridPos screenPos,
        List<GridPos> emptySquaresAfterScreen,
        GridPos terminalBlockerPos,
        EquipmentBoardView.EnemyInfo terminalEnemy
    ) {
        public boolean hasScreen() {
            return screenPos != null;
        }

        public boolean hasTerminalBlocker() {
            return terminalBlockerPos != null;
        }

        public boolean hasTerminalEnemy() {
            return terminalEnemy != null;
        }
    }

    private CannonRayScanner() {}

    public static List<RayResult> scanAllDirections(EquipmentBoardView board) {
        List<RayResult> results = new ArrayList<>(4);
        for (int[] dir : ORTHOGONAL_DIRS) {
            results.add(scanDirection(board, dir[0], dir[1]));
        }
        return results;
    }

    public static RayResult scanDirection(EquipmentBoardView board, int dCol, int dRow) {
        GridPos current = board.getKnightPosition().step(dCol, dRow);
        GridPos screenPos = null;
        List<GridPos> emptyAfterScreen = new ArrayList<>();

        while (current.isValid()) {
            boolean isRock = board.isRockAt(current);
            Optional<EquipmentBoardView.EnemyInfo> enemy = board.getEnemyAt(current);
            boolean hasObstacle = isRock || enemy.isPresent();

            if (screenPos == null) {
                if (hasObstacle) {
                    screenPos = current;
                }
            } else {
                if (!hasObstacle) {
                    emptyAfterScreen.add(current);
                } else {
                    return new RayResult(
                        dCol,
                        dRow,
                        screenPos,
                        List.copyOf(emptyAfterScreen),
                        current,
                        enemy.orElse(null)
                    );
                }
            }
            current = current.step(dCol, dRow);
        }

        return new RayResult(
            dCol,
            dRow,
            screenPos,
            List.copyOf(emptyAfterScreen),
            null,
            null
        );
    }
}