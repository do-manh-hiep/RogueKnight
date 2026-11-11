package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;

/**
 * Result returned after attempting to execute a move.
 *
 * @param accepted whether the request was accepted and executed
 * @param errorReason error explanation if rejected, or null if accepted
 * @param before state snapshot before the move was attempted
 * @param after state snapshot after the move was executed (or unchanged before state if rejected)
 */
public record MoveResult(
    boolean accepted,
    String errorReason,
    BattleViewSnapshot before,
    BattleViewSnapshot after
) {
    public static MoveResult success(BattleViewSnapshot before, BattleViewSnapshot after) {
        return new MoveResult(true, null, before, after);
    }

    public static MoveResult rejected(String errorReason, BattleViewSnapshot before) {
        return new MoveResult(false, errorReason, before, before);
    }
}
