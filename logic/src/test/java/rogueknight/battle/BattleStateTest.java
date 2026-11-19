package rogueknight.battle;

import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;
import static org.junit.jupiter.api.Assertions.*;

class BattleStateTest {
    private static PieceState knight() { return new PieceState("K1", PieceType.KNIGHT, 5, 0, 0, 0); }
    private static PieceState enemy() { return new PieceState("E1", PieceType.ROOK, 2, 3, 4, 1); }
    private static Inventory loadout() { return Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER); }

    @Test void ownsBoardPiecesAndInventoryAndBuildsSnapshot() {
        BattleState state = new BattleState(Set.of(Position.fromNotation("c4")), loadout());
        state.addPiece(knight(), Position.fromNotation("d4")); state.addPiece(enemy(), Position.fromNotation("h8"));
        BattleSnapshot snapshot = state.snapshot();
        assertEquals(1, snapshot.globalTurn());
        assertEquals(Position.fromNotation("d4"), snapshot.pieceById("K1").orElseThrow().position());
        assertEquals("E1", snapshot.pieceAt(Position.fromNotation("h8")).orElseThrow().id());
        assertTrue(snapshot.rocks().contains(Position.fromNotation("c4")));
        assertEquals(loadout(), snapshot.inventory());
    }
    @Test void movingPieceUpdatesBoardButNotPieceStats() {
        BattleState state = new BattleState(Set.of(), loadout()); state.addPiece(knight(), Position.fromNotation("d4"));
        state.movePiece("K1", Position.fromNotation("e6"));
        assertTrue(state.pieceAt(Position.fromNotation("d4")).isEmpty());
        assertEquals("K1", state.pieceAt(Position.fromNotation("e6")).orElseThrow());
        assertEquals(5, state.pieceState("K1").orElseThrow().hp());
    }
    @Test void rejectsDuplicateIdsAndOverlappingPiecesAtomically() {
        BattleState state = new BattleState(Set.of(), loadout()); state.addPiece(knight(), Position.fromNotation("d4"));
        assertThrows(IllegalArgumentException.class, () -> state.addPiece(knight(), Position.fromNotation("e4")));
        assertThrows(IllegalArgumentException.class, () -> state.addPiece(enemy(), Position.fromNotation("d4")));
        assertEquals(1, state.snapshot().pieces().size()); assertTrue(state.pieceState("E1").isEmpty());
    }
    @Test void rejectsUnknownPieceMutation() {
        BattleState state = new BattleState(Set.of(), loadout());
        assertThrows(IllegalArgumentException.class, () -> state.movePiece("missing", Position.fromNotation("a1")));
        assertThrows(IllegalArgumentException.class, () -> state.removePiece("missing"));
        assertThrows(IllegalArgumentException.class, () -> state.replacePiece(new PieceState("missing", PieceType.PAWN, 1, 2, 2, 2)));
    }
    @Test void removingPieceRemovesBothStateAndOccupancy() {
        BattleState state = new BattleState(Set.of(), loadout()); Position d4 = Position.fromNotation("d4");
        state.addPiece(knight(), d4); state.removePiece("K1");
        assertTrue(state.pieceState("K1").isEmpty()); assertTrue(state.positionOf("K1").isEmpty()); assertTrue(state.isEmpty(d4));
    }
    @Test void replacingPiecePreservesIdentityAndUpdatesStats() {
        BattleState state = new BattleState(Set.of(), loadout()); state.addPiece(knight(), Position.fromNotation("d4"));
        state.replacePiece(knight().withHp(3));
        assertEquals(3, state.pieceState("K1").orElseThrow().hp());
        assertEquals(Position.fromNotation("d4"), state.positionOf("K1").orElseThrow());
    }
    @Test void rejectsZeroHpPieceInBattleAggregate() {
        BattleState state = new BattleState(Set.of(), loadout());
        assertThrows(IllegalArgumentException.class, () -> state.addPiece(knight().withHp(0), Position.fromNotation("d4")));
        state.addPiece(knight(), Position.fromNotation("d4"));
        assertThrows(IllegalArgumentException.class, () -> state.replacePiece(knight().withHp(0)));
        assertEquals(5, state.pieceState("K1").orElseThrow().hp());
        assertEquals(Position.fromNotation("d4"), state.positionOf("K1").orElseThrow());
    }
    @Test void snapshotIsDetachedAndCollectionsAreImmutable() {
        BattleState state = new BattleState(Set.of(Position.fromNotation("c4")), loadout());
        state.addPiece(knight(), Position.fromNotation("d4")); BattleSnapshot before = state.snapshot();
        state.movePiece("K1", Position.fromNotation("e6")); state.replacePiece(knight().withHp(2));
        state.consumeEquipment(EquipmentType.CANNON_MOVEMENT);
        assertEquals(Position.fromNotation("d4"), before.pieceById("K1").orElseThrow().position());
        assertEquals(5, before.pieceById("K1").orElseThrow().hp());
        assertEquals(1, before.inventory().chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertThrows(UnsupportedOperationException.class, () -> before.pieces().clear());
        assertThrows(UnsupportedOperationException.class, () -> before.rocks().clear());
        assertThrows(UnsupportedOperationException.class, () -> before.inventory().equipment().clear());
    }
    @Test void snapshotsCompareByValueAndHaveDeterministicPieceOrder() {
        BattleState a = new BattleState(Set.of(), loadout()); a.addPiece(enemy(), Position.fromNotation("h8")); a.addPiece(knight(), Position.fromNotation("d4"));
        BattleState b = new BattleState(Set.of(), loadout()); b.addPiece(knight(), Position.fromNotation("d4")); b.addPiece(enemy(), Position.fromNotation("h8"));
        assertEquals(a.snapshot(), b.snapshot()); assertEquals("K1", a.snapshot().pieces().get(0).id());
    }
    @Test void replacePieceCannotChangeStableSpawnOrder() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(knight(), Position.fromNotation("d4"));
        assertThrows(IllegalArgumentException.class, () -> state.replacePiece(
                new PieceState("K1", PieceType.KNIGHT, 4, 0, 0, 99)));
        assertEquals(0, state.pieceState("K1").orElseThrow().spawnOrder());
    }
    @Test void readOnlyQueriesDoNotMutateBattle() {
        BattleState state = new BattleState(Set.of(Position.fromNotation("c4")), loadout());
        state.addPiece(knight(), Position.fromNotation("d4"));
        BattleSnapshot before = state.snapshot();
        assertEquals("K1", state.pieceAt(Position.fromNotation("d4")).orElseThrow());
        assertEquals(Position.fromNotation("d4"), state.positionOf("K1").orElseThrow());
        assertFalse(state.isEmpty(Position.fromNotation("d4")));
        assertFalse(state.isEmpty(Position.fromNotation("c4")));
        assertTrue(state.isEmpty(Position.fromNotation("e4")));
        assertEquals(before, state.snapshot());
    }
    @Test void globalTurnStartsAtOne() { assertEquals(1, new BattleState(Set.of(), loadout()).globalTurn()); }
    @Test void startsOngoingInPlayerSelectAndFinishMovesToFinished() {
        BattleState state = new BattleState(Set.of(), loadout());
        assertEquals(BattlePhase.PLAYER_SELECT, state.snapshot().phase());
        assertEquals(BattleResult.ONGOING, state.snapshot().result());
        assertThrows(IllegalArgumentException.class, () -> state.finish(BattleResult.ONGOING));
        state.finish(BattleResult.DEFEAT);
        assertEquals(BattlePhase.FINISHED, state.phase());
        assertEquals(BattleResult.DEFEAT, state.snapshot().result());
    }
    @Test void advanceTurnIsReflectedOnlyInLaterSnapshots() {
        BattleState state = new BattleState(Set.of(), loadout());
        BattleSnapshot first = state.snapshot();
        state.advanceTurn();
        assertEquals(1, first.globalTurn());
        assertEquals(2, state.snapshot().globalTurn());
    }
}
