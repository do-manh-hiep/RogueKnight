package rogueknight.action;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentActionIntegrationTest {

    @Test
    void cannonMovement_map01Fixture_movesCapturesAndConsumesCharge() {
        // Knight d1, Rock d3 (screen), Pawn d7 (HP 1), King d8
        Inventory inv = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        BattleState state = new BattleState(Set.of(Position.fromNotation("d3")), inv);
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 3, 1, 1, 0), Position.fromNotation("d1"));
        state.addPiece(new PieceState("pawn_1", PieceType.PAWN, 1, 2, 2, 1), Position.fromNotation("d7"));
        state.addPiece(new PieceState("king", PieceType.KING, 3, 2, 2, 2), Position.fromNotation("d8"));

        BattleController controller = new BattleController(state);

        // Kiểm tra legalTargets trả về đúng d4, d5, d6 và d7 (không vượt qua d7 tới d8)
        Set<Position> legal = controller.legalTargets("knight", ActionKind.CANNON_MOVEMENT);
        assertEquals(Set.of(
            Position.fromNotation("d4"),
            Position.fromNotation("d5"),
            Position.fromNotation("d6"),
            Position.fromNotation("d7")
        ), legal);

        // Thực thi Cannon Capture vào d7
        ActionResult res = controller.submit(ActionRequest.cannonMovement("knight", Position.fromNotation("d7")));
        assertTrue(res.accepted());
        assertEquals(Position.fromNotation("d7"), res.after().pieceById("knight").orElseThrow().position());
        assertTrue(res.after().pieceById("pawn_1").isEmpty());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertTrue(res.after().inventory().contains(EquipmentType.CANNON_MOVEMENT));

        // Hết charge -> gọi lại bị từ chối và không tăng turn
        int turnAfterFirstMove = res.after().globalTurn();
        ActionResult exhaustedRes = controller.submit(ActionRequest.cannonMovement("knight", Position.fromNotation("d6")));
        assertFalse(exhaustedRes.accepted());
        assertEquals(ActionError.NO_EQUIPMENT_CHARGES, exhaustedRes.error());
        assertEquals(turnAfterFirstMove, exhaustedRes.after().globalTurn());
    }

    @Test
    void cannonAttack_damagesTargetBehindScreenWithoutMovingKnight() {
        Inventory inv = Inventory.initial(EquipmentType.CANNON_ATTACK, EquipmentType.DIAGONAL_SKEWER);
        BattleState state = new BattleState(Set.of(Position.fromNotation("d3")), inv);
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 3, 1, 1, 0), Position.fromNotation("d1"));
        state.addPiece(new PieceState("rook_1", PieceType.ROOK, 3, 2, 2, 1), Position.fromNotation("d6"));

        BattleController controller = new BattleController(state);

        // Cannon Movement không sở hữu trong túi -> bị chặn
        ActionResult unowned = controller.submit(ActionRequest.cannonMovement("knight", Position.fromNotation("d4")));
        assertFalse(unowned.accepted());
        assertEquals(ActionError.EQUIPMENT_NOT_OWNED, unowned.error());

        // Cannon Attack bắn d6: Knight đứng yên ở d1, Rook mất 1 HP còn 2 HP, tốn 1 charge
        ActionResult res = controller.submit(ActionRequest.cannonAttack("knight", Position.fromNotation("d6")));
        assertTrue(res.accepted());
        assertEquals(Position.fromNotation("d1"), res.after().pieceById("knight").orElseThrow().position());
        assertEquals(2, res.after().pieceById("rook_1").orElseThrow().hp());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.CANNON_ATTACK));
        assertEquals(List.of(
            new BattleEvent.ActionPerformed(ActionKind.CANNON_ATTACK, "knight"),
            new BattleEvent.PieceDamaged("rook_1", 1, 2)
        ), res.events());
    }

    @Test
    void diagonalSkewer_hitsMultipleEnemiesAndStopsBeforeRock() {
        Inventory inv = Inventory.initial(EquipmentType.DIAGONAL_SKEWER, EquipmentType.CANNON_MOVEMENT);
        BattleState state = new BattleState(Set.of(Position.fromNotation("b3")), inv);
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 3, 1, 1, 0), Position.fromNotation("c2"));
        state.addPiece(new PieceState("pawn_1", PieceType.PAWN, 1, 2, 2, 1), Position.fromNotation("d3"));
        state.addPiece(new PieceState("bishop_1", PieceType.BISHOP, 2, 2, 2, 2), Position.fromNotation("e4"));

        BattleController controller = new BattleController(state);

        // Hướng chéo lên-trái bị Rock ở b3 chặn
        ActionResult blocked = controller.submit(ActionRequest.diagonalSkewer("knight", Position.fromNotation("b3")));
        assertFalse(blocked.accepted());
        assertEquals(ActionError.OCCUPIED_BY_ROCK, blocked.error());

        // Lướt chéo lên-phải tới e4: hạ pawn_1 (d3, 1 HP), gây 1 dmg lên bishop_1 (e4, 2->1 HP), Knight đáp tại d3
        ActionResult res = controller.submit(ActionRequest.diagonalSkewer("knight", Position.fromNotation("e4")));
        assertTrue(res.accepted());
        assertTrue(res.after().pieceById("pawn_1").isEmpty());
        assertEquals(1, res.after().pieceById("bishop_1").orElseThrow().hp());
        assertEquals(Position.fromNotation("d3"), res.after().pieceById("knight").orElseThrow().position());
        assertEquals(0, res.after().inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
    }
}