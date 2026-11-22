package rogueknight.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import rogueknight.action.ActionRequest;
import rogueknight.action.EnemyDecision;
import rogueknight.action.EnemyPolicy;
import rogueknight.action.EnemyRules;
import rogueknight.action.KnightRules;
import rogueknight.battle.PieceState;
import rogueknight.battle.StateView;
import rogueknight.board.Position;

/*
 * GenericEnemyPolicy.java  |  T-03.02  |  TD: 2.4 (R-E02..R-E07), 4.7 (C-03), 5.4, DEC-02
 *
 * AI của Rook, Bishop, King, Queen. Thuần đọc, deterministic, chỉ chọn trong lựa chọn của EnemyRules.
 * 1. Knight đang trong attack range -> ATTACK (kết liễu cũng là trường hợp này vì damage luôn 1).
 * 2. Không có legal Move -> Skip.
 * 3. Xếp hạng mọi legal Move theo DEC-02 (đã chốt theo đề xuất TD 5.4), ưu tiên giảm dần:
 *    a. moveAttack: từ ô đích đánh được Knight ngay sau Move;
 *    b. safe: Knight không default-Capture được Enemy ở ô đích (ô L và HP <= 2, dùng luật Capture thật);
 *    c. threat: từ ô đích, lần activation kế có ít nhất một Move -> Attack nếu các quân khác đứng yên;
 *    d. khoảng cách Chebyshev max(|dx|,|dy|) tới Knight nhỏ hơn;
 *    e. column nhỏ hơn, rồi row nhỏ hơn.
 * Còn legal Move thì luôn đi (TD 5.4); post-Move Attack do EnemyTurnRunner xét trên state sau Move.
 */
public final class GenericEnemyPolicy implements EnemyPolicy {

    /** Facts about one candidate destination, evaluated on a hypothetical view. */
    record Candidate(Position destination, boolean moveAttack, boolean safe, boolean threat, int distance) { }

    /** DEC-02 ranking; the smallest element is the best move. Booleans are negated so {@code true} sorts first. */
    static final Comparator<Candidate> RANKING = Comparator
            .comparing((Candidate candidate) -> !candidate.moveAttack())
            .thenComparing(candidate -> !candidate.safe())
            .thenComparing(candidate -> !candidate.threat())
            .thenComparingInt(Candidate::distance)
            .thenComparingInt(candidate -> candidate.destination().column())
            .thenComparingInt(candidate -> candidate.destination().row());

    @Override
    public EnemyDecision choose(StateView view, String enemyId) {
        Optional<PieceState> enemy = view.pieceState(enemyId);
        if (enemy.isEmpty() || !EnemyRules.isGenericEnemy(enemy.get().type())) {
            return EnemyDecision.skip("NOT_A_GENERIC_ENEMY");
        }
        Optional<Position> knight = EnemyRules.knightPosition(view);
        if (knight.isEmpty()) return EnemyDecision.skip("NO_KNIGHT");

        if (EnemyRules.canAttackKnight(view, enemyId)) {
            return EnemyDecision.act(ActionRequest.attack(enemyId, knight.get()));
        }

        List<Position> moves = EnemyRules.moveDestinations(view, enemyId);
        if (moves.isEmpty()) return EnemyDecision.skip("NO_LEGAL_ACTION");

        Candidate best = moves.stream()
                .map(destination -> evaluate(view, enemy.get(), destination, knight.get()))
                .min(RANKING)
                .orElseThrow();
        return EnemyDecision.act(ActionRequest.move(enemyId, best.destination()));
    }

    static Candidate evaluate(StateView view, PieceState enemy, Position destination, Position knight) {
        StateView afterMove = new HypotheticalView(view, enemy.id(), destination);
        boolean moveAttack = EnemyRules.canAttackKnight(afterMove, enemy.id());
        boolean safe = !KnightRules.canDefaultCapture(knight, destination, enemy.hp());
        boolean threat = EnemyRules.moveDestinations(afterMove, enemy.id()).stream()
                .anyMatch(next -> EnemyRules.canAttackKnight(new HypotheticalView(afterMove, enemy.id(), next), enemy.id()));
        return new Candidate(destination, moveAttack, safe, threat, chebyshev(destination, knight));
    }

    static int chebyshev(Position a, Position b) {
        return Math.max(Math.abs(a.column() - b.column()), Math.abs(a.row() - b.row()));
    }
}
