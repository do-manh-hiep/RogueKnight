package rogueknight.battle;

import rogueknight.ai.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.action.ActionRequest;
import rogueknight.action.EnemyDecision;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/*
 * GenericEnemyPolicyTest  |  T-03.02  |  R-E02, R-E03, R-E05..R-E07, FX-09, FX-16, DEC-02
 * DEC-02 đã chốt theo đề xuất TD: moveAttack > safe > threat > Chebyshev > column > row.
 * Expected của mỗi fixture được tính tay trong comment, không gọi lại policy để tính expected.
 */
class GenericEnemyPolicyTest {

    private final GenericEnemyPolicy policy = new GenericEnemyPolicy();

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState battle(Set<Position> rocks) {
        return new BattleState(rocks, Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
    }

    private static PieceState knight(int hp) {
        return new PieceState("K", PieceType.KNIGHT, hp, 0, 0, 0);
    }

    private static PieceState enemy(String id, PieceType type, int hp, int spawnOrder) {
        return new PieceState(id, type, hp, 3, 3, spawnOrder, null);
    }

    private static EnemyDecision move(String id, String square) {
        return EnemyDecision.act(ActionRequest.move(id, at(square)));
    }

    @Test
    void attacksImmediatelyWhenKnightIsInRange() {
        BattleState state = battle(Set.of());
        state.addPiece(knight(5), at("d3"));
        state.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("d4"));
        state.addPiece(enemy("KG", PieceType.KING, 5, 2), at("h8"));

        assertEquals(EnemyDecision.act(ActionRequest.attack("R", at("d3"))), policy.choose(state, "R"));
    }

    @Test
    void choosesTheSquareThatAllowsMoveThenAttack() {
        // TD 5.12B: Knight c2, Rook d4. Orthogonal neighbours of c2 are b2, c1, c3, d2; only d2 is reachable.
        BattleState state = battle(Set.of());
        state.addPiece(knight(1), at("c2"));
        state.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("d4"));
        state.addPiece(enemy("B", PieceType.BISHOP, 2, 2), at("f6"));
        state.addPiece(enemy("KG", PieceType.KING, 5, 3), at("h8"));

