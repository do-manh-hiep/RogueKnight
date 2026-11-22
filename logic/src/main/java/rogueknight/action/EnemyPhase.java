package rogueknight.action;

import java.util.List;
import rogueknight.battle.BattleState;

/*
 * EnemyPhase.java  |  T-02.01, T-03.01  |  TD: 5.2-5.3
 *
 * Điểm nối cho lượt địch sau Player Action. BattleController gọi run() sau khi đã tăng Turn.
 * - EnemyTurnRunner: lượt địch thật (queue, lịch, terminal) từ T-03.01.
 * - NONE: không Enemy nào hành động; dùng cho test luật của Player.
 */
@FunctionalInterface
public interface EnemyPhase {

    EnemyPhase NONE = state -> List.of();

    /** Chạy các Enemy đến hạn trên state và trả events theo thứ tự. */
    List<BattleEvent> run(BattleState state);
}
