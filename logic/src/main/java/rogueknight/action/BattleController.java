package rogueknight.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.board.Position;

/*
 * BattleController.java  |  T-02.01  |  TD: 4.6, 5.2, DEC-04, DEC-08
 *
 * Cửa vào duy nhất cho Player Action: submit(request) -> ActionResult (snapshot + events).
 * Luồng: kiểm tra result/phase -> payload/kind -> KnightRules.validateMove
 *        -> ActionResolver.resolve -> tăng turn đúng một lần -> EnemyPhase -> PLAYER_SELECT.
 * - Mọi lỗi trả trước khi mutate, nên request bị từ chối giữ nguyên toàn bộ state
 *   (vị trí, HP, charge, turn, phase, lịch).
 * - Resolve đồng bộ một submit rồi trả batch events (đề xuất DEC-08).
 * - [TẠM - T-02.01] Chưa có evaluator thắng/thua và lượt địch thật; EnemyPhase.NONE
 *   mặc định. T-03.01 thêm terminal check sau từng action và queue Enemy.
 * Từ khóa tìm kiếm: submit, legalMoves, snapshot.
 */
public final class BattleController {

    private final BattleState state;
    private final EnemyPhase enemyPhase;

    public BattleController(BattleState state) {
        this(state, EnemyPhase.NONE);
    }

    public BattleController(BattleState state, EnemyPhase enemyPhase) {
        this.state = Objects.requireNonNull(state, "state");
        this.enemyPhase = Objects.requireNonNull(enemyPhase, "enemyPhase");
    }

    public BattleSnapshot snapshot() {
        return state.snapshot();
    }

    /** Ô đích Move hợp lệ cho highlight; rỗng khi không nhận Player Action. */
    public Set<Position> legalMoves(String actorId) {
        if (!acceptsPlayerAction()) {
            return Set.of();
        }
        return KnightRules.legalMoveTargets(state, actorId);
    }

    public ActionResult submit(ActionRequest request) {
        BattleSnapshot before = state.snapshot();
        if (state.result() != BattleResult.ONGOING) {
            return ActionResult.rejected(ActionError.BATTLE_FINISHED, before);
        }
        if (state.phase() != BattlePhase.PLAYER_SELECT) {
            return ActionResult.rejected(ActionError.WRONG_PHASE, before);
        }
        if (request == null || request.actorId() == null || request.kind() == null) {
            return ActionResult.rejected(ActionError.INVALID_PAYLOAD, before);
        }
        if (request.kind() != ActionKind.MOVE) {
            return ActionResult.rejected(ActionError.UNSUPPORTED_ACTION, before);
        }
        ValidationOutcome outcome = KnightRules.validateMove(state, request);
        if (!outcome.isValid()) {
            return ActionResult.rejected(outcome.error(), before);
        }

        List<BattleEvent> events = new ArrayList<>();
        state.setPhase(BattlePhase.PLAYER_RESOLVE);
        events.addAll(ActionResolver.resolve(state, outcome.plan()));
        // [TẠM] Terminal check sau Player Action thuộc T-03.01.
        state.advanceTurn();
        state.setPhase(BattlePhase.ENEMY_RESOLVE);
        events.addAll(enemyPhase.run(state));
        if (state.result() == BattleResult.ONGOING) {
            state.setPhase(BattlePhase.PLAYER_SELECT);
        }
        return ActionResult.accepted(before, state.snapshot(), events);
    }

    private boolean acceptsPlayerAction() {
        return state.result() == BattleResult.ONGOING && state.phase() == BattlePhase.PLAYER_SELECT;
    }
}
