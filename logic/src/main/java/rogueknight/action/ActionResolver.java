package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;

/** Applies a previously validated Player Action. It never validates or advances lifecycle. */
final class ActionResolver {
    private ActionResolver() { }

    static List<BattleEvent> resolve(BattleState state, ActionPlan plan) {
        List<BattleEvent> events = new ArrayList<>();
        events.add(new BattleEvent.ActionPerformed(plan.kind(), plan.actorId()));

        switch (plan.kind()) {
            case MOVE -> {
                state.movePiece(plan.actorId(), plan.target());
                events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.target()));
            }
            case SWORD -> {
                PieceState victim = state.pieceState(plan.targetId()).orElseThrow();
                int remainingHp = victim.hp() - 1;
                if (remainingHp <= 0) {
                    state.removePiece(victim.id());
                    events.add(new BattleEvent.PieceDamaged(victim.id(), 1, 0));
                    events.add(new BattleEvent.PieceRemoved(victim.id(), plan.target()));
                } else {
                    state.replacePiece(victim.withHp(remainingHp));
                    events.add(new BattleEvent.PieceDamaged(victim.id(), 1, remainingHp));
                }
            }
            case CAPTURE -> {
                // CaptureDamage is 2; validation guarantees the target has at most 2 HP.
                state.removePiece(plan.targetId());
                events.add(new BattleEvent.PieceDamaged(plan.targetId(), 2, 0));
                events.add(new BattleEvent.PieceRemoved(plan.targetId(), plan.target()));
                state.movePiece(plan.actorId(), plan.target());
                events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.target()));
            }
            case WAIT -> { /* Lifecycle is advanced by BattleController, not by the resolver. */ }
        }
        return List.copyOf(events);
    }
}
