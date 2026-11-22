package rogueknight.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.data.BattleFactory;

/*
 * EnemyTurnIntegrationTest  |  T-03.01 + T-03.02 (G2)  |  IT-02, IT-04, TC-E02, TD 5.12A/B
 * BattleController thật + EnemyTurnRunner + GenericEnemyPolicy thật.
 * Pawn vẫn dùng PawnActivation.SKIP (T-03.03 chưa merge): Pawn chỉ phát ActorActivated, lịch vẫn tiến.
 */
class EnemyTurnIntegrationTest {

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleController realController(BattleState state) {
        return new BattleController(state, new EnemyTurnRunner(new GenericEnemyPolicy()));
    }

    @Test
    void rookMovesThenAttacksAndDefeatStopsTheQueue() {
        // TD 5.12B / IT-04: Turn 2 -> 3; queue Rook -> Bishop. Rook d4 -> d2, attacks Knight c2 (HP 1 -> 0).
        BattleState state = new BattleState(Set.of(),
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 1, 0, 0, 0), at("c2"));
        state.addPiece(new PieceState("rook-1", PieceType.ROOK, 2, 3, 3, 1), at("d4"));
        state.addPiece(new PieceState("bishop-1", PieceType.BISHOP, 2, 3, 3, 2), at("f6"));
        state.addPiece(new PieceState("king-1", PieceType.KING, 5, 5, 5, 3), at("h8"));
        state.advanceTurn(); // fixture starts at Turn 2
        BattleController controller = realController(state);

        ActionResult result = controller.submit(ActionRequest.waitAction("knight"));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, "knight"),
                new BattleEvent.TurnAdvanced(2, 3),
                new BattleEvent.ActorActivated("rook-1", at("d4"), 3),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "rook-1"),
                new BattleEvent.PieceMoved("rook-1", at("d4"), at("d2")),
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "rook-1"),
                new BattleEvent.PieceDamaged("knight", 1, 0),
                new BattleEvent.PieceRemoved("knight", at("c2")),
                new BattleEvent.BattleEnded(BattleResult.DEFEAT)), result.events());
        BattleSnapshot after = result.after();
        assertEquals(3, after.globalTurn(), "both Rook Actions happen inside Turn 3");
        assertEquals(BattleResult.DEFEAT, after.result());
        assertEquals(BattlePhase.FINISHED, after.phase());
        assertEquals(at("f6"), after.pieceById("bishop-1").orElseThrow().position());
        assertEquals(3, after.pieceById("bishop-1").orElseThrow().nextActivationTurn(), "Bishop never activated");
    }

    @Test
    void enemyInRangeAttacksWithoutMovingFirst() {
        BattleState state = new BattleState(Set.of(),
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), at("d3"));
        state.addPiece(new PieceState("rook-1", PieceType.ROOK, 2, 3, 2, 1), at("d4"));
        state.addPiece(new PieceState("king-1", PieceType.KING, 5, 5, 5, 2), at("h8"));
        BattleController controller = realController(state);

        ActionResult result = controller.submit(ActionRequest.waitAction("knight"));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, "knight"),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("rook-1", at("d4"), 2),
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "rook-1"),
                new BattleEvent.PieceDamaged("knight", 1, 4)), result.events());
        assertEquals(at("d4"), result.after().pieceById("rook-1").orElseThrow().position());
        assertEquals(5, result.after().pieceById("rook-1").orElseThrow().nextActivationTurn());
    }

    @Test
    void map01CannonAttackOpeningQueuesOnlyPawnOne() throws IOException {
        // TD 5.12A / IT-02: Pawn2 dies, Turn 1 -> 2, the queue holds only Pawn1.
        BattleState state = new BattleFactory().createMap01(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleController controller = realController(state);

        ActionResult result = controller.submit(ActionRequest.cannonAttack("knight", at("d7")));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.CANNON_ATTACK, "knight"),
                new BattleEvent.PieceDamaged("pawn-2", 1, 0),
                new BattleEvent.PieceRemoved("pawn-2", at("d7")),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("pawn-1", at("c7"), 2)), result.events());
        assertEquals(0, result.after().inventory().chargesOf(EquipmentType.CANNON_ATTACK));
        assertEquals(1, result.after().inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertEquals(BattleResult.ONGOING, result.after().result());
    }

    @Test
    void map01RookAndBishopActDeterministicallyOnTurnThree() throws IOException {
        // Turn 3 on Map01 (Knight d1 waits). Hand-ranked with DEC-02:
        // Rook h8: no Move->Attack, all squares safe; threat only from h1 (-> e1) and h2 (-> d2), both distance 4,
        //          h1 wins on row. Bishop c8: only b7 and a6 (d7 blocks); neither threatens, a6 is closer (5 < 6).
        List<BattleEvent> firstRun = playTwoWaitsOnMap01();
        List<BattleEvent> secondRun = playTwoWaitsOnMap01();

        assertEquals(firstRun, secondRun, "same state must give the same enemy turn");
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, "knight"),
                new BattleEvent.TurnAdvanced(2, 3),
                new BattleEvent.ActorActivated("rook-1", at("h8"), 3),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "rook-1"),
                new BattleEvent.PieceMoved("rook-1", at("h8"), at("h1")),
                new BattleEvent.ActorActivated("bishop-1", at("c8"), 3),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "bishop-1"),
                new BattleEvent.PieceMoved("bishop-1", at("c8"), at("a6"))), firstRun);
    }

    private static List<BattleEvent> playTwoWaitsOnMap01() throws IOException {
        BattleState state = new BattleFactory().createMap01(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
        BattleController controller = realController(state);

        ActionResult turn2 = controller.submit(ActionRequest.waitAction("knight"));
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, "knight"),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("pawn-1", at("c7"), 2),
                new BattleEvent.ActorActivated("pawn-2", at("d7"), 2)), turn2.events());

        ActionResult turn3 = controller.submit(ActionRequest.waitAction("knight"));
        assertTrue(turn3.accepted());
        assertEquals(6, turn3.after().pieceById("rook-1").orElseThrow().nextActivationTurn());
        assertEquals(6, turn3.after().pieceById("bishop-1").orElseThrow().nextActivationTurn());
        assertEquals(5, turn3.after().pieceById("knight").orElseThrow().hp());
        return turn3.events();
    }
}
