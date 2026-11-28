package rogueknight.battle;

import rogueknight.action.*;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

/*
 * BattleControllerTest  |  T-02.01
 * Điều phối submit: actor, phase, result, invalid không mutate, turn tăng đúng một lần.
 * Fixture: Knight K1 (HP3) ở d4, King KG ở h8, Rook R1 ở a8; loadout đã dùng một charge.
 */
class BattleControllerTest {

    private static final String KNIGHT = "K1";

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState fixture() {
        BattleState state = new BattleState(Set.of(at("c4")),
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER));
        state.addPiece(new PieceState(KNIGHT, PieceType.KNIGHT, 3, 0, 0, 0), at("d4"));
        state.addPiece(new PieceState("KG", PieceType.KING, 5, 5, 5, 1), at("h8"));
        state.addPiece(new PieceState("R1", PieceType.ROOK, 2, 3, 3, 2), at("a8"));
        state.consumeEquipment(EquipmentType.CANNON_MOVEMENT);
        return state;
    }

    private static void assertRejectedUnchanged(ActionResult result, ActionError expected,
                                                BattleSnapshot before, BattleController controller) {
        assertFalse(result.accepted());
        assertEquals(expected, result.error());
        assertEquals(before, controller.snapshot());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void unknownActorIsRejectedWithoutMutation() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        assertRejectedUnchanged(controller.submit(ActionRequest.move("ghost", at("e6"))),
                ActionError.UNKNOWN_ACTOR, before, controller);
    }

    @Test
    void enemyActorIsNotAPlayerActor() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        // a8 -> b7 sẽ là ô kề hợp lệ nếu là Knight.
        assertRejectedUnchanged(controller.submit(ActionRequest.move("R1", at("b7"))),
                ActionError.NOT_PLAYER_ACTOR, before, controller);
    }

    @Test
    void missingPayloadIsRejected() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        assertRejectedUnchanged(controller.submit(null), ActionError.INVALID_PAYLOAD, before, controller);
        assertRejectedUnchanged(controller.submit(new ActionRequest(null, ActionKind.MOVE, 4, 5)),
                ActionError.INVALID_PAYLOAD, before, controller);
        assertRejectedUnchanged(controller.submit(new ActionRequest(KNIGHT, null, 4, 5)),
                ActionError.INVALID_PAYLOAD, before, controller);
    }

    @Test
    void submitOutsidePlayerSelectIsRejected() {
        BattleState state = fixture();
        state.setPhase(BattlePhase.ENEMY_RESOLVE);
        BattleController controller = new BattleController(state);
        BattleSnapshot before = controller.snapshot();

        assertRejectedUnchanged(controller.submit(ActionRequest.move(KNIGHT, at("e6"))),
                ActionError.WRONG_PHASE, before, controller);
        assertTrue(controller.legalMoves(KNIGHT).isEmpty());
    }

    @Test
    void controllerTakesAnOwnedSnapshotAndDoesNotRetainCallerMutableAlias() {
        BattleState initial = fixture();
        BattleController controller = new BattleController(initial);

        initial.movePiece(KNIGHT, at("e6"));
        initial.replacePiece(initial.pieceState(KNIGHT).orElseThrow().withHp(1));
        initial.consumeEquipment(EquipmentType.DIAGONAL_SKEWER);

        BattleSnapshot owned = controller.snapshot();
        assertEquals(at("d4"), owned.pieceById(KNIGHT).orElseThrow().position());
        assertEquals(3, owned.pieceById(KNIGHT).orElseThrow().hp());
        assertEquals(1, owned.inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
    }

    @Test
    void submitAfterBattleFinishedIsRejected() {
        BattleState state = fixture();
        state.finish(BattleResult.VICTORY);
        BattleController controller = new BattleController(state);
        BattleSnapshot before = controller.snapshot();

        assertRejectedUnchanged(controller.submit(ActionRequest.move(KNIGHT, at("e6"))),
                ActionError.BATTLE_FINISHED, before, controller);
        assertEquals(BattlePhase.FINISHED, controller.snapshot().phase());
        assertTrue(controller.legalMoves(KNIGHT).isEmpty());
    }

    @Test
    void invalidMoveKeepsHpChargeTurnAndSchedule() {
        BattleController controller = new BattleController(fixture());

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("d6")));

        BattleSnapshot after = controller.snapshot();
        assertFalse(result.accepted());
        assertEquals(1, after.globalTurn());
        assertEquals(BattlePhase.PLAYER_SELECT, after.phase());
        assertEquals(3, after.pieceById(KNIGHT).orElseThrow().hp());
        assertEquals(at("d4"), after.pieceById(KNIGHT).orElseThrow().position());
        assertEquals(0, after.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertEquals(1, after.inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
        assertEquals(5, after.pieceById("KG").orElseThrow().nextActivationTurn());
        assertEquals(3, after.pieceById("R1").orElseThrow().nextActivationTurn());
    }

    @Test
    void waitAdvancesTurnOnceWithoutMovingKnightOrChangingHp() {
        BattleController controller = new BattleController(fixture(), EnemyPolicy.NONE);
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertTrue(result.accepted());
        assertEquals(2, result.after().globalTurn());
        assertEquals(at("d4"), result.after().pieceById(KNIGHT).orElseThrow().position());
        assertEquals(3, result.after().pieceById(KNIGHT).orElseThrow().hp());
        assertEquals(before.inventory(), result.after().inventory());
    }

    @Test
    void validMoveAdvancesTurnExactlyOnce() {
        BattleController controller = new BattleController(fixture());

        ActionResult first = controller.submit(ActionRequest.move(KNIGHT, at("e6")));
        ActionResult second = controller.submit(ActionRequest.move(KNIGHT, at("e5")));

        assertEquals(1, first.before().globalTurn());
        assertEquals(2, first.after().globalTurn());
        assertEquals(3, second.after().globalTurn());
        assertEquals(3, controller.snapshot().globalTurn());
    }

    @Test
    void validMoveKeepsUnrelatedStateUnchanged() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        BattleSnapshot after = controller.snapshot();
        assertEquals(before.inventory(), after.inventory());
        assertEquals(before.rocks(), after.rocks());
        assertEquals(before.pieceById("KG"), after.pieceById("KG"));
        assertEquals(before.pieceById("R1"), after.pieceById("R1"));
        assertEquals(3, after.pieceById(KNIGHT).orElseThrow().hp());
        assertEquals(BattleResult.ONGOING, after.result());
    }

    @Test
    void earlierSnapshotIsNotAffectedByLaterMove() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertEquals(at("d4"), before.pieceById(KNIGHT).orElseThrow().position());
        assertEquals(before, result.before());
        assertEquals(1, before.globalTurn());
    }

    @Test
    void enemyPhaseRunsOnceAfterTurnAdvanceAndSeesNewPosition() {
        List<String> trace = new ArrayList<>();
        EnemyPolicy stub = (view, enemyId) -> {
            trace.add(view.globalTurn() + ":" + view.positionOf(KNIGHT).orElseThrow());
            return EnemyDecision.skip("TEST");
        };
        BattleState state = fixture();
        state.replacePiece(state.pieceState("R1").orElseThrow().withNextActivationTurn(2));
        BattleController controller = new BattleController(state, stub);

        controller.submit(ActionRequest.move(KNIGHT, at("d6")));
        assertTrue(trace.isEmpty(), "invalid request must not start the enemy phase");

        controller.submit(ActionRequest.move(KNIGHT, at("e6")));
        assertEquals(List.of("2:e6"), trace);
    }

    @Test
    void enemyPhaseEventsFollowPlayerEvents() {
        List<String> trace = new ArrayList<>();
        EnemyPolicy stub = (view, enemyId) -> {
            trace.add(enemyId + "@" + view.globalTurn());
            return EnemyDecision.skip("READ_ONLY_TEST");
        };
        BattleState state = fixture();
        state.replacePiece(state.pieceState("R1").orElseThrow().withNextActivationTurn(2));
        BattleController controller = new BattleController(state, stub);

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.MOVE, KNIGHT),
                new BattleEvent.PieceMoved(KNIGHT, at("d4"), at("e6")),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("R1", at("a8"), 2)), result.events());
        assertEquals(List.of("R1@2"), trace);
        assertEquals(at("a8"), result.after().pieceById("R1").orElseThrow().position());
    }
}
