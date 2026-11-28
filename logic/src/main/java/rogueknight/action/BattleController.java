package rogueknight.action;

import java.util.Objects;
import java.util.Set;
import rogueknight.battle.BattleSnapshot;
import rogueknight.board.Position;

/** Public immutable-input facade; mutable battle aggregates remain package-private. */
public final class BattleController {
    private final rogueknight.battle.BattleController delegate;

    private BattleController(rogueknight.battle.BattleController delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static BattleController fromSnapshot(BattleSnapshot initialSnapshot, EnemyPolicy enemyPolicy) {
        return new BattleController(rogueknight.battle.BattleController.fromSnapshot(
            Objects.requireNonNull(initialSnapshot, "initialSnapshot"),
            Objects.requireNonNull(enemyPolicy, "enemyPolicy")));
    }

    public BattleSnapshot snapshot() { return delegate.snapshot(); }
    public Set<Position> legalMoves(String actorId) { return delegate.legalMoves(actorId); }
    public Set<Position> legalTargets(String actorId, ActionKind kind) { return delegate.legalTargets(actorId, kind); }
    public boolean canWait(String actorId) { return delegate.canWait(actorId); }
    public ActionResult submit(ActionRequest request) { return delegate.submit(request); }
}
