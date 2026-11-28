package rogueknight.battle;

import rogueknight.action.ActionKind;
import rogueknight.action.ActionRequest;
import rogueknight.action.BattleEvent;
import rogueknight.action.EnemyDecision;
import rogueknight.action.EnemyPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.board.Position;

/*
 * EnemyTurnRunner.java  |  T-03.01 (+ sequencing T-03.02/T-03.03)  |  TD: 2.1, 5.3-5.5, 5.9-5.11, C-03..C-06
 *
 * Lượt địch thật. BattleController gọi run() sau khi đã tăng Turn đúng một lần.
 * - Queue = ID đến hạn theo TurnScheduler, chụp một lần đầu lượt; không thêm Queen mới vào queue đang chạy.
 * - Trước mỗi activation đọc lại state: ID đã chết thì bỏ qua (R-T04); Enemy sau thấy state mới nhất (R-T05).
 * - Mọi Enemy: policy chọn Action đầu tiên -> EnemyRules.validate -> ActionResolver -> check terminal.
 * - Chỉ Enemy generic (loại lúc BẮT ĐẦU activation là Rook/Bishop/King/Queen) mới được xét Attack sau Move,
 *   trên state sau Move và không hỏi policy lần hai. Pawn vừa phong Queen không đánh tiếp (R-P05).
 * - Check terminal sau MỖI Action, kể cả giữa Move và Attack; terminal thì dừng queue ngay (R-T06).
 * - Không có legal action: skip, lịch vẫn tiến, không retry trong cùng lượt (R-T07).
 * - Phong cấp đã đặt lịch T+5 trong Resolver: type đổi trong activation thì không cộng cycle (I09).
 * - Không tăng globalTurn theo từng Enemy (I08).
 * - DEC-12: AI chọn request invalid là bug tích hợp -> IllegalStateException, không âm thầm skip.
 */
final class EnemyTurnRunner {

    private final EnemyPolicy policy;

    /** {@code policy} decides for every Enemy type; the game uses rogueknight.ai.EnemyAI. */
    EnemyTurnRunner(EnemyPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    List<BattleEvent> run(BattleState state) {
        Objects.requireNonNull(state, "state");
        if (state.result() != BattleResult.ONGOING) return List.of();

        List<BattleEvent> events = new ArrayList<>();
        for (String enemyId : TurnScheduler.dueEnemyIds(state)) {
            if (state.result() != BattleResult.ONGOING) break;
            Optional<PieceState> enemy = state.pieceState(enemyId);
            if (enemy.isEmpty()) continue; // removed earlier in this submit

            // Remember the type at activation start: a Pawn promoted by its Move must not attack as a Queen now.
            PieceType typeAtStart = enemy.get().type();
            Position at = state.positionOf(enemyId).orElseThrow();
            events.add(new BattleEvent.ActorActivated(enemyId, at, state.globalTurn()));

            if (activate(state, enemyId, typeAtStart, events)) break; // a finished battle needs no schedule update
            completeSchedule(state, enemyId, typeAtStart);
        }
        return List.copyOf(events);
    }

    /** Runs one activation. Returns true when the battle ended during it. */
    private boolean activate(BattleState state, String enemyId, PieceType typeAtStart, List<BattleEvent> events) {
        EnemyDecision decision = policy.choose(state, enemyId);
        if (decision instanceof EnemyDecision.Skip) return false; // Explicit Skip is a valid no-action decision.
        if (!(decision instanceof EnemyDecision.Act act)) {
            throw new IllegalStateException("DEC-12: enemy policy returned a null or unsupported decision for "
                    + enemyId + " at turn " + state.globalTurn());
        }

        ActionRequest first = act.request();
        if (resolveEnemyAction(state, first, events)) return true;

        // Generic Move -> Attack (TD 5.4 step 6): only an Attack may follow, judged on the post-Move state.
        if (first.kind() == ActionKind.MOVE
                && EnemyRules.isGenericEnemy(typeAtStart)
                && EnemyRules.canAttackKnight(state, enemyId)) {
            Position knight = EnemyRules.knightPosition(state).orElseThrow();
            return resolveEnemyAction(state, ActionRequest.attack(enemyId, knight), events);
        }
        return false;
    }

    /** Validates and applies one Enemy Action, then checks terminal. Returns true when the battle ended. */
    private static boolean resolveEnemyAction(BattleState state, ActionRequest request, List<BattleEvent> events) {
        ValidationOutcome outcome = EnemyRules.validate(state, request);
        if (!outcome.isValid()) {
            throw new IllegalStateException("DEC-12: enemy AI chose an invalid request " + request
                    + " (" + outcome.error() + ") at turn " + state.globalTurn());
        }
        events.addAll(ActionResolver.resolve(state, outcome.plan()));
        return BattleController.endIfTerminal(state, events);
    }

    private static void completeSchedule(BattleState state, String enemyId, PieceType typeAtStart) {
        Optional<PieceState> after = state.pieceState(enemyId);
        if (after.isEmpty()) return;
        // Promotion already set the new T+5 schedule inside the same Action (DEC-05); never overwrite it.
        if (after.get().type() != typeAtStart) return;
        TurnScheduler.completeActivation(state, enemyId);
    }
}
