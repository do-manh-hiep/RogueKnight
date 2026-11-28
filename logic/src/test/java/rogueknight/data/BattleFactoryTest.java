package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.action.ActionRequest;
import rogueknight.battle.BattleController;
import rogueknight.action.EnemyPolicy;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.Direction;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BattleFactoryTest {
    private BattleController create(EquipmentType first, EquipmentType second) throws IOException {
        return new BattleFactory().createMap01Controller(first, second, EnemyPolicy.NONE);
    }

    @Test
    void createsMap01WithInitialState() throws IOException {
        BattleSnapshot snapshot = create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER).snapshot();
        assertEquals(1, snapshot.globalTurn());
        assertEquals(BattlePhase.PLAYER_SELECT, snapshot.phase());
        assertEquals(BattleResult.ONGOING, snapshot.result());
        assertEquals(6, snapshot.rocks().size());
        assertEquals(6, snapshot.pieces().size());
        BattleSnapshot.PieceView knight = snapshot.pieceById("knight").orElseThrow();
        assertEquals(PieceType.KNIGHT, knight.type());
        assertEquals(new Position(3, 0), knight.position());
        assertEquals(5, knight.hp());
        assertEquals(0, knight.cycle());
        assertEquals(0, knight.nextActivationTurn());
        BattleSnapshot.PieceView pawn1 = snapshot.pieceById("pawn-1").orElseThrow();
        assertEquals(PieceType.PAWN, pawn1.type());
        assertEquals(new Position(2, 6), pawn1.position());
        assertEquals(1, pawn1.hp());
        assertEquals(2, pawn1.cycle());
        assertEquals(2, pawn1.nextActivationTurn());
        assertEquals(Direction.SOUTH, pawn1.currentDirection());
        assertEquals(1, snapshot.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertEquals(1, snapshot.inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
    }

    @Test
    void createsRocksAtCorrectPositions() throws IOException {
        assertEquals(java.util.Set.of(new Position(2,3), new Position(2,4), new Position(3,2),
            new Position(4,5), new Position(5,3), new Position(5,4)),
            create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER).snapshot().rocks());
    }

    @Test
    void preservesSpawnOrderFromMap01() throws IOException {
        BattleSnapshot snapshot = create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER).snapshot();
        assertEquals(0, snapshot.pieceById("knight").orElseThrow().spawnOrder());
        assertEquals(1, snapshot.pieceById("pawn-1").orElseThrow().spawnOrder());
        assertEquals(2, snapshot.pieceById("pawn-2").orElseThrow().spawnOrder());
        assertEquals(3, snapshot.pieceById("rook-1").orElseThrow().spawnOrder());
        assertEquals(4, snapshot.pieceById("bishop-1").orElseThrow().spawnOrder());
        assertEquals(5, snapshot.pieceById("king-1").orElseThrow().spawnOrder());
    }

    @Test
    void createsBattleWithAnotherEquipmentSelection() throws IOException {
        BattleSnapshot snapshot = create(EquipmentType.CANNON_ATTACK, EquipmentType.DIAGONAL_SKEWER).snapshot();
        assertEquals(1, snapshot.inventory().chargesOf(EquipmentType.CANNON_ATTACK));
        assertEquals(1, snapshot.inventory().chargesOf(EquipmentType.DIAGONAL_SKEWER));
    }

    @Test
    void rejectsDuplicateEquipment() {
        assertThrows(IllegalArgumentException.class,
            () -> new BattleFactory().createMap01Controller(
                EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_MOVEMENT, EnemyPolicy.NONE));
    }

    @Test
    void createsIndependentBattlesAndKeepsSnapshotsDetached() throws IOException {
        BattleController first = create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
        BattleController second = create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
        BattleSnapshot secondBefore = second.snapshot();
        BattleSnapshot firstBefore = first.snapshot();
        assertTrue(first.submit(ActionRequest.move("knight", 4, 2)).accepted());
        assertEquals(new Position(3,0), firstBefore.pieceById("knight").orElseThrow().position());
        assertEquals(new Position(4,2), first.snapshot().pieceById("knight").orElseThrow().position());
        assertEquals(secondBefore, second.snapshot());
    }

    @Test
    void createdBattleWorksWithController() throws IOException {
        BattleController controller = create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
        assertTrue(controller.submit(ActionRequest.move("knight", 4, 2)).accepted());
        BattleSnapshot afterMove = controller.snapshot();
        assertEquals(new Position(4,2), afterMove.pieceById("knight").orElseThrow().position());
        assertEquals(2, afterMove.globalTurn());
        assertEquals(BattlePhase.PLAYER_SELECT, afterMove.phase());
        assertEquals(BattleResult.ONGOING, afterMove.result());
        assertFalse(controller.submit(ActionRequest.move("knight", 3, 2)).accepted());
        assertEquals(afterMove, controller.snapshot());
    }

    @Test
    void initializesPawnDirectionsFromCompleteMap() throws IOException {
        BattleSnapshot snapshot = create(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER).snapshot();
        assertEquals(Direction.SOUTH, snapshot.pieceById("pawn-1").orElseThrow().currentDirection());
        assertEquals(Direction.SOUTH, snapshot.pieceById("pawn-2").orElseThrow().currentDirection());
        assertEquals(new Position(2,6), snapshot.pieceById("pawn-1").orElseThrow().position());
        assertEquals(new Position(3,6), snapshot.pieceById("pawn-2").orElseThrow().position());
        assertEquals(1, snapshot.globalTurn());
        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.type() != PieceType.PAWN) assertNull(piece.currentDirection(), piece.id());
        }
    }
}
