package rogueknight.battle;

import rogueknight.action.*;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

/*
 * ResultEvaluatorTest  |  T-03.01  |  FX-13, R-R01..R-R03, DEC-11
 * Expected suy ra từ luật GDD: King chết hoặc hết Enemy non-King -> VICTORY; Knight HP<=0 -> DEFEAT;
 * state bất thường thỏa cả hai -> DEFEAT (DEC-11 đã chốt).
 */
class ResultEvaluatorTest {

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState emptyBattle() {
        return new BattleState(Set.of(), Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
    }

    private static PieceState piece(String id, PieceType type, int spawnOrder) {
        return new PieceState(id, type, 2, 3, 3, spawnOrder);
    }

    @Test
    void knightKingAndNonKingEnemyAliveIsOngoing() {
        BattleState state = emptyBattle();
        state.addPiece(piece("K", PieceType.KNIGHT, 0), at("d1"));
        state.addPiece(piece("KG", PieceType.KING, 1), at("d8"));
        state.addPiece(piece("R", PieceType.ROOK, 2), at("h8"));

        assertEquals(BattleResult.ONGOING, ResultEvaluator.evaluate(state));
    }

    @Test
    void kingDeadIsVictoryEvenWithRookAlive() {
        BattleState state = emptyBattle();
        state.addPiece(piece("K", PieceType.KNIGHT, 0), at("d1"));
        state.addPiece(piece("R", PieceType.ROOK, 2), at("h8"));

        assertEquals(BattleResult.VICTORY, ResultEvaluator.evaluate(state));
    }

    @Test
    void onlyKingLeftIsVictory() {
        BattleState state = emptyBattle();
        state.addPiece(piece("K", PieceType.KNIGHT, 0), at("d1"));
        state.addPiece(piece("KG", PieceType.KING, 1), at("d8"));

        assertEquals(BattleResult.VICTORY, ResultEvaluator.evaluate(state));
    }

    @Test
    void promotedQueenCountsAsNonKingEnemy() {
        BattleState state = emptyBattle();
        state.addPiece(piece("K", PieceType.KNIGHT, 0), at("d1"));
        state.addPiece(piece("KG", PieceType.KING, 1), at("d8"));
        state.addPiece(new PieceState("pawn-1", PieceType.QUEEN, 2, 4, 11, 2, null), at("a1"));

        assertEquals(BattleResult.ONGOING, ResultEvaluator.evaluate(state));
    }

    @Test
    void knightGoneIsDefeat() {
        BattleState state = emptyBattle();
        state.addPiece(piece("KG", PieceType.KING, 1), at("d8"));
        state.addPiece(piece("R", PieceType.ROOK, 2), at("h8"));

        assertEquals(BattleResult.DEFEAT, ResultEvaluator.evaluate(state));
    }

    @Test
    void knightAndKingBothGoneIsDefeatByDec11() {
        BattleState state = emptyBattle();
        state.addPiece(piece("R", PieceType.ROOK, 2), at("h8"));

        assertEquals(BattleResult.DEFEAT, ResultEvaluator.evaluate(state));
    }

    @Test
    void evaluateDoesNotMutateState() {
        BattleState state = emptyBattle();
        state.addPiece(piece("K", PieceType.KNIGHT, 0), at("d1"));
        state.addPiece(piece("R", PieceType.ROOK, 2), at("h8"));
        BattleSnapshot before = state.snapshot();

        ResultEvaluator.evaluate(state);

        assertEquals(before, state.snapshot());
    }
}
