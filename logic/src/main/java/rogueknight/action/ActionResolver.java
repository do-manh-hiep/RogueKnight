package rogueknight.action;

import java.util.List;
import rogueknight.battle.BattleState;

/*
 * ActionResolver.java  |  T-02.01  |  TD: 4.9-4.10
 *
 * Áp dụng plan đã được KnightRules validate lên BattleState và trả events theo thứ tự.
 * - Không validate lại luật, không đổi turn/phase: đó là việc của BattleController.
 * - Board vẫn tự chặn occupancy sai (I01) như lưới an toàn cuối.
 */
final class ActionResolver {

    private ActionResolver() {
    }

    static List<BattleEvent> resolve(BattleState state, MovePlan plan) {
        state.movePiece(plan.actorId(), plan.to());
        return List.of(new BattleEvent.PieceMoved(plan.actorId(), plan.from(), plan.to()));
    }
}
