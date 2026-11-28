package rogueknight.battle;

import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;
import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    @Test void placesAndQueriesPieceByIdAndTile() {
        Board board = new Board(Set.of());
        Position d4 = Position.fromNotation("d4");
        board.place("K1", d4);
        assertEquals(d4, board.positionOf("K1").orElseThrow());
        assertEquals("K1", board.pieceAt(d4).orElseThrow());
        assertTrue(board.pieceAt(Position.fromNotation("e4")).isEmpty());
    }
    @Test void rejectsDuplicateIdsAndOccupiedTilesWithoutMutation() {
        Board board = new Board(Set.of());
        Position d4 = Position.fromNotation("d4");
        board.place("K1", d4);
        assertThrows(IllegalArgumentException.class, () -> board.place("K1", Position.fromNotation("e4")));
        assertThrows(IllegalArgumentException.class, () -> board.place("E1", d4));
        assertEquals(d4, board.positionOf("K1").orElseThrow());
        assertTrue(board.positionOf("E1").isEmpty());
    }
    @Test void rockOccupiesTileAndCannotBeOverwritten() {
        Position c4 = Position.fromNotation("c4");
        Board board = new Board(Set.of(c4));
        assertTrue(board.isRock(c4));
        assertThrows(IllegalArgumentException.class, () -> board.place("K1", c4));
        assertTrue(board.pieceIds().isEmpty());
    }
    @Test void moveUpdatesSingleSourceOfPosition() {
        Board board = new Board(Set.of());
        Position d4 = Position.fromNotation("d4");
        Position e6 = Position.fromNotation("e6");
        board.place("K1", d4);
        board.move("K1", e6);
        assertTrue(board.pieceAt(d4).isEmpty());
        assertEquals("K1", board.pieceAt(e6).orElseThrow());
        assertEquals(e6, board.positionOf("K1").orElseThrow());
    }
    @Test void failedMoveLeavesPositionUnchanged() {
        Position rock = Position.fromNotation("e6");
        Board board = new Board(Set.of(rock));
        Position d4 = Position.fromNotation("d4");
        board.place("K1", d4);
        assertThrows(IllegalArgumentException.class, () -> board.move("K1", rock));
        assertEquals(d4, board.positionOf("K1").orElseThrow());
    }
    @Test void pieceIdsReturnsDetachedImmutableSnapshot() {
        Board board = new Board(Set.of());
        board.place("K1", Position.fromNotation("d4"));
        Set<String> idsBefore = board.pieceIds();
        board.place("E1", Position.fromNotation("h8"));
        assertEquals(Set.of("K1"), idsBefore);
        assertEquals(Set.of("K1", "E1"), board.pieceIds());
        assertThrows(UnsupportedOperationException.class, () -> idsBefore.add("E2"));
    }
}
