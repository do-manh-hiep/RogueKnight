package rogueknight.core.input;

import java.util.List;
import java.util.Objects;
import rogueknight.action.BattleEvent;
import rogueknight.core.view.BattleViewSnapshot;

/**
 * Result returned after attempting to execute a player action.
 *
 * @param accepted whether the request was accepted and executed
 * @param errorReason error explanation if rejected, or null if accepted
 * @param before state snapshot before the action was attempted
 * @param after state snapshot after the action was executed (or unchanged before state if rejected)
 * @param events ordered domain events emitted by the action and its Enemy phase
 */
public record MoveResult(
    boolean accepted,
    String errorReason,
    BattleViewSnapshot before,
    BattleViewSnapshot after,
    List<BattleEvent> events
) {
    public MoveResult {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        events = List.copyOf(events);
        if (accepted && errorReason != null) {
            throw new IllegalArgumentException("accepted result cannot have an error");
        }
        if (!accepted && (errorReason == null || !events.isEmpty() || !before.equals(after))) {
            throw new IllegalArgumentException("rejected result must preserve state, have an error and no events");
        }
    }

    public static MoveResult success(
        BattleViewSnapshot before,
        BattleViewSnapshot after,
        List<BattleEvent> events
    ) {
        return new MoveResult(true, null, before, after, events);
    }

    public static MoveResult rejected(String errorReason, BattleViewSnapshot before) {
        return new MoveResult(false, errorReason, before, before, List.of());
    }
}
