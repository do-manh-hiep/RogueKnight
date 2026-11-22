package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;
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

    /**
     * Checks if the Knight is within the attack range of the specified Pawn, using its stored direction.
     * The Knight is found by type, not by a fixed ID.
     */
    public static boolean canAttackKnight(StateView view, String pawnId) {
        Objects.requireNonNull(view, "view must not be null");
        Objects.requireNonNull(pawnId, "pawnId must not be null");

        Optional<PieceState> pawn = view.pieceState(pawnId);
        if (pawn.isEmpty() || pawn.get().type() != PieceType.PAWN) {
            return false;
        }
        return EnemyRules.canAttackKnight(view, pawnId);
    }
}
