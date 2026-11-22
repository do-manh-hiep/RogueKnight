package rogueknight.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/*
 * KnightMoveTest  |  T-02.01  |  Bộ tình huống Knight Move trong hướng dẫn triển khai.
 * Fixture: Knight K1 ở d4; King KG ở h8 và Rook R1 ở a8 giữ trận ongoing,
 * không Enemy nào đến hạn (EnemyPhase.NONE).
 */
class KnightMoveTest {

    private static final String KNIGHT = "K1";

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static BattleState fixture(Set<Position> rocks) {
        BattleState state = new BattleState(rocks,
                Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER));
        state.addPiece(new PieceState(KNIGHT, PieceType.KNIGHT, 5, 0, 0, 0), at("d4"));
        state.addPiece(new PieceState("KG", PieceType.KING, 5, 5, 5, 1), at("h8"));
        state.addPiece(new PieceState("R1", PieceType.ROOK, 2, 3, 3, 2), at("a8"));
        return state;
    }

    private static BattleState fixture() {
        return fixture(Set.of());
    }

    private static void assertRejectedUnchanged(ActionResult result, ActionError expected, BattleSnapshot before,
                                                BattleController controller) {
        assertFalse(result.accepted());
        assertEquals(expected, result.error());
        assertEquals(before, result.after());
        assertEquals(before, controller.snapshot());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void lShapeToEmptyTileMovesKnight() {
        BattleController controller = new BattleController(fixture());

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertTrue(result.accepted());
        BattleSnapshot after = controller.snapshot();
        assertTrue(after.pieceAt(at("d4")).isEmpty());
        assertEquals(KNIGHT, after.pieceAt(at("e6")).orElseThrow().id());
        assertEquals(5, after.pieceById(KNIGHT).orElseThrow().hp());
        assertEquals(List.of(
                new BattleEvent.ActionPerformed(ActionKind.MOVE, KNIGHT),
                new BattleEvent.PieceMoved(KNIGHT, at("d4"), at("e6")),
                new BattleEvent.TurnAdvanced(1, 2)), result.events());
    }

    @Test
    void adjacentDiagonalToEmptyTileMovesKnight() {
        BattleController controller = new BattleController(fixture());

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e5")));

        assertTrue(result.accepted());
        assertEquals(at("e5"), controller.snapshot().pieceById(KNIGHT).orElseThrow().position());
    }

    @Test
    void twoTilesStraightIsInvalidGeometry() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("d6")));

        assertRejectedUnchanged(result, ActionError.INVALID_GEOMETRY, before, controller);
    }

    @Test
    void ownTileIsInvalidGeometry() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("d4")));

        assertRejectedUnchanged(result, ActionError.INVALID_GEOMETRY, before, controller);
    }

    @Test
    void moveToRockDoesNotChangeBattle() {
        BattleController controller = new BattleController(fixture(Set.of(at("e6"))));
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertRejectedUnchanged(result, ActionError.OCCUPIED_BY_ROCK, before, controller);
    }

    @Test
    void moveOntoPieceIsRejectedAndNeverCaptures() {
        BattleState state = fixture();
        state.addPiece(new PieceState("P1", PieceType.PAWN, 1, 2, 2, 3), at("e6"));
        BattleController controller = new BattleController(state);
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertRejectedUnchanged(result, ActionError.OCCUPIED_BY_PIECE, before, controller);
        assertEquals(1, controller.snapshot().pieceById("P1").orElseThrow().hp());
    }

    @Test
    void lShapeJumpsOverIntermediateBlockers() {
        // d5, e5, e4 bao quanh đường đi tới e6 nhưng e6 trống.
        BattleController controller = new BattleController(fixture(Set.of(at("d5"), at("e5"), at("e4"))));

        ActionResult result = controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertTrue(result.accepted());
        assertEquals(at("e6"), controller.snapshot().pieceById(KNIGHT).orElseThrow().position());
    }

    @Test
    void targetOutsideBoardIsRejectedBeforeMutation() {
        BattleController controller = new BattleController(fixture());
        BattleSnapshot before = controller.snapshot();

        assertRejectedUnchanged(controller.submit(ActionRequest.move(KNIGHT, 3, 8)),
                ActionError.OUT_OF_BOUNDS, before, controller);
        assertRejectedUnchanged(controller.submit(ActionRequest.move(KNIGHT, -1, 2)),
                ActionError.OUT_OF_BOUNDS, before, controller);
    }

    @Test
    void legalMovesFromD4MatchRuleWithoutRocks() {
        BattleController controller = new BattleController(fixture());
        Set<Position> expected = new HashSet<>();
        for (String n : List.of("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5",
                "c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5")) {
            expected.add(at(n));
        }

        assertEquals(expected, controller.legalMoves(KNIGHT));
    }

    @Test
    void legalMovesExcludeRocksAndPieces() {
        BattleState state = fixture(Set.of(at("e6"), at("c4")));
        state.addPiece(new PieceState("P1", PieceType.PAWN, 1, 2, 2, 3), at("e5"));
        BattleController controller = new BattleController(state);

        Set<Position> legal = controller.legalMoves(KNIGHT);

        assertFalse(legal.contains(at("e6")));
        assertFalse(legal.contains(at("c4")));
        assertFalse(legal.contains(at("e5")));
        assertEquals(13, legal.size());
    }

    @Test
    void everyHighlightedTargetIsAcceptedBySubmit() {
        BattleState probe = fixture(Set.of(at("e6"), at("c4")));
        Set<Position> legal = new BattleController(probe).legalMoves(KNIGHT);

        for (Position target : legal) {
            BattleController controller = new BattleController(fixture(Set.of(at("e6"), at("c4"))));
            assertTrue(controller.submit(ActionRequest.move(KNIGHT, target)).accepted(), "target " + target);
        }
        for (int row = 0; row < Position.BOARD_SIZE; row++) {
            for (int column = 0; column < Position.BOARD_SIZE; column++) {
                Position target = new Position(column, row);
                if (!legal.contains(target)) {
                    BattleController controller = new BattleController(fixture(Set.of(at("e6"), at("c4"))));
                    assertFalse(controller.submit(ActionRequest.move(KNIGHT, target)).accepted(), "target " + target);
                }
            }
        }
    }

    @Test
    void validMoveReturnsToPlayerSelect() {
        BattleController controller = new BattleController(fixture());

        controller.submit(ActionRequest.move(KNIGHT, at("e6")));

        assertEquals(BattlePhase.PLAYER_SELECT, controller.snapshot().phase());
    }
}
