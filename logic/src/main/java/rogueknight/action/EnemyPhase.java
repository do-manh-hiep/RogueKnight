package rogueknight.action;

/** Legacy read-only name; unlike the former callback, it cannot access mutable BattleState. */
@FunctionalInterface
public interface EnemyPhase extends EnemyPolicy {
    EnemyPhase NONE = (view, enemyId) -> EnemyDecision.skip("DISABLED");
}
