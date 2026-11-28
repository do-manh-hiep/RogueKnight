package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import java.util.List;
import java.util.Set;

/** UI-facing port; production adapters and test doubles implement the complete contract. */
public interface BattleControllerPort {
    BattleViewSnapshot snapshot();
    Set<BoardPosition> legalMoves(String actorId);
    List<ActionMode> availableModes();
    Set<BoardPosition> legalTargets(String actorId, ActionMode mode);
    UiActionResult submitAction(String actorId, ActionMode mode, BoardPosition target);
    UiActionResult submitWait(String actorId);
}
