package rogueknight.ai;

import java.util.Optional;
import rogueknight.action.ActionRequest;
import rogueknight.action.EnemyDecision;
import rogueknight.action.EnemyPolicy;
import rogueknight.action.EnemyRules;
import rogueknight.action.PawnPathfinder;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;
import rogueknight.board.Position;

/*
 * PawnPolicy.java  |  T-03.03 tích hợp vào T-03.01  |  TD: 2.5 (R-P02..R-P06), 5.5, 5.10, DEC-01
 *
 * AI của Pawn. Thuần đọc; chỉ trả request, Resolver mới đổi hướng và phong cấp.
 * 1. Knight nằm trong ba ô theo hướng ĐANG LƯU (không tính hướng mới trước) -> ATTACK.
 * 2. Nếu không, bước đầu của đường BFS ngắn nhất tới hàng 1 (PawnPathfinder: bỏ qua Knight khi tìm đường,
 *    không bước vào ô Knight, ưu tiên SOUTH rồi column/row) -> MOVE một ô.
 * 3. Không có bước hợp lệ -> Skip. Fallback "không có đường phong cấp" của DEC-01 chưa được định nghĩa
 *    trong repo nên chưa cài; Pawn hiện bỏ activation trong trường hợp đó.
 * Không Attack sau Move: EnemyTurnRunner chỉ xét post-Move Attack cho Enemy generic.
 */
public final class PawnPolicy implements EnemyPolicy {

    @Override
    public EnemyDecision choose(StateView view, String pawnId) {
        Optional<PieceState> pawn = view.pieceState(pawnId);
        if (pawn.isEmpty() || pawn.get().type() != PieceType.PAWN) return EnemyDecision.skip("NOT_A_PAWN");
        Optional<Position> knight = EnemyRules.knightPosition(view);
        if (knight.isEmpty()) return EnemyDecision.skip("NO_KNIGHT");

        if (EnemyRules.canAttackKnight(view, pawnId)) {
            return EnemyDecision.act(ActionRequest.attack(pawnId, knight.get()));
        }
        return PawnPathfinder.findNextMove(view, pawnId)
                .map(next -> EnemyDecision.act(ActionRequest.move(pawnId, next.target())))
                .orElseGet(() -> EnemyDecision.skip("NO_STEP_TOWARD_PROMOTION"));
    }
}
