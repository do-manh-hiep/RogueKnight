package rogueknight.battle;

import rogueknight.action.BattleEvent;

import java.util.ArrayList;
import java.util.List;
import rogueknight.board.Position;

/**
 * Applies a previously validated Player or Enemy Action (C-02).
 * It never validates, advances the turn, touches the schedule or decides the battle result.
 */
final class ActionResolver {
    private static final int PROMOTED_QUEEN_HP = 2;
    private static final int PROMOTED_QUEEN_CYCLE = 4;
    private static final int PROMOTION_ACTIVATION_OFFSET = 5;

    private ActionResolver() { }

    static List<BattleEvent> resolve(BattleState state, ActionPlan plan) {
        List<BattleEvent> events = new ArrayList<>();
        events.add(new BattleEvent.ActionPerformed(plan.kind(), plan.actorId()));

        switch (plan.kind()) {
            case MOVE -> {
                state.movePiece(plan.actorId(), plan.target());
                events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.target()));
                applyPawnStepEffects(state, events, plan);
            }
            case SWORD -> {
                applyDamage(state, events, plan.affectedTargetIds().get(0), 1);
            }
            case CAPTURE -> {
                // Validation guarantees CaptureDamage (2) removes this target.
                applyDamage(state, events, plan.affectedTargetIds().get(0), 2);
                state.movePiece(plan.actorId(), plan.target());
                events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.target()));
            }
            case WAIT -> { /* Lifecycle is advanced by BattleController, not by the resolver. */ }
            case CANNON_MOVEMENT -> {
                state.consumeEquipment(plan.equipmentType());
                if (!plan.affectedTargetIds().isEmpty()) {
                    // Validation guarantees Cannon Movement capture deals lethal damage (2).
                    applyDamage(state, events, plan.affectedTargetIds().get(0), 2);
                }
                state.movePiece(plan.actorId(), plan.target());
                events.add(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.target()));
            }
            case CANNON_ATTACK -> {
                state.consumeEquipment(plan.equipmentType());
                applyDamage(state, events, plan.affectedTargetIds().get(0), 1);
            }
            case DIAGONAL_SKEWER -> {
                state.consumeEquipment(plan.equipmentType());
                for (String hitId : plan.affectedTargetIds()) {
                    applyDamage(state, events, hitId, 1);
                }
            }
            // Enemy basic attack; the actor stays in place. A Knight at 0 HP leaves the Board like any piece.
            case ATTACK -> applyDamage(state, events, plan.affectedTargetIds().get(0), EnemyRules.ENEMY_DAMAGE);
        }
        return List.copyOf(events);
    }

    /**
     * T-03.03 effects of a Pawn step, inside the same Action (C-02): the step vector becomes the new
     * currentDirection, and reaching row 1 promotes to Queen with the T+5 schedule (DEC-05/07).
     * The battle controller sees the type change and does not overwrite that schedule.
     */
    private static void applyPawnStepEffects(BattleState state, List<BattleEvent> events, ActionPlan plan) {
        PieceState actor = state.pieceState(plan.actorId()).orElseThrow();
        if (actor.type() != PieceType.PAWN) return;

        if (plan.target().row() == 0) {
            PieceState queen = new PieceState(
                    actor.id(),
                    PieceType.QUEEN,
                    PROMOTED_QUEEN_HP,
                    PROMOTED_QUEEN_CYCLE,
                    state.globalTurn() + PROMOTION_ACTIVATION_OFFSET,
                    actor.spawnOrder(),
                    null
            );
            state.replacePiece(queen);
            events.add(new BattleEvent.PiecePromoted(
                    actor.id(), PieceType.PAWN, PieceType.QUEEN, plan.target()));
        } else {
            Direction step = Direction.fromDelta(
                    plan.target().column() - plan.from().column(),
                    plan.target().row() - plan.from().row());
            state.replacePiece(actor.withDirection(step));
        }
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
