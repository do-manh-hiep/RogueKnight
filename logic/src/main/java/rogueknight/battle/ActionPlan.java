package rogueknight.battle;

import rogueknight.action.ActionKind;
import java.util.List;
import rogueknight.board.Position;

/** Validated internal action; only rules create plans and the resolver consumes them. */
record ActionPlan(ActionKind kind, String actorId, Position from, Position target,
                  EquipmentType equipmentType, List<String> affectedTargetIds) {
    ActionPlan {
        affectedTargetIds = affectedTargetIds == null ? List.of() : List.copyOf(affectedTargetIds);
    }
    ActionPlan(ActionKind kind, String actorId, Position from, Position target, String targetId) {
        this(kind, actorId, from, target, null, targetId == null ? List.of() : List.of(targetId));
    }
    static ActionPlan waitPlan(String actorId, Position from) {
        return new ActionPlan(ActionKind.WAIT, actorId, from, null, null, List.of());
    }
}
