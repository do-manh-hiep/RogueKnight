package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.action.ActionRequest;
import rogueknight.action.BattleController;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.PieceType;
import rogueknight.battle.Direction;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleFactoryTest {

    @Test
    void createsMap01WithInitialState() throws IOException {
        BattleFactory factory = new BattleFactory();

        BattleState battle = factory.createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleSnapshot snapshot = battle.snapshot();

        // Trạng thái ban đầu của trận.
        assertEquals(1, snapshot.globalTurn());
        assertEquals(BattlePhase.PLAYER_SELECT, snapshot.phase());
        assertEquals(BattleResult.ONGOING, snapshot.result());

        assertEquals(6, snapshot.rocks().size());
        assertEquals(6, snapshot.pieces().size());

        // Knight.
        BattleSnapshot.PieceView knight =
            snapshot.pieceById("knight").orElseThrow();

        assertEquals(PieceType.KNIGHT, knight.type());
        assertEquals(new Position(3, 0), knight.position());
        assertEquals(5, knight.hp());
        assertEquals(0, knight.cycle());
        assertEquals(0, knight.nextActivationTurn());

        // Pawn thứ nhất.
        BattleSnapshot.PieceView pawn1 =
            snapshot.pieceById("pawn-1").orElseThrow();

        assertEquals(PieceType.PAWN, pawn1.type());
        assertEquals(new Position(2, 6), pawn1.position());
        assertEquals(1, pawn1.hp());
        assertEquals(2, pawn1.cycle());
        assertEquals(2, pawn1.nextActivationTurn());

        // Pawn thứ hai.
        BattleSnapshot.PieceView pawn2 =
            snapshot.pieceById("pawn-2").orElseThrow();

        assertEquals(PieceType.PAWN, pawn2.type());
        assertEquals(new Position(3, 6), pawn2.position());
        assertEquals(1, pawn2.hp());
        assertEquals(2, pawn2.cycle());
        assertEquals(2, pawn2.nextActivationTurn());

        // Rook.
        BattleSnapshot.PieceView rook =
            snapshot.pieceById("rook-1").orElseThrow();

        assertEquals(PieceType.ROOK, rook.type());
        assertEquals(new Position(7, 7), rook.position());
        assertEquals(2, rook.hp());
        assertEquals(3, rook.cycle());
        assertEquals(3, rook.nextActivationTurn());

        // Bishop.
        BattleSnapshot.PieceView bishop =
            snapshot.pieceById("bishop-1").orElseThrow();

        assertEquals(PieceType.BISHOP, bishop.type());
        assertEquals(new Position(2, 7), bishop.position());
        assertEquals(2, bishop.hp());
        assertEquals(3, bishop.cycle());
        assertEquals(3, bishop.nextActivationTurn());

        // King.
        BattleSnapshot.PieceView king =
            snapshot.pieceById("king-1").orElseThrow();

        assertEquals(PieceType.KING, king.type());
        assertEquals(new Position(3, 7), king.position());
        assertEquals(5, king.hp());
        assertEquals(5, king.cycle());
        assertEquals(5, king.nextActivationTurn());

        // Hai trang bị đã chọn, mỗi loại có một charge.
        assertEquals(2, snapshot.inventory().equipment().size());
        assertEquals(
            1,
            snapshot.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT)
        );
        assertEquals(
            1,
            snapshot.inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER)
        );
    }

    @Test
    void createsRocksAtCorrectPositions() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        Set<Position> expectedRocks = Set.of(
            new Position(2, 3),
            new Position(2, 4),
            new Position(3, 2),
            new Position(4, 5),
            new Position(5, 3),
            new Position(5, 4)
        );

        assertEquals(expectedRocks, battle.snapshot().rocks());
    }

    @Test
    void preservesSpawnOrderFromMap01() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleSnapshot snapshot = battle.snapshot();

        assertEquals(0, snapshot.pieceById("knight").orElseThrow().spawnOrder());
        assertEquals(1, snapshot.pieceById("pawn-1").orElseThrow().spawnOrder());
        assertEquals(2, snapshot.pieceById("pawn-2").orElseThrow().spawnOrder());
        assertEquals(3, snapshot.pieceById("rook-1").orElseThrow().spawnOrder());
        assertEquals(4, snapshot.pieceById("bishop-1").orElseThrow().spawnOrder());
        assertEquals(5, snapshot.pieceById("king-1").orElseThrow().spawnOrder());
    }

    @Test
    void createsBattleWithAnotherEquipmentSelection() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_ATTACK,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleSnapshot snapshot = battle.snapshot();

        assertEquals(2, snapshot.inventory().equipment().size());
        assertEquals(
            1,
            snapshot.inventory().chargesOf(EquipmentType.CANNON_ATTACK)
        );
        assertEquals(
            1,
            snapshot.inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER)
        );
    }

    @Test
    void rejectsDuplicateEquipment() {
        BattleFactory factory = new BattleFactory();

        assertThrows(
            IllegalArgumentException.class,
            () -> factory.createMap01(
                EquipmentType.CANNON_MOVEMENT,
                EquipmentType.CANNON_MOVEMENT
            )
        );
    }

    @Test
    void createsIndependentBattles() throws IOException {
        BattleFactory factory = new BattleFactory();

        BattleState first = factory.createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleState second = factory.createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleSnapshot secondBefore = second.snapshot();

        // Chỉ thay đổi trận thứ nhất.
        first.movePiece("knight", new Position(4, 2));
        first.replacePiece(
            first.pieceState("knight").orElseThrow().withHp(4)
        );
        first.consumeEquipment(EquipmentType.CANNON_MOVEMENT);

        BattleSnapshot firstAfter = first.snapshot();

        // Xác nhận trận thứ nhất thực sự đã thay đổi.
        assertEquals(
            new Position(4, 2),
            firstAfter.pieceById("knight").orElseThrow().position()
        );
        assertEquals(
            4,
            firstAfter.pieceById("knight").orElseThrow().hp()
        );
        assertEquals(
            0,
            firstAfter.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT)
        );

        // Trận thứ hai phải giữ nguyên toàn bộ trạng thái.
        assertEquals(
            secondBefore,
            second.snapshot(),
            "Thay đổi trận thứ nhất không được ảnh hưởng trận thứ hai"
        );
    }

    @Test
    void keepsOldSnapshotUnchanged() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleSnapshot before = battle.snapshot();

        battle.movePiece("knight", new Position(4, 2));
        battle.replacePiece(
            battle.pieceState("knight").orElseThrow().withHp(4)
        );
        battle.consumeEquipment(EquipmentType.CANNON_MOVEMENT);

        // Snapshot đã lấy trước đó vẫn phải giữ dữ liệu ban đầu.
        assertEquals(
            new Position(3, 0),
            before.pieceById("knight").orElseThrow().position()
        );
        assertEquals(
            5,
            before.pieceById("knight").orElseThrow().hp()
        );
        assertEquals(
            1,
            before.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT)
        );

        // Trạng thái hiện tại đã thay đổi.
        BattleSnapshot after = battle.snapshot();

        assertEquals(
            new Position(4, 2),
            after.pieceById("knight").orElseThrow().position()
        );
        assertEquals(
            4,
            after.pieceById("knight").orElseThrow().hp()
        );
        assertEquals(
            0,
            after.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT)
        );
    }

    @Test
    void createdBattleWorksWithController() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleController controller = new BattleController(battle);

        // Knight từ d1 (3, 0) đi hình L tới e3 (4, 2).
        controller.submit(ActionRequest.move("knight", 4, 2));

        BattleSnapshot afterMove = controller.snapshot();

        assertEquals(
            new Position(4, 2),
            afterMove.pieceById("knight").orElseThrow().position()
        );
        assertEquals(2, afterMove.globalTurn());
        assertEquals(BattlePhase.PLAYER_SELECT, afterMove.phase());
        assertEquals(BattleResult.ONGOING, afterMove.result());

        // d3 (3, 2) có đá: yêu cầu phải bị từ chối.
        controller.submit(ActionRequest.move("knight", 3, 2));

        assertEquals(
            afterMove,
            controller.snapshot(),
            "Đi vào đá phải giữ nguyên toàn bộ trạng thái trận"
        );
    }

    @Test
    void initializesPawnDirectionsFromCompleteMap() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleSnapshot snapshot = battle.snapshot();

        // Hai Pawn đều có bước SOUTH thuộc đường ngắn nhất
        // tới hàng phong cấp, nên ưu tiên hướng SOUTH.
        assertEquals(
            Direction.SOUTH,
            snapshot.pieceById("pawn-1").orElseThrow().currentDirection()
        );
        assertEquals(
            Direction.SOUTH,
            snapshot.pieceById("pawn-2").orElseThrow().currentDirection()
        );

        // Kiểm tra hướng đã được lưu trong state thật.
        assertEquals(
            Direction.SOUTH,
            battle.pieceState("pawn-1").orElseThrow().currentDirection()
        );
        assertEquals(
            Direction.SOUTH,
            battle.pieceState("pawn-2").orElseThrow().currentDirection()
        );

        // Tính hướng không làm di chuyển quân hoặc tăng lượt.
        assertEquals(
            new Position(2, 6),
            snapshot.pieceById("pawn-1").orElseThrow().position()
        );
        assertEquals(
            new Position(3, 6),
            snapshot.pieceById("pawn-2").orElseThrow().position()
        );
        assertEquals(1, snapshot.globalTurn());

        // Các quân khác không có hướng Pawn.
        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.type() != PieceType.PAWN) {
                assertNull(piece.currentDirection(), piece.id());
            }
        }
    }
}