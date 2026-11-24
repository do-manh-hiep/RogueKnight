package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;

import java.util.List;
import java.util.Set;

/** Port between input/UI and the battle domain controller. */
public interface BattleControllerPort {
    BattleViewSnapshot snapshot();
    Set<BoardPosition> legalMoves(String actorId);
    MoveResult submitMove(String actorId, BoardPosition target);

    /** Current action array: Wait [0], Move/Attack [1], then usable equipment in loadout order. */
    default List<ActionMode> availableModes() {
        return List.of(ActionMode.WAIT, ActionMode.MOVE);
    }

    /** Legal targets for an action mode. MOVE may include both move and sword targets. */
    default Set<BoardPosition> legalTargets(String actorId, ActionMode mode) {
        return mode == ActionMode.MOVE ? legalMoves(actorId) : Set.of();
    }

    default MoveResult submitAction(String actorId, ActionMode mode, BoardPosition target) {
        if (mode == ActionMode.MOVE) return submitMove(actorId, target);
        return MoveResult.rejected("UNSUPPORTED_ACTION", snapshot());
    }

    /** Wait is targetless and consumes the player's action. */
    default MoveResult submitWait(String actorId) {
        return MoveResult.rejected("UNSUPPORTED_ACTION", snapshot());
    }
}