        assertEquals(move("R", "d2"), policy.choose(state, "R"));
    }

    @Test
    void raysStopBeforeTheKnightAndTheBestSquareIsAdjacent() {
        // Rook a4 with Knight d4 on the same rank: legal east squares are b4 and c4 only; c4 attacks next.
        BattleState state = battle(Set.of(at("a3"), at("a5")));
        state.addPiece(knight(5), at("d4"));
        state.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("a4"));
        state.addPiece(enemy("KG", PieceType.KING, 5, 2), at("h8"));

        assertEquals(move("R", "c4"), policy.choose(state, "R"));
    }

    @Test
    void avoidsKnightCaptureSquareOnlyWhenEnemyHpIsAtMostTwo() {
        // Knight d4, Rook b8, King h1. No square allows Move->Attack.
        // Threat squares at distance 2: b3, b4, b5 (next activation reaches d3, c4, d5).
        // b3 and b5 are L-squares from d4, so with HP 2 the Knight could Capture there.
        BattleState hp2 = battle(Set.of());
        hp2.addPiece(knight(5), at("d4"));
        hp2.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("b8"));
        hp2.addPiece(enemy("KG", PieceType.KING, 5, 2), at("h1"));
        assertEquals(move("R", "b4"), policy.choose(hp2, "R"), "safe beats lexicographic order");

        // With HP 3 the default Capture is impossible, so every square is safe and b3 wins by column/row.
        BattleState hp3 = battle(Set.of());
        hp3.addPiece(knight(5), at("d4"));
        hp3.addPiece(enemy("R", PieceType.ROOK, 3, 1), at("b8"));
        hp3.addPiece(enemy("KG", PieceType.KING, 5, 2), at("h1"));
        assertEquals(move("R", "b3"), policy.choose(hp3, "R"), "capture safety must check HP <= 2");
    }

    @Test
    void skipsWhenThereIsNoLegalMove() {
        BattleState state = battle(Set.of(at("a2"), at("b1")));
        state.addPiece(knight(5), at("e5"));
        state.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("a1"));
        state.addPiece(enemy("KG", PieceType.KING, 5, 2), at("h8"));

        assertEquals(EnemyDecision.skip("NO_LEGAL_ACTION"), policy.choose(state, "R"));
    }

    @Test
    void rankingOrderMatchesDec02() {
        GenericEnemyPolicy.Candidate moveAttack = candidate("h8", true, false, false, 7);
        GenericEnemyPolicy.Candidate safeThreatC2 = candidate("c2", false, true, true, 2);
        GenericEnemyPolicy.Candidate safeThreatC5 = candidate("c5", false, true, true, 2);
        GenericEnemyPolicy.Candidate safeThreatFar = candidate("a1", false, true, true, 6);
        GenericEnemyPolicy.Candidate safeOnlyNear = candidate("a2", false, true, false, 1);
        GenericEnemyPolicy.Candidate unsafeThreatNear = candidate("a3", false, false, true, 1);
        List<GenericEnemyPolicy.Candidate> shuffled = new ArrayList<>(List.of(
                unsafeThreatNear, safeOnlyNear, safeThreatFar, safeThreatC5, moveAttack, safeThreatC2));
        Collections.reverse(shuffled);

        shuffled.sort(GenericEnemyPolicy.RANKING);

        assertEquals(List.of(moveAttack, safeThreatC2, safeThreatC5, safeThreatFar, safeOnlyNear, unsafeThreatNear),
                shuffled);
    }

    @Test
    void sameStateGivesSameDecisionRegardlessOfInsertionOrderAndDoesNotMutate() {
        BattleState first = battle(Set.of());
        first.addPiece(knight(1), at("c2"));
        first.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("d4"));
        first.addPiece(enemy("B", PieceType.BISHOP, 2, 2), at("f6"));
        first.addPiece(enemy("KG", PieceType.KING, 5, 3), at("h8"));

        BattleState second = battle(Set.of());
        second.addPiece(enemy("KG", PieceType.KING, 5, 3), at("h8"));
        second.addPiece(enemy("B", PieceType.BISHOP, 2, 2), at("f6"));
        second.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("d4"));
        second.addPiece(knight(1), at("c2"));

        BattleSnapshot before = first.snapshot();
        EnemyDecision decision = policy.choose(first, "R");

        assertEquals(decision, policy.choose(first, "R"));
        assertEquals(decision, policy.choose(second, "R"));
        assertEquals(decision, policy.choose(second, "R"));
        assertEquals(before, first.snapshot());
    }

    @Test
    void hypotheticalViewMovesOnlyTheChosenPieceAndRejectsOccupiedSquares() {
        BattleState state = battle(Set.of(at("e4")));
        state.addPiece(knight(5), at("c2"));
        state.addPiece(enemy("R", PieceType.ROOK, 2, 1), at("d4"));
        BattleSnapshot before = state.snapshot();

        HypotheticalView view = new HypotheticalView(state, "R", at("d2"));

        assertEquals(at("d2"), view.positionOf("R").orElseThrow());
        assertEquals("R", view.pieceAt(at("d2")).orElseThrow());
        assertEquals(true, view.isEmpty(at("d4")));
        assertEquals(at("c2"), view.positionOf("K").orElseThrow());
        assertEquals(before, state.snapshot());
        assertThrows(IllegalArgumentException.class, () -> new HypotheticalView(state, "R", at("c2")));
        assertThrows(IllegalArgumentException.class, () -> new HypotheticalView(state, "R", at("e4")));
    }

    private static GenericEnemyPolicy.Candidate candidate(String square, boolean moveAttack, boolean safe,
                                                          boolean threat, int distance) {
        return new GenericEnemyPolicy.Candidate(at(square), moveAttack, safe, threat, distance);
    }
}
