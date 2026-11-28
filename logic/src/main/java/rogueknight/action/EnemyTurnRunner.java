package rogueknight.action;

import java.util.Objects;
import rogueknight.battle.StateView;

/** Compatibility policy adapter. The actual enemy queue is internal to the battle controller. */
public final class EnemyTurnRunner implements EnemyPolicy {
    private final EnemyPolicy policy;
    public EnemyTurnRunner(EnemyPolicy policy) { this.policy = Objects.requireNonNull(policy, "policy"); }
    @Override public EnemyDecision choose(StateView view, String enemyId) { return policy.choose(view, enemyId); }
}
