package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleState;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/**
 * Rules and execution for Pawn's attack action.
 * Pawn attacks any target in 3 forward tiles (currentDirection and +/-45 deg).
 * In Map01/GDD, Pawn deals 1 damage to Knight.
 */
public final class PawnAttackRule {

    private static final int PAWN_DAMAGE = 1;
    private static final String KNIGHT_ID = "knight";

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
     * Checks if the Knight is within the attack range of the specified Pawn.
     */
    public static boolean canAttackKnight(BattleState state, String pawnId) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(pawnId, "pawnId must not be null");

        Optional<PieceState> pawnOpt = state.pieceState(pawnId);
        if (pawnOpt.isEmpty() || pawnOpt.get().type() != PieceType.PAWN) {
            return false;
        }

        PieceState pawn = pawnOpt.get();
        Position pawnPos = state.positionOf(pawnId).orElse(null);
        if (pawnPos == null || pawn.currentDirection() == null) {
            return false;
        }

        Optional<Position> knightPosOpt = state.positionOf(KNIGHT_ID);
        if (knightPosOpt.isEmpty()) {
            return false;
        }

        Position knightPos = knightPosOpt.get();
        List<Position> targets = attackTargets(pawnPos, pawn.currentDirection());
        return targets.contains(knightPos);
    }

    /**
     * Executes the Pawn's attack against the Knight, applying 1 damage.
     * If the Knight dies (HP drops to 0), the game ends with BattleResult.DEFEAT.
     */
    public static List<BattleEvent> executeAttack(BattleState state, String pawnId) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(pawnId, "pawnId must not be null");

        if (!canAttackKnight(state, pawnId)) {
            throw new IllegalStateException("Pawn " + pawnId + " cannot attack Knight");
        }

        PieceState knight = state.pieceState(KNIGHT_ID).orElseThrow();
        Position knightPos = state.positionOf(KNIGHT_ID).orElseThrow();

        List<BattleEvent> events = new ArrayList<>();
        int remainingHp = knight.hp() - PAWN_DAMAGE;

        if (remainingHp <= 0) {
            state.removePiece(KNIGHT_ID);
            events.add(new BattleEvent.PieceDamaged(KNIGHT_ID, PAWN_DAMAGE, 0));
            events.add(new BattleEvent.PieceRemoved(KNIGHT_ID, knightPos));
            state.finish(BattleResult.DEFEAT);
        } else {
            state.replacePiece(knight.withHp(remainingHp));
            events.add(new BattleEvent.PieceDamaged(KNIGHT_ID, PAWN_DAMAGE, remainingHp));
        }

        return List.copyOf(events);
    }
}
