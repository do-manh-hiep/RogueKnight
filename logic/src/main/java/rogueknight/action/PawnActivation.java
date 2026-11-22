package rogueknight.action;

import java.util.List;
import rogueknight.battle.BattleState;

/*
 * PawnActivation.java  |  T-03.01 (điểm nối cho T-03.03)  |  TD: 5.5, 5.10, DEC-01, DEC-05
 *
 * Một activation trọn vẹn của Pawn: Attack theo hướng đang lưu, nếu không thì BFS/Move một ô,
 * cập nhật hướng và phong cấp. Logic thật thuộc T-03.03; EnemyTurnRunner chỉ điều phối.
 *
 * Hợp đồng với EnemyTurnRunner:
 * - Runner đã phát ActorActivated; hook chỉ trả events của Action (ActionPerformed, PieceMoved, ...).
 * - Không tự cộng cycle: runner gọi TurnScheduler.completeActivation sau activation.
 * - Nếu phong cấp (type đổi PAWN -> QUEEN), hook đặt nextActivationTurn = T+5 trong cùng Action;
 *   runner thấy type đổi nên giữ nguyên lịch đó (I09).
 * - Không tăng globalTurn, không tự kết thúc trận: runner check terminal ngay sau activation.
 * - Không Attack sau Move (R-P05).
 */
@FunctionalInterface
public interface PawnActivation {

    /**
     * [TẠM - T-03.01/T-03.02] Pawn bỏ activation (lịch vẫn tiến theo cycle)
     * cho tới khi T-03.03 cắm cài đặt thật vào EnemyTurnRunner.
     */
    PawnActivation SKIP = (state, pawnId) -> List.of();

    List<BattleEvent> activate(BattleState state, String pawnId);
}
