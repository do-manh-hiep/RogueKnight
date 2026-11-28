package rogueknight.battle;

import rogueknight.action.*;


import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-04.01 / T-04.02 integration tests: Cannon Movement, Cannon Attack, Diagonal Skewer.
 * Rules: GDD s6, TD 4.9, R-Q05..R-Q11, FX-04..FX-07. DEC-03: Skewer must hit >= 1 Enemy.
 * Skewer never moves the Knight. Fixtures are built directly; BattleController performs the terminal check after each resolved Action (T-03.01).
 */
class EquipmentActionIntegrationTest {

    private static Position p(String notation) {
        return Position.fromNotation(notation);
    }

    private static PieceState knight() {
        return new PieceState("knight", PieceType.KNIGHT, 3, 1, 1, 0);
    }

    private static PieceState pawn(int hp) {
        return new PieceState("pawn_1", PieceType.PAWN, hp, 2, 2, 1);
    }

    @Test
    void cannonMovement_map01Fixture_movesCapturesAndConsumesCharge() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(p("d3")), inv);
        state.addPiece(knight(), p("d1"));
        state.addPiece(pawn(1), p("d7"));
        state.addPiece(new PieceState("king", PieceType.KING, 3, 2, 2, 2), p("d8"));
        // Rook h8 as in Map01: capturing the Pawn must not leave the battle without non-King Enemies (R-R02).
        state.addPiece(new PieceState("rook_1", PieceType.ROOK, 2, 3, 3, 3), p("h8"));
        BattleController controller = new BattleController(state);

        assertEquals(Set.of(p("d4"), p("d5"), p("d6"), p("d7")),
            controller.legalTargets("knight", ActionKind.CANNON_MOVEMENT));

        ActionResult res = controller.submit(ActionRequest.cannonMovement("knight", p("d7")));
        assertTrue(res.accepted());
        assertEquals(p("d7"), res.after().pieceById("knight").orElseThrow().position());
        assertTrue(res.after().pieceById("pawn_1").isEmpty());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertTrue(res.after().inventory().contains(EquipmentType.CANNON_MOVEMENT));

        int turnAfter = res.after().globalTurn();
        ActionResult exhausted = controller.submit(ActionRequest.cannonMovement("knight", p("d6")));
        assertFalse(exhausted.accepted());
        assertEquals(ActionError.NO_EQUIPMENT_CHARGES, exhausted.error());
        assertEquals(turnAfter, exhausted.after().globalTurn());
    }

    @Test
    void cannonMovement_toKingBehindTerminalBlocker_isInvalidAndKeepsState() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(p("d3")), inv);
        state.addPiece(knight(), p("d1"));
        state.addPiece(pawn(1), p("d7"));
        state.addPiece(new PieceState("king", PieceType.KING, 3, 2, 2, 2), p("d8"));
        BattleController controller = new BattleController(state);

        var before = controller.snapshot();
        ActionResult res = controller.submit(ActionRequest.cannonMovement("knight", p("d8")));
        assertFalse(res.accepted());
        assertEquals(before, controller.snapshot());
    }

    @Test
    void cannonMovement_enemyHp3_onlyEmptyLandingsAreLegal() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(p("d3")), inv);
        state.addPiece(knight(), p("d1"));
        state.addPiece(pawn(3), p("d7"));
        BattleController controller = new BattleController(state);

        assertEquals(Set.of(p("d4"), p("d5"), p("d6")),
            controller.legalTargets("knight", ActionKind.CANNON_MOVEMENT));
        assertEquals(Set.of(p("d7")), controller.legalTargets("knight", ActionKind.CANNON_ATTACK));
    }

    @Test
    void cannonMovement_adjacentScreenWithoutGap_captureStillLegal() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(p("a2")), inv);
        state.addPiece(knight(), p("a1"));
        state.addPiece(pawn(2), p("a3"));
        BattleController controller = new BattleController(state);

        assertEquals(Set.of(p("a3")), controller.legalTargets("knight", ActionKind.CANNON_MOVEMENT));
        ActionResult screen = controller.submit(ActionRequest.cannonMovement("knight", p("a2")));
        assertFalse(screen.accepted());
    }

    @Test
    void cannonAttack_damagesTargetBehindScreenWithoutMovingKnight() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_ATTACK, EquipmentType.DIAGONAL_SKEWER);
        BattleState state = new BattleState(Set.of(p("d3")), inv);
        state.addPiece(knight(), p("d1"));
        state.addPiece(new PieceState("rook_1", PieceType.ROOK, 3, 2, 2, 1), p("d6"));
        // King off the ray keeps the battle ongoing (no King would be an immediate VICTORY).
        state.addPiece(new PieceState("king", PieceType.KING, 5, 5, 5, 2), p("h8"));
        BattleController controller = new BattleController(state);

        ActionResult unowned = controller.submit(ActionRequest.cannonMovement("knight", p("d4")));
        assertFalse(unowned.accepted());
        assertEquals(ActionError.EQUIPMENT_NOT_OWNED, unowned.error());

        ActionResult res = controller.submit(ActionRequest.cannonAttack("knight", p("d6")));
        assertTrue(res.accepted());
        assertEquals(p("d1"), res.after().pieceById("knight").orElseThrow().position());
        assertEquals(2, res.after().pieceById("rook_1").orElseThrow().hp());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.CANNON_ATTACK));
        assertEquals(List.of(
            new BattleEvent.ActionPerformed(ActionKind.CANNON_ATTACK, "knight"),
            new BattleEvent.PieceDamaged("rook_1", 1, 2),
            new BattleEvent.TurnAdvanced(1, 2),
            new BattleEvent.ActorActivated("rook_1", p("d6"), 2)
        ), res.events());
    }

    @Test
    void diagonalSkewer_hitsTwoEnemies_knightStaysAndOneChargeUsed() {
        Inventory inv = Inventory.initial(EquipmentType.DIAGONAL_SKEWER, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(p("b3")), inv);
        state.addPiece(knight(), p("c2"));
        state.addPiece(pawn(1), p("d3"));
        state.addPiece(new PieceState("bishop_1", PieceType.BISHOP, 2, 2, 2, 2), p("e4"));
        BattleController controller = new BattleController(state);

        ActionResult blocked = controller.submit(ActionRequest.diagonalSkewer("knight", p("b3")));
        assertFalse(blocked.accepted());
        assertEquals(ActionError.OCCUPIED_BY_ROCK, blocked.error());

        ActionResult res = controller.submit(ActionRequest.diagonalSkewer("knight", p("e4")));
        assertTrue(res.accepted());
        assertTrue(res.after().pieceById("pawn_1").isEmpty());
        assertEquals(1, res.after().pieceById("bishop_1").orElseThrow().hp());
        assertEquals(p("c2"), res.after().pieceById("knight").orElseThrow().position());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
        assertEquals(1, res.after().inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
    }

    @Test
    void diagonalSkewer_noEnemyInRange_isInvalidAndKeepsState() {
        Inventory inv = Inventory.initial(EquipmentType.DIAGONAL_SKEWER, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(), inv);
        state.addPiece(knight(), p("b2"));
        state.addPiece(pawn(1), p("e5"));
        BattleController controller = new BattleController(state);

        var before = controller.snapshot();
        ActionResult res = controller.submit(ActionRequest.diagonalSkewer("knight", p("c3")));
        assertFalse(res.accepted());
        assertEquals(ActionError.NO_TARGET_PIECE, res.error());
        assertEquals(before, controller.snapshot());
    }

    @Test
    void diagonalSkewer_rockOnFirstCell_blocksEnemyBehind() {
        Inventory inv = Inventory.initial(EquipmentType.DIAGONAL_SKEWER, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(p("c3")), inv);
        state.addPiece(knight(), p("b2"));
        state.addPiece(pawn(1), p("d4"));
        BattleController controller = new BattleController(state);

        ActionResult res = controller.submit(ActionRequest.diagonalSkewer("knight", p("d4")));
        assertFalse(res.accepted());
        assertEquals(1, controller.snapshot().pieceById("pawn_1").orElseThrow().hp());
        assertEquals(1, controller.snapshot().inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
    }

    @Test
    void diagonalSkewer_rockOnSecondCell_onlyFirstEnemyDamaged() {
        Inventory inv = Inventory.initial(EquipmentType.DIAGONAL_SKEWER, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(p("d4")), inv);
        state.addPiece(knight(), p("b2"));
        state.addPiece(new PieceState("rook_1", PieceType.ROOK, 2, 2, 2, 1), p("c3"));
        BattleController controller = new BattleController(state);

        ActionResult res = controller.submit(ActionRequest.diagonalSkewer("knight", p("c3")));
        assertTrue(res.accepted());
        assertEquals(1, res.after().pieceById("rook_1").orElseThrow().hp());
        assertEquals(p("b2"), res.after().pieceById("knight").orElseThrow().position());
    }

    @Test
    void cannonAttack_map01Opening_killsPawnAndRejectsKingBehind() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_ATTACK, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(p("d3")), inv);
        state.addPiece(knight(), p("d1"));
        state.addPiece(pawn(1), p("d7"));
        state.addPiece(new PieceState("king", PieceType.KING, 5, 5, 5, 2), p("d8"));
        BattleController controller = new BattleController(state);

        var before = controller.snapshot();
        ActionResult behind = controller.submit(ActionRequest.cannonAttack("knight", p("d8")));
        assertFalse(behind.accepted());
        assertEquals(before, controller.snapshot());

        ActionResult res = controller.submit(ActionRequest.cannonAttack("knight", p("d7")));
        assertTrue(res.accepted());
        assertTrue(res.after().pieceById("pawn_1").isEmpty());
        assertEquals(p("d1"), res.after().pieceById("knight").orElseThrow().position());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.CANNON_ATTACK));
    }

    @Test
    void cannonMovement_noSecondBlocker_allEmptyTilesToEdgeAreLegal() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(p("a2")), inv);
        state.addPiece(knight(), p("a1"));
        state.addPiece(new PieceState("king", PieceType.KING, 5, 5, 5, 2), p("h8"));
        BattleController controller = new BattleController(state);

        assertEquals(Set.of(p("a3"), p("a4"), p("a5"), p("a6"), p("a7"), p("a8")),
            controller.legalTargets("knight", ActionKind.CANNON_MOVEMENT));
        assertTrue(controller.legalTargets("knight", ActionKind.CANNON_ATTACK).isEmpty());
    }

    @Test
    void exhaustedEquipment_staysInInventoryWithNoLegalTargets() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(p("d3")), inv);
        state.addPiece(knight(), p("d1"));
        state.addPiece(pawn(1), p("d7"));
        BattleController controller = new BattleController(state);

        ActionResult res = controller.submit(ActionRequest.cannonMovement("knight", p("d4")));
        assertTrue(res.accepted());
        assertTrue(controller.legalTargets("knight", ActionKind.CANNON_MOVEMENT).isEmpty());
        assertTrue(controller.snapshot().inventory().contains(EquipmentType.CANNON_MOVEMENT));
        assertEquals(0, controller.snapshot().inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
    }

    @Test
    void diagonalSkewer_atBoardEdge_hitsOnlyTheInBoundsCell() {
        Inventory inv = Inventory.initial(EquipmentType.DIAGONAL_SKEWER, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(), inv);
        state.addPiece(knight(), p("g7"));
        state.addPiece(new PieceState("rook_1", PieceType.ROOK, 2, 2, 2, 1), p("h8"));
        BattleController controller = new BattleController(state);

        assertTrue(controller.legalTargets("knight", ActionKind.DIAGONAL_SKEWER).contains(p("h8")));
        ActionResult res = controller.submit(ActionRequest.diagonalSkewer("knight", p("h8")));
        assertTrue(res.accepted());
        assertEquals(1, res.after().pieceById("rook_1").orElseThrow().hp());
        assertEquals(p("g7"), res.after().pieceById("knight").orElseThrow().position());
    }

    @Test
    void cannonAttack_enemyScreen_hitsTargetBehindButNotTheScreen() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_ATTACK, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(), inv);
        state.addPiece(knight(), p("a1"));
        state.addPiece(pawn(1), p("a2"));
        state.addPiece(new PieceState("rook_1", PieceType.ROOK, 2, 2, 2, 2), p("a3"));
        BattleController controller = new BattleController(state);

        assertEquals(Set.of(p("a3")), controller.legalTargets("knight", ActionKind.CANNON_ATTACK));
        ActionResult res = controller.submit(ActionRequest.cannonAttack("knight", p("a3")));
        assertTrue(res.accepted());
        assertEquals(1, res.after().pieceById("rook_1").orElseThrow().hp());
        assertEquals(1, res.after().pieceById("pawn_1").orElseThrow().hp());
    }
}
