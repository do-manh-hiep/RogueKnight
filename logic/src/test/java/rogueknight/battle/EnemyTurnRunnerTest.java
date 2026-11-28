package rogueknight.battle;

import rogueknight.action.*;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

/*
 * EnemyTurnRunnerTest  |  T-03.01  |  TD 5.2-5.3, 5.11; R-T03..R-T07, R-R01..R-R03, I07..I09, DEC-12
 *
 * Kiểm tra ĐIỀU PHỐI lượt địch bằng Enemy stub có kiểm soát, không phải AI thật:
 * stub đánh Knight nếu EnemyRules cho phép, còn lại skip (hoặc trả nước đi cố định khi test sequencing).
 * AI thật (DEC-02) được kiểm tra riêng ở rogueknight.ai.
 * Fixture mặc định: Knight K ở d4; King KG ở h8 (đến hạn turn 5) để trận còn ONGOING.
 */
class EnemyTurnRunnerTest {

    private static final String KNIGHT = "K";

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState battle(int knightHp) {
        BattleState state = new BattleState(Set.of(at("b6")),
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
        state.addPiece(new PieceState(KNIGHT, PieceType.KNIGHT, knightHp, 0, 0, 0), at("d4"));
        state.addPiece(new PieceState("KG", PieceType.KING, 5, 5, 5, 9), at("h8"));
        return state;
    }

    private static PieceState rook(String id, int nextTurn, int spawnOrder) {
        return new PieceState(id, PieceType.ROOK, 2, 3, nextTurn, spawnOrder);
    }

    private static PieceState bishop(String id, int nextTurn, int spawnOrder) {
        return new PieceState(id, PieceType.BISHOP, 2, 3, nextTurn, spawnOrder);
    }

    private static PieceState pawn(String id, int nextTurn, int spawnOrder) {
        return new PieceState(id, PieceType.PAWN, 1, 2, nextTurn, spawnOrder);
    }

    /** Stub: attack the Knight when the shared rules allow it, otherwise skip. Records what each Enemy saw. */
    private static EnemyPolicy attackOrSkip(List<String> trace) {
        return (view, enemyId) -> {
            int knightHp = view.pieceState(KNIGHT).map(PieceState::hp).orElse(0);
            trace.add(enemyId + "@" + view.globalTurn() + ":hp=" + knightHp);
            if (EnemyRules.canAttackKnight(view, enemyId)) {
                return EnemyDecision.act(ActionRequest.attack(enemyId, EnemyRules.knightPosition(view).orElseThrow()));
            }
            return EnemyDecision.skip("STUB");
        };
    }

    /** Stub: always the same first request, to test sequencing independently of DEC-02 ranking. */
    private static EnemyPolicy fixed(ActionRequest request, List<String> trace) {
        return (view, enemyId) -> {
            trace.add(enemyId);
            return EnemyDecision.act(request);
        };
    }

    private static long count(List<BattleEvent> events, Class<? extends BattleEvent> type) {
        return events.stream().filter(type::isInstance).count();
    }

    @Test
    void playerKillingKingWinsWithoutTurnAdvanceOrEnemyQueue() {
        List<String> trace = new ArrayList<>();
        BattleState state = new BattleState(Set.of(),
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
        state.addPiece(new PieceState(KNIGHT, PieceType.KNIGHT, 5, 0, 0, 0), at("d4"));
        state.addPiece(new PieceState("KG", PieceType.KING, 1, 5, 2, 1), at("e5"));
        state.addPiece(rook("R", 2, 2), at("d5"));
        BattleController controller = new BattleController(state, attackOrSkip(trace));

        ActionResult result = controller.submit(ActionRequest.sword(KNIGHT, at("e5")));

        assertTrue(result.accepted());
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.SWORD, KNIGHT),
                new BattleEvent.PieceDamaged("KG", 1, 0),
                new BattleEvent.PieceRemoved("KG", at("e5")),
                new BattleEvent.BattleEnded(BattleResult.VICTORY)), result.events());
        assertEquals(1, result.after().globalTurn());
        assertEquals(BattleResult.VICTORY, result.after().result());
        assertEquals(BattlePhase.FINISHED, result.after().phase());
        assertTrue(trace.isEmpty(), "no Enemy may act after the Player ends the battle");

