package rogueknight.core.input;

import rogueknight.board.Position;
import rogueknight.core.view.BattleViewSnapshot;
import java.util.List;
import java.util.Set;

/** UI-facing port; production adapters and test doubles implement the complete contract. */
public interface BattleControllerPort {
    BattleViewSnapshot snapshot();
    Set<Position> legalMoves(String actorId);
    List<ActionMode> availableModes();
    Set<Position> legalTargets(String actorId, ActionMode mode);
    UiActionResult submitAction(String actorId, ActionMode mode, Position target);
    UiActionResult submitWait(String actorId);
}
