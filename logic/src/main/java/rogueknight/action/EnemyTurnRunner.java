package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/*
 * EnemyTurnRunner.java  |  T-03.01 (+ sequencing T-03.02)  |  TD: 2.1, 5.3, 5.4, 5.9, 5.11, C-03..C-06
 *
 * Lượt địch thật, thay EnemyPhase.NONE. BattleController gọi run() sau khi đã tăng Turn đúng một lần.
 * - Queue = ID đến hạn theo TurnScheduler, chụp một lần đầu lượt; không thêm Queen mới vào queue đang chạy.
 * - Trước mỗi activation đọc lại state: ID đã chết thì bỏ qua (R-T04); Enemy sau thấy state mới nhất (R-T05).
 * - Generic (Rook/Bishop/King/Queen): policy chọn Action đầu tiên -> validate -> resolve -> check terminal;
 *   nếu Action đầu là MOVE thì runner tự xét Attack trên state sau Move (không hỏi policy Move lần hai).
 * - Pawn: giao trọn activation cho PawnActivation (T-03.03); mặc định SKIP.
 * - Check terminal sau MỖI Action, kể cả giữa Move và Attack; terminal thì dừng queue ngay (R-T06).
 * - Không có legal action: skip, lịch vẫn tiến, không retry trong cùng lượt (R-T07).
 * - Không tăng globalTurn theo từng Enemy (I08).
 * - DEC-12: AI chọn request invalid là bug tích hợp -> IllegalStateException, không âm thầm skip.
 */
public final class EnemyTurnRunner implements EnemyPhase {

    private final EnemyPolicy genericPolicy;
    private final PawnActivation pawnActivation;

    /** Generic Enemies use {@code genericPolicy}; Pawns skip until T-03.03 provides a PawnActivation. */
    public EnemyTurnRunner(EnemyPolicy genericPolicy) {
        this(genericPolicy, PawnActivation.SKIP);
    }

    public EnemyTurnRunner(EnemyPolicy genericPolicy, PawnActivation pawnActivation) {
        this.genericPolicy = Objects.requireNonNull(genericPolicy, "genericPolicy");
        this.pawnActivation = Objects.requireNonNull(pawnActivation, "pawnActivation");
    }

    @Override
    public List<BattleEvent> run(BattleState state) {
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

            boolean battleEnded = typeAtStart == PieceType.PAWN
                    ? activatePawn(state, enemyId, events)
                    : activateGeneric(state, enemyId, events);
            if (battleEnded) break; // a finished battle does not need its schedule completed

            completeSchedule(state, enemyId, typeAtStart);
        }
        return List.copyOf(events);
    }

    private boolean activatePawn(BattleState state, String pawnId, List<BattleEvent> events) {
        events.addAll(pawnActivation.activate(state, pawnId));
        return BattleController.endIfTerminal(state, events);
    }

    private boolean activateGeneric(BattleState state, String enemyId, List<BattleEvent> events) {
        EnemyDecision decision = genericPolicy.choose(state, enemyId);
        if (!(decision instanceof EnemyDecision.Act act)) return false; // Skip: no legal action this activation

        ActionRequest first = act.request();
        if (resolveEnemyAction(state, first, events)) return true;

        // Generic Move -> Attack (TD 5.4 step 6): only an Attack may follow, judged on the post-Move state.
        if (first.kind() == ActionKind.MOVE && EnemyRules.canAttackKnight(state, enemyId)) {
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
