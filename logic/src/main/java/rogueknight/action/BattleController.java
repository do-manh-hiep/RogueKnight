package rogueknight.action;

import java.util.Objects;
import java.util.Set;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.board.Position;

/** Compatibility facade; it snapshots input state to prevent a retained mutable alias. */
public final class BattleController {
    private final rogueknight.battle.BattleController delegate;

    public BattleController(BattleState initialState) { this(initialState, EnemyPolicy.NONE); }
    public BattleController(BattleState initialState, EnemyPolicy enemyPolicy) {
        Objects.requireNonNull(initialState, "initialState");
        this.delegate = rogueknight.battle.BattleController.fromSnapshot(initialState.snapshot(),
            Objects.requireNonNull(enemyPolicy, "enemyPolicy"));
    }
    private BattleController(rogueknight.battle.BattleController delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }
    public static BattleController fromSnapshot(BattleSnapshot snapshot, EnemyPolicy enemyPolicy) {
        return new BattleController(rogueknight.battle.BattleController.fromSnapshot(snapshot, enemyPolicy));
    }
    public BattleSnapshot snapshot() { return delegate.snapshot(); }
    public Set<Position> legalMoves(String actorId) { return delegate.legalMoves(actorId); }
    public Set<Position> legalTargets(String actorId, ActionKind kind) { return delegate.legalTargets(actorId, kind); }
    public boolean canWait(String actorId) { return delegate.canWait(actorId); }
    public ActionResult submit(ActionRequest request) { return delegate.submit(request); }
}
