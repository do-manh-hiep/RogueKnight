package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;
import rogueknight.board.Position;

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
                applyDamage(state, events, plan.targetId(), 1);
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
            case CANNON_MOVEMENT -> {
                state.consumeEquipment(plan.equipmentType());
                if (plan.targetId() != null) {
                    state.removePiece(plan.targetId());
                    events.add(new BattleEvent.PieceDamaged(plan.targetId(), 2, 0));
                    events.add(new BattleEvent.PieceRemoved(plan.targetId(), plan.target()));
                }
                state.movePiece(plan.actorId(), plan.landingPosition());
                events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.landingPosition()));
            }
            case CANNON_ATTACK -> {
                state.consumeEquipment(plan.equipmentType());
                applyDamage(state, events, plan.targetId(), 1);
            }
            case DIAGONAL_SKEWER -> {
                state.consumeEquipment(plan.equipmentType());
                for (String hitId : plan.affectedTargetIds()) {
                    applyDamage(state, events, hitId, 1);
                }
                if (!plan.landingPosition().equals(plan.from())) {
                    state.movePiece(plan.actorId(), plan.landingPosition());
                    events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.landingPosition()));
                }
            }
        }
        return List.copyOf(events);
    }

    private static void applyDamage(BattleState state, List<BattleEvent> events, String victimId, int damage) {
        PieceState victim = state.pieceState(victimId).orElseThrow();
        Position victimPos = state.positionOf(victimId).orElseThrow();
        int remainingHp = victim.hp() - damage;
        if (remainingHp <= 0) {
            state.removePiece(victim.id());
            events.add(new BattleEvent.PieceDamaged(victim.id(), damage, 0));
            events.add(new BattleEvent.PieceRemoved(victim.id(), victimPos));
        } else {
            state.replacePiece(victim.withHp(remainingHp));
            events.add(new BattleEvent.PieceDamaged(victim.id(), damage, remainingHp));
        }
    }
}