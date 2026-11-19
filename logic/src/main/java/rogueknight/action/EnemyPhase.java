package rogueknight.action;

import java.util.List;
import rogueknight.battle.BattleState;

/*
 * EnemyPhase.java  |  T-02.01  |  TD: 5.2-5.3
 *
 * Điểm nối cho lượt địch sau Player Action.
 * [TẠM - T-02.01] NONE không hành động; T-03.01 thay bằng scheduler/queue thật.
 */
@FunctionalInterface
public interface EnemyPhase {

    EnemyPhase NONE = state -> List.of();

    /** Chạy các Enemy đến hạn trên state và trả events theo thứ tự. */
    List<BattleEvent> run(BattleState state);
}
