package rogueknight.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

/*
 * TurnSchedulerTest  |  T-03.01  |  FX-08, R-T04, DEC-05, DEC-07
 * Scheduler thuần: không resolve chiến đấu, nên Knight không thể chết giữa chừng làm sai mục đích kiểm tra clock.
 */
class TurnSchedulerTest {

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState emptyBattle() {
        return new BattleState(Set.of(), Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
    }

    /** Map01 cycles, inserted deliberately out of queue order. */
    private static BattleState map01Schedule() {
        BattleState state = emptyBattle();
        state.addPiece(new PieceState("king-1", PieceType.KING, 5, 5, 5, 5), at("d8"));
        state.addPiece(new PieceState("bishop-1", PieceType.BISHOP, 2, 3, 3, 4), at("c8"));
        state.addPiece(new PieceState("pawn-2", PieceType.PAWN, 1, 2, 2, 2), at("d7"));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), at("d1"));
        state.addPiece(new PieceState("rook-1", PieceType.ROOK, 2, 3, 3, 3), at("h8"));
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1), at("c7"));
        return state;
    }

    @Test
    void map01TimelineForTurnsOneToSix() {
        BattleState state = map01Schedule();
        Map<Integer, List<String>> expected = Map.of(
                1, List.of(),
                2, List.of("pawn-1", "pawn-2"),
                3, List.of("rook-1", "bishop-1"),
                4, List.of("pawn-1", "pawn-2"),
                5, List.of("king-1"),
                6, List.of("pawn-1", "pawn-2", "rook-1", "bishop-1"));

        for (int turn = 1; turn <= 6; turn++) {
            assertEquals(turn, state.globalTurn());
            List<String> due = TurnScheduler.dueEnemyIds(state);
            assertEquals(expected.get(turn), due, "queue at turn " + turn);
            due.forEach(id -> TurnScheduler.completeActivation(state, id));
            state.advanceTurn();
        }
    }

    @Test
    void queueOrderIsTypeRankThenSpawnOrderNotInsertionOrder() {
        BattleState state = emptyBattle();
        state.addPiece(new PieceState("K", PieceType.KNIGHT, 5, 0, 0, 0), at("a1"));
        state.addPiece(new PieceState("Q", PieceType.QUEEN, 2, 4, 1, 1, null), at("b1"));
        state.addPiece(new PieceState("KG", PieceType.KING, 5, 5, 1, 2), at("c1"));
        state.addPiece(new PieceState("B", PieceType.BISHOP, 2, 3, 1, 3), at("d1"));
        state.addPiece(new PieceState("R", PieceType.ROOK, 2, 3, 1, 4), at("e1"));
        state.addPiece(new PieceState("P6", PieceType.PAWN, 1, 2, 1, 6), at("f1"));
        state.addPiece(new PieceState("P5", PieceType.PAWN, 1, 2, 1, 5), at("g1"));

        assertEquals(List.of("P5", "P6", "R", "B", "KG", "Q"), TurnScheduler.dueEnemyIds(state));
    }

    @Test
    void removedEnemyAndKnightAreNeverQueued() {
        BattleState state = map01Schedule();
        state.advanceTurn(); // Turn 2: both Pawns due
        state.removePiece("pawn-1");

        List<String> due = TurnScheduler.dueEnemyIds(state);

        assertEquals(List.of("pawn-2"), due);
        assertFalse(due.contains("knight"));
    }

    @Test
    void completeActivationAddsOneCycleWithoutTouchingTurn() {
        BattleState state = map01Schedule();

        TurnScheduler.completeActivation(state, "rook-1");

        assertEquals(6, state.pieceState("rook-1").orElseThrow().nextActivationTurn());
        assertEquals(1, state.globalTurn());
    }
}
