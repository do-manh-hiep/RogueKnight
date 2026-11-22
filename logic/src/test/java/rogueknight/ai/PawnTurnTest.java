package rogueknight.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.action.ActionKind;
import rogueknight.action.ActionRequest;
import rogueknight.action.ActionResult;
import rogueknight.action.BattleController;
import rogueknight.action.BattleEvent;
import rogueknight.action.EnemyTurnRunner;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.Direction;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/*
 * PawnTurnTest  |  T-03.03 trong luồng T-03.01  |  R-P02..R-P08, FX-10..FX-12, IT-05, I09, DEC-01, DEC-05
 *
 * Pawn chạy qua đúng luồng của mọi Enemy: BattleController -> EnemyTurnRunner -> PawnPolicy ->
 * EnemyRules.validate -> ActionResolver (hướng, phong cấp) -> check terminal. Không stub.
 * Thay cho PawnEnemyPhaseTest và hai test executeAttack cũ; giữ nguyên ý nghĩa từng tình huống.
 * Fixture: King h8 có nextActivationTurn = 99 để không bao giờ đến hạn, chỉ giữ trận ONGOING.
 */
class PawnTurnTest {

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState battle(Set<Position> rocks, Position knight, int knightHp) {
        BattleState state = new BattleState(rocks,
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, knightHp, 0, 0, 0), knight);
        state.addPiece(new PieceState("king-1", PieceType.KING, 5, 5, 99, 9), at("h8"));
        return state;
    }

    private static PieceState pawn(String id, Direction facing, int nextTurn, int spawnOrder) {
        return new PieceState(id, PieceType.PAWN, 1, 2, nextTurn, spawnOrder, facing);
    }

    private static BattleController controller(BattleState state) {
        return new BattleController(state, new EnemyTurnRunner(new EnemyAI()));
    }

    private static ActionResult waitOneTurn(BattleController controller) {
        ActionResult result = controller.submit(ActionRequest.waitAction("knight"));
        assertTrue(result.accepted());
        return result;
    }

    @Test
    void pawnStepsStraightSouthAndSchedulesItsNextCycle() {
        BattleState state = battle(Set.of(), at("d1"), 5);
        state.addPiece(pawn("pawn-1", Direction.SOUTH, 2, 1), at("d7"));

        ActionResult result = waitOneTurn(controller(state));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, "knight"),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("pawn-1", at("d7"), 2),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "pawn-1"),
                new BattleEvent.PieceMoved("pawn-1", at("d7"), at("d6"))), result.events());
        BattleSnapshot.PieceView pawn = result.after().pieceById("pawn-1").orElseThrow();
        assertEquals(4, pawn.nextActivationTurn());
        assertEquals(Direction.SOUTH, pawn.currentDirection());
    }

    @Test
    void pawnAttacksKnightInsideItsConeWithoutMoving() {
        BattleState state = battle(Set.of(), at("d2"), 5);
        state.addPiece(pawn("pawn-1", Direction.SOUTH, 2, 1), at("d3"));

        ActionResult result = waitOneTurn(controller(state));

        List<BattleEvent> events = result.events();
        assertEquals(List.of(
                new BattleEvent.ActorActivated("pawn-1", at("d3"), 2),
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "pawn-1"),
                new BattleEvent.PieceDamaged("knight", 1, 4)), events.subList(2, events.size()));
        BattleSnapshot.PieceView pawn = result.after().pieceById("pawn-1").orElseThrow();
        assertEquals(at("d3"), pawn.position());
        assertEquals(4, pawn.nextActivationTurn());
    }

    @Test
    void pawnDoesNotAttackAfterMovingIntoRange() {
        // FX-10: Pawn d7 -> d6; its new cone (d5, c5, e5) holds the Knight on c5, but no Attack after Move.
        BattleState state = battle(Set.of(), at("c5"), 5);
        state.addPiece(pawn("pawn-1", Direction.SOUTH, 2, 1), at("d7"));

        ActionResult result = waitOneTurn(controller(state));

        List<BattleEvent> events = result.events();
        assertEquals(new BattleEvent.PieceMoved("pawn-1", at("d7"), at("d6")), events.get(events.size() - 1));
        assertEquals(5, result.after().pieceById("knight").orElseThrow().hp());
    }

    @Test
    void pawnKeepsItsStoredDirectionAndRoutesAroundTheKnightOnTheFirstStep() {
        // R-P02: facing EAST (e4, e5, e6), the Knight directly south is not attackable; no re-aiming first.
        // R-P06: d4 is on a shortest path but holds the Knight, so the first step is c4 or e4 (3 steps each);
        // without a straight SOUTH step the column/row tie-break picks c4.
        BattleState state = battle(Set.of(), at("d4"), 5);
        state.addPiece(pawn("pawn-1", Direction.EAST, 2, 1), at("d5"));

        ActionResult result = waitOneTurn(controller(state));

        BattleSnapshot.PieceView pawn = result.after().pieceById("pawn-1").orElseThrow();
        assertEquals(at("c4"), pawn.position());
        assertEquals(Direction.SOUTH_WEST, pawn.currentDirection());
        assertEquals(5, result.after().pieceById("knight").orElseThrow().hp());
    }

    @Test
    void promotionAtTurnSixThenQueenActsOnTurnsElevenAndFifteen() {
        // IT-05 / FX-12: Pawn a2 -> a1 at Turn 6 becomes a Queen (same ID, HP 2, cycle 4, next 11). The Queen on a1
        // is next to the Knight on b2 but does not attack in the promotion activation, nor on 7..10.
        BattleState state = battle(Set.of(), at("b2"), 5);
        state.addPiece(pawn("pawn-1", Direction.SOUTH, 6, 1), at("a2"));
        for (int turn = 1; turn < 5; turn++) state.advanceTurn(); // fixture starts at Turn 5
        BattleController controller = controller(state);

        ActionResult turn6 = waitOneTurn(controller);

        assertEquals(List.of(
                new BattleEvent.ActorActivated("pawn-1", at("a2"), 6),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "pawn-1"),
                new BattleEvent.PieceMoved("pawn-1", at("a2"), at("a1")),
                new BattleEvent.PiecePromoted("pawn-1", PieceType.PAWN, PieceType.QUEEN, at("a1"))),
                turn6.events().subList(2, turn6.events().size()));
        BattleSnapshot.PieceView queen = turn6.after().pieceById("pawn-1").orElseThrow();
        assertEquals(PieceType.QUEEN, queen.type());
        assertEquals(2, queen.hp());
        assertEquals(4, queen.cycle());
        assertEquals(11, queen.nextActivationTurn());
        assertEquals(5, turn6.after().pieceById("knight").orElseThrow().hp());

        List<Integer> activations = new ArrayList<>(activationTurns(turn6, "pawn-1"));
        ActionResult last = turn6;
        while (last.after().globalTurn() < 16) {
            last = waitOneTurn(controller);
            activations.addAll(activationTurns(last, "pawn-1"));
        }

        assertEquals(List.of(6, 11, 15), activations, "never 7, 8 or 12 (no modulo, no overwritten schedule)");
        assertEquals(19, last.after().pieceById("pawn-1").orElseThrow().nextActivationTurn());
        assertEquals(3, last.after().pieceById("knight").orElseThrow().hp(), "the Queen attacked on 11 and 15 only");
    }

    @Test
    void pawnAttackKillingTheKnightIsDefeatAndLaterEnemiesDoNotAct() {
        BattleState state = battle(Set.of(), at("d2"), 1);
        state.addPiece(pawn("pawn-1", Direction.SOUTH, 2, 1), at("d3"));
        state.addPiece(pawn("pawn-2", Direction.SOUTH, 2, 2), at("e7"));

        ActionResult result = waitOneTurn(controller(state));

        List<BattleEvent> events = result.events();
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "pawn-1"),
                new BattleEvent.PieceDamaged("knight", 1, 0),
                new BattleEvent.PieceRemoved("knight", at("d2")),
                new BattleEvent.BattleEnded(BattleResult.DEFEAT)), events.subList(events.size() - 4, events.size()));
        assertTrue(activationTurns(result, "pawn-2").isEmpty());
        BattleSnapshot after = result.after();
        assertEquals(BattleResult.DEFEAT, after.result());
        assertEquals(BattlePhase.FINISHED, after.phase());
        assertEquals(at("e7"), after.pieceById("pawn-2").orElseThrow().position());
    }

    @Test
    void trappedPawnSkipsItsActivationButTheScheduleStillAdvances() {
        BattleState state = battle(Set.of(at("a8"), at("b8"), at("b7"), at("b6"), at("a6")), at("h1"), 5);
        state.addPiece(pawn("pawn-1", Direction.SOUTH, 2, 1), at("a7"));

        ActionResult result = waitOneTurn(controller(state));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, "knight"),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("pawn-1", at("a7"), 2)), result.events());
        assertEquals(4, result.after().pieceById("pawn-1").orElseThrow().nextActivationTurn());
    }

    private static List<Integer> activationTurns(ActionResult result, String pieceId) {
        return result.events().stream()
                .filter(event -> event instanceof BattleEvent.ActorActivated activated && activated.actorId().equals(pieceId))
                .map(event -> ((BattleEvent.ActorActivated) event).turn())
                .toList();
    }
}
