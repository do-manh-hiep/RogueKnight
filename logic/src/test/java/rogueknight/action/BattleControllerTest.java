package rogueknight.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
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
        EnemyPhase stub = state -> {
            trace.add(state.globalTurn() + ":" + state.phase() + ":" + state.positionOf(KNIGHT).orElseThrow());
            return List.of();
        };
        BattleController controller = new BattleController(fixture(), stub);

        controller.submit(ActionRequest.move(KNIGHT, at("d6")));
        assertTrue(trace.isEmpty(), "invalid request must not start the enemy phase");

        controller.submit(ActionRequest.move(KNIGHT, at("e6")));
        assertEquals(List.of("2:ENEMY_RESOLVE:e6"), trace);
    }

    @Test
    void enemyPhaseEventsFollowPlayerEvents() {
        BattleEvent enemyEvent = new BattleEvent.PieceMoved("R1", at("a8"), at("a7"));
        EnemyPhase stub = state -> {
            state.movePiece("R1", at("a7"));
            return List.of(enemyEvent);
        };
        BattleController controller = new BattleController(fixture(), stub);

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertEquals(List.of(new BattleEvent.PieceMoved(KNIGHT, at("d4"), at("e6")), enemyEvent), result.events());
        assertEquals(at("a7"), result.after().pieceById("R1").orElseThrow().position());
    }
}
