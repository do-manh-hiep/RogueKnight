package rogueknight.battle;

import rogueknight.action.*;


import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PawnPathfinderTest {

    private static Inventory loadout() {
        return Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
    }

    @Test
    @DisplayName("Pawn moves straight SOUTH when no obstacles are present")
    void movesStraightSouthWithoutObstacles() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 6)); // d7

        Optional<PawnPathfinder.NextMove> move = PawnPathfinder.findNextMove(state, "pawn-1");

        assertTrue(move.isPresent());
        assertEquals(new Position(3, 5), move.get().target()); // d6
        assertEquals(Direction.SOUTH, move.get().direction());
    }

    @Test
    @DisplayName("Pawn sidesteps around Rock obstacle towards row 0")
    void sidestepsRockObstacle() {
        // Rock directly south of Pawn at d6 (3, 5)
        Set<Position> rocks = Set.of(new Position(3, 5));
        BattleState state = new BattleState(rocks, loadout());
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 6));

        Optional<PawnPathfinder.NextMove> move = PawnPathfinder.findNextMove(state, "pawn-1");

        assertTrue(move.isPresent());
        // Can step diagonally down (c5: 2,5 or e5: 4,5). Tie-breaking column asc picks c5 (2, 5)
        assertEquals(new Position(2, 5), move.get().target());
        assertEquals(Direction.SOUTH_WEST, move.get().direction());
    }

    @Test
    @DisplayName("Pawn sidesteps around another Enemy piece")
    void sidestepsEnemyPiece() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 6));
        // Another enemy at d6 (3, 5)
        state.addPiece(new PieceState("rook-1", PieceType.ROOK, 2, 3, 3, 2), new Position(3, 5));

        Optional<PawnPathfinder.NextMove> move = PawnPathfinder.findNextMove(state, "pawn-1");

        assertTrue(move.isPresent());
        assertEquals(new Position(2, 5), move.get().target());
        assertEquals(Direction.SOUTH_WEST, move.get().direction());
    }

    @Test
    @DisplayName("Knight directly in front blocks first step, forcing Pawn to pick alternative path")
    void knightBlocksFirstStepForcingAlternative() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 6));
        // Knight at d6 (3, 5)
        state.addPiece(new PieceState("knight", PieceType.KNIGHT, 5, 0, 0, 0), new Position(3, 5));

        Optional<PawnPathfinder.NextMove> move = PawnPathfinder.findNextMove(state, "pawn-1");

        assertTrue(move.isPresent());
        // Cannot land on Knight at (3, 5), picks diagonal down (2, 5)
        assertEquals(new Position(2, 5), move.get().target());
        assertEquals(Direction.SOUTH_WEST, move.get().direction());
    }

    @Test
    @DisplayName("Pawn completely boxed in returns empty")
    void trappedPawnReturnsEmpty() {
        // Corner at (0, 7) boxed by rocks at (0, 6), (1, 6), (1, 7)
        Set<Position> rocks = Set.of(
                new Position(0, 6),
                new Position(1, 6),
                new Position(1, 7)
        );
        BattleState state = new BattleState(rocks, loadout());
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(0, 7));

        Optional<PawnPathfinder.NextMove> move = PawnPathfinder.findNextMove(state, "pawn-1");

        assertTrue(move.isEmpty());
    }

    @Test
    @DisplayName("Pawn already at row 0 returns empty")
    void pawnAlreadyAtRow0ReturnsEmpty() {
        BattleState state = new BattleState(Set.of(), loadout());
        state.addPiece(new PieceState("pawn-1", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), new Position(3, 0));

        Optional<PawnPathfinder.NextMove> move = PawnPathfinder.findNextMove(state, "pawn-1");

        assertTrue(move.isEmpty());
    }
}
