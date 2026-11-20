package rogueknight.action;

import rogueknight.board.Position;

/** Validated internal plan; only rules create plans and the resolver consumes them. */
record ActionPlan(ActionKind kind, String actorId, Position from, Position target, String targetId) {
    static ActionPlan waitPlan(String actorId, Position from) {
        return new ActionPlan(ActionKind.WAIT, actorId, from, null, null);
    }
}
