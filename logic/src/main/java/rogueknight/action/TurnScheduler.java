package rogueknight.action;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;

/*
 * TurnScheduler.java  |  T-03.01  |  TD: 2.1 (R-T04, R-T07), 4.8 (C-05), 5.3, DEC-05, DEC-07
 *
 * Lịch kích hoạt Enemy theo nextActivationTurn tường minh (DEC-05):
 * - Factory đặt nextActivationTurn = cycle (Pawn 2, Rook/Bishop 3, King 5).
 * - Enemy sống có nextActivationTurn <= globalTurn thì đến hạn.
 * - Xong activation hoặc skip: nextActivationTurn += cycle. Không dùng globalTurn % cycle.
 * - Promotion tự đặt lịch T+5 trong cùng Action; runner không gọi completeActivation để khỏi ghi đè.
 * Thứ tự queue (DEC-07): PAWN, ROOK, BISHOP, KING, QUEEN, rồi spawnOrder, rồi id; không dựa thứ tự HashMap.
 * Scheduler không tăng globalTurn: chỉ BattleController làm việc đó.
 */
public final class TurnScheduler {

    private static final Map<PieceType, Integer> QUEUE_RANK = Map.of(
            PieceType.PAWN, 0,
            PieceType.ROOK, 1,
            PieceType.BISHOP, 2,
            PieceType.KING, 3,
            PieceType.QUEEN, 4);

    private static final Comparator<PieceState> QUEUE_ORDER =
            Comparator.comparingInt((PieceState piece) -> QUEUE_RANK.get(piece.type()))
                    .thenComparingInt(PieceState::spawnOrder)
                    .thenComparing(PieceState::id);

    private TurnScheduler() { }

    /** IDs of live Enemies due at the current global turn, in the fixed queue order. Read-only. */
    public static List<String> dueEnemyIds(StateView view) {
        int turn = view.globalTurn();
        return view.livePieces().stream()
                .filter(piece -> QUEUE_RANK.containsKey(piece.type()))
                .filter(piece -> piece.hp() > 0 && piece.nextActivationTurn() <= turn)
                .sorted(QUEUE_ORDER)
                .map(PieceState::id)
                .toList();
    }

    /** Ends one normal activation or skip: the next activation is one cycle later. */
    static void completeActivation(BattleState state, String enemyId) {
        PieceState enemy = state.pieceState(enemyId).orElseThrow(
                () -> new IllegalArgumentException("Unknown enemy: " + enemyId));
        state.replacePiece(enemy.withNextActivationTurn(enemy.nextActivationTurn() + enemy.cycle()));
    }
}
