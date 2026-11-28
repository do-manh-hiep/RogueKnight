package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import rogueknight.battle.Direction;
import rogueknight.board.Position;

/**
 * Pawn attack geometry (T-03.03): the 3 forward tiles, currentDirection and +/-45 degrees.
 * Read-only. The attack itself is an Enemy ATTACK request validated by EnemyRules and applied by
 * ActionResolver; the battle result is decided by BattleController, never here.
 */
public final class PawnAttackRule {

    private PawnAttackRule() { }

    /**
     * Returns the up to 3 attack target positions based on Pawn's position and direction.
     */
    public static List<Position> attackTargets(Position currentPos, Direction direction) {
        Objects.requireNonNull(currentPos, "currentPos must not be null");
        Objects.requireNonNull(direction, "direction must not be null");

        Direction[] directions = new Direction[] {
            direction,
            direction.rotate45CounterClockwise(),
            direction.rotate45Clockwise()
        };

        List<Position> targets = new ArrayList<>(3);
        for (Direction dir : directions) {
            int col = currentPos.column() + dir.deltaColumn();
            int row = currentPos.row() + dir.deltaRow();
            if (Position.isInBounds(col, row)) {
                targets.add(new Position(col, row));
            }
        }
        return List.copyOf(targets);
    }

}