        ActionResult late = controller.submit(ActionRequest.waitAction(KNIGHT));
        assertFalse(late.accepted());
        assertEquals(ActionError.BATTLE_FINISHED, late.error());
    }

    @Test
    void playerKillingLastNonKingEnemyWins() {
        BattleState state = battle(5);
        state.addPiece(new PieceState("R", PieceType.ROOK, 1, 3, 2, 1), at("e5"));
        BattleController controller = new BattleController(state, attackOrSkip(new ArrayList<>()));

        ActionResult result = controller.submit(ActionRequest.sword(KNIGHT, at("e5")));

        assertEquals(BattleResult.VICTORY, result.after().result());
        assertEquals(1, result.after().globalTurn());
        assertEquals(0, count(result.events(), BattleEvent.TurnAdvanced.class));
    }

    @Test
    void dueEnemiesActInFixedOrderAndEachSeesTheLatestState() {
        List<String> trace = new ArrayList<>();
        BattleState state = battle(5);
        // Inserted against queue order: Bishop before Rook, Pawn last.
        state.addPiece(bishop("B", 2, 2), at("e5"));
        state.addPiece(rook("R", 2, 3), at("d5"));
        state.addPiece(pawn("P", 2, 4), at("a7"));
        BattleController controller = new BattleController(state, attackOrSkip(trace));

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        // Pawn a7 faces SOUTH (a6, b6): the Knight is outside its cone, so the stub skips it.
        assertEquals(List.of("P@2:hp=5", "R@2:hp=5", "B@2:hp=4"), trace, "Bishop must see the Rook's damage");
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, KNIGHT),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("P", at("a7"), 2),
                new BattleEvent.ActorActivated("R", at("d5"), 2),
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "R"),
                new BattleEvent.PieceDamaged(KNIGHT, 1, 4),
                new BattleEvent.ActorActivated("B", at("e5"), 2),
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "B"),
                new BattleEvent.PieceDamaged(KNIGHT, 1, 3)), result.events());
        BattleSnapshot after = result.after();
        assertEquals(2, after.globalTurn());
        assertEquals(BattlePhase.PLAYER_SELECT, after.phase());
        assertEquals(3, after.pieceById(KNIGHT).orElseThrow().hp());
        assertEquals(4, after.pieceById("P").orElseThrow().nextActivationTurn());
        assertEquals(5, after.pieceById("R").orElseThrow().nextActivationTurn());
        assertEquals(5, after.pieceById("B").orElseThrow().nextActivationTurn());
        assertEquals(5, after.pieceById("KG").orElseThrow().nextActivationTurn());
    }

    @Test
    void firstEnemyKillingKnightStopsTheQueue() {
        List<String> trace = new ArrayList<>();
        BattleState state = battle(1);
        state.addPiece(rook("R", 2, 1), at("d5"));
        state.addPiece(bishop("B", 2, 2), at("e5"));
        BattleController controller = new BattleController(state, attackOrSkip(trace));

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertEquals(List.of("R@2:hp=1"), trace, "Bishop must not act after Defeat");
        List<BattleEvent> events = result.events();
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "R"),
                new BattleEvent.PieceDamaged(KNIGHT, 1, 0),
                new BattleEvent.PieceRemoved(KNIGHT, at("d4")),
                new BattleEvent.BattleEnded(BattleResult.DEFEAT)), events.subList(events.size() - 4, events.size()));
        assertEquals(1, count(events, BattleEvent.BattleEnded.class));
        assertEquals(BattleResult.DEFEAT, result.after().result());
        assertEquals(BattlePhase.FINISHED, result.after().phase());
        assertEquals(2, result.after().globalTurn());
        assertEquals(2, result.after().pieceById("B").orElseThrow().nextActivationTurn());
        assertEquals(ActionError.BATTLE_FINISHED, controller.submit(ActionRequest.waitAction(KNIGHT)).error());
    }

    @Test
    void enemyWithoutLegalActionSkipsOnceAndWaitsForItsNextCycle() {
        List<String> trace = new ArrayList<>();
        BattleState state = battle(5);
        state.addPiece(rook("R", 2, 1), at("a8"));
        BattleController controller = new BattleController(state, attackOrSkip(trace));
        BattleSnapshot before = controller.snapshot();

        ActionResult turn2 = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, KNIGHT),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("R", at("a8"), 2)), turn2.events());
        assertEquals(before.pieceById("R").orElseThrow().position(), turn2.after().pieceById("R").orElseThrow().position());
        assertEquals(5, turn2.after().pieceById("R").orElseThrow().nextActivationTurn());

        controller.submit(ActionRequest.waitAction(KNIGHT)); // turn 3
        controller.submit(ActionRequest.waitAction(KNIGHT)); // turn 4
        controller.submit(ActionRequest.waitAction(KNIGHT)); // turn 5
        assertEquals(List.of("R@2:hp=5", "R@5:hp=5"),
                trace.stream().filter(entry -> entry.startsWith("R@")).toList());
    }

    @Test
    void invalidPlayerActionBuildsNoQueueAndKeepsSchedule() {
        List<String> trace = new ArrayList<>();
        BattleState state = battle(5);
        state.addPiece(rook("R", 2, 1), at("d5"));
        BattleController controller = new BattleController(state, attackOrSkip(trace));
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("d6")));

        assertFalse(result.accepted());
        assertTrue(result.events().isEmpty());
        assertTrue(trace.isEmpty());
        assertEquals(before, controller.snapshot());
    }

    @Test
    void genericMoveThenAttackStaysInOneTurnAndAsksPolicyOnce() {
        List<String> trace = new ArrayList<>();
        BattleState state = battle(5);
        state.addPiece(rook("R", 2, 1), at("a5"));
        BattleController controller = new BattleController(state,
                fixed(ActionRequest.move("R", at("d5")), trace));

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertEquals(List.of("R"), trace, "post-Move Attack is decided by the runner, not by a second choose()");
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, KNIGHT),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("R", at("a5"), 2),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "R"),
                new BattleEvent.PieceMoved("R", at("a5"), at("d5")),
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "R"),
                new BattleEvent.PieceDamaged(KNIGHT, 1, 4)), result.events());
        assertEquals(1, count(result.events(), BattleEvent.TurnAdvanced.class));
        assertEquals(2, result.after().globalTurn());
    }

    @Test
    void moveThatEndsOutOfRangeIsNotFollowedByAttack() {
        BattleState state = battle(5);
        state.addPiece(rook("R", 2, 1), at("a5"));
        BattleController controller = new BattleController(state,
                fixed(ActionRequest.move("R", at("b5")), new ArrayList<>()));

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertEquals(new BattleEvent.PieceMoved("R", at("a5"), at("b5")), result.events().get(result.events().size() - 1));
        assertEquals(0, result.events().stream()
                .filter(e -> e.equals(new BattleEvent.ActionPerformed(ActionKind.ATTACK, "R"))).count());
        assertEquals(5, result.after().pieceById(KNIGHT).orElseThrow().hp());
    }

    @Test
    void invalidAiRequestFailsLoudlyInsteadOfSkipping() {
        BattleState state = battle(5);
        state.addPiece(rook("R", 2, 1), at("b8"));
        // b6 is a Rock: the Rook can never land there (DEC-12: an AI bug, not a silent skip).
        BattleController controller = new BattleController(state,
                fixed(ActionRequest.move("R", at("b6")), new ArrayList<>()));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controller.submit(ActionRequest.waitAction(KNIGHT)));
        assertTrue(error.getMessage().contains("OCCUPIED_BY_ROCK"));
    }



    @Test
    void nullAiDecisionFailsLoudlyInsteadOfBeingTreatedAsSkip() {
        BattleState state = battle(5);
        state.addPiece(rook("R", 2, 1), at("d5"));
        BattleController controller = new BattleController(state,
                (view, enemyId) -> null);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controller.submit(ActionRequest.waitAction(KNIGHT)));
        assertTrue(error.getMessage().contains("DEC-12"));
        assertTrue(error.getMessage().contains("null or unsupported decision"));
    }

    @Test
    void promotionKeepsItsScheduleAndThePromotedQueenDoesNotAttackYet() {
        // Pawn c2 steps to c1 and promotes inside the Move (ActionResolver). The new Queen on c1 is diagonal-adjacent
        // to the Knight on d2, but the activation started as a Pawn, so no post-Move Attack (R-P05).
        BattleState state = new BattleState(Set.of(),
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
        state.addPiece(new PieceState(KNIGHT, PieceType.KNIGHT, 5, 0, 0, 0), at("d2"));
        state.addPiece(new PieceState("KG", PieceType.KING, 5, 5, 5, 9), at("h8"));
        state.addPiece(rook("R", 9, 1), at("a8"));
        state.addPiece(pawn("P", 2, 2), at("c2"));
        BattleController controller = new BattleController(state,
                fixed(ActionRequest.move("P", at("c1")), new ArrayList<>()));

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.WAIT, KNIGHT),
                new BattleEvent.TurnAdvanced(1, 2),
                new BattleEvent.ActorActivated("P", at("c2"), 2),
                new BattleEvent.ActionPerformed(ActionKind.MOVE, "P"),
                new BattleEvent.PieceMoved("P", at("c2"), at("c1")),
                new BattleEvent.PiecePromoted("P", PieceType.PAWN, PieceType.QUEEN, at("c1"))), result.events());
        BattleSnapshot.PieceView queen = result.after().pieceById("P").orElseThrow();
        assertEquals(PieceType.QUEEN, queen.type());
        assertEquals(7, queen.nextActivationTurn(), "T+5 from turn 2 must not get +cycle on top");
        assertEquals(5, result.after().pieceById(KNIGHT).orElseThrow().hp(), "no Queen Attack in the promotion activation");
    }

    @Test
    void pawnAttackKillingKnightEndsBattleBeforeLaterEnemies() {
        List<String> trace = new ArrayList<>();
        BattleState state = battle(1);
        state.addPiece(pawn("P", 2, 1), at("d5")); // faces SOUTH: d4, c4, e4
        state.addPiece(rook("R", 2, 2), at("a8"));
        BattleController controller = new BattleController(state, attackOrSkip(trace));

        ActionResult result = controller.submit(ActionRequest.waitAction(KNIGHT));

        assertEquals(List.of("P@2:hp=1"), trace, "Rook must not act after the Pawn ended the battle");
        List<BattleEvent> events = result.events();
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.ATTACK, "P"),
                new BattleEvent.PieceDamaged(KNIGHT, 1, 0),
                new BattleEvent.PieceRemoved(KNIGHT, at("d4")),
                new BattleEvent.BattleEnded(BattleResult.DEFEAT)), events.subList(events.size() - 4, events.size()));
        assertEquals(BattleResult.DEFEAT, result.after().result());
    }

}
