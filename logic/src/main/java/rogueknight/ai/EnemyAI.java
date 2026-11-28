package rogueknight.ai;

import rogueknight.action.EnemyDecision;
import rogueknight.action.EnemyPolicy;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;

/*
 * EnemyAI.java  |  T-03.01-T-03.03  |  TD: 3.1 (ai), 4.7 (C-03), 6.1, 6.6
 *
 * Chọn policy theo loại quân hiện tại rồi gọi choose; không tự sửa state.
 * Pawn -> PawnPolicy; Rook/Bishop/King/Queen -> GenericEnemyPolicy (Queen phong cấp dùng policy generic
 * từ activation sau, vì EnemyTurnRunner hỏi lại loại quân mỗi activation).
 * BattleFactory inject policy này vào BattleController; runner nội bộ thực thi activation.
 */
public final class EnemyAI implements EnemyPolicy {

    private final EnemyPolicy pawnPolicy = new PawnPolicy();
    private final EnemyPolicy genericPolicy = new GenericEnemyPolicy();

    @Override
    public EnemyDecision choose(StateView view, String enemyId) {
        boolean isPawn = view.pieceState(enemyId).map(piece -> piece.type() == PieceType.PAWN).orElse(false);
        return isPawn ? pawnPolicy.choose(view, enemyId) : genericPolicy.choose(view, enemyId);
    }
}
