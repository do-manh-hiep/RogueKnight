package rogueknight.action;

import rogueknight.battle.BattleResult;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;

/*
 * ResultEvaluator.java  |  T-03.01  |  TD: 2.7 (R-R01..R-R03), 4.8 (C-06), 5.2, DEC-11
 *
 * Tính kết quả trận từ state sau một Action đã resolve trọn. Thuần đọc;
 * BattleController (và EnemyTurnRunner thay mặt nó) mới ghi result/phase và phát BattleEnded.
 * - DEFEAT: không còn Knight sống (Knight HP về 0 bị gỡ khỏi Board như mọi quân).
 * - VICTORY: King chết, hoặc không còn Enemy non-King sống (Queen phong cấp tính là non-King).
 * - DEC-11 (đã chốt): state bất thường thỏa cả hai thì DEFEAT được ưu tiên.
 * Lưu ý fixture: thiếu King hoặc thiếu Enemy non-King ngay từ đầu sẽ thành VICTORY sau Action đầu tiên.
 */
public final class ResultEvaluator {

    private ResultEvaluator() { }

    public static BattleResult evaluate(StateView view) {
        boolean knightAlive = false;
        boolean kingAlive = false;
        boolean nonKingEnemyAlive = false;

        for (PieceState piece : view.livePieces()) {
            if (piece.hp() <= 0) continue;
            if (piece.type() == PieceType.KNIGHT) knightAlive = true;
            else if (piece.type() == PieceType.KING) kingAlive = true;
            else nonKingEnemyAlive = true;
        }

        if (!knightAlive) return BattleResult.DEFEAT; // DEC-11: Defeat takes priority.
        if (!kingAlive || !nonKingEnemyAlive) return BattleResult.VICTORY;
        return BattleResult.ONGOING;
    }
}
