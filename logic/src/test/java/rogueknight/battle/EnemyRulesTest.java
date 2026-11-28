package rogueknight.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.action.ActionError;
import rogueknight.action.ActionRequest;
import rogueknight.action.ActionResult;
import rogueknight.action.ActionKind;
import rogueknight.board.Position;

/*
 * EnemyRulesTest  |  T-03.02  |  TC-E04, TC-B03, TC-B04, R-E02..R-E04, C-02
 * Hình học Move/Attack của Rook/Bishop/King/Queen; expected liệt kê tay theo luật GDD, không gọi lại thuật toán.
 */
class EnemyRulesTest {

    private static Position at(String notation) {
        return Position.fromNotation(notation);
    }

    private static List<Position> squares(String... notations) {
        return Arrays.stream(notations).map(Position::fromNotation).toList();
    }

    private static BattleState battle(Set<Position> rocks) {
        return new BattleState(rocks, Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK));
    }

    private static PieceState enemy(String id, PieceType type, int spawnOrder) {
        return new PieceState(id, type, 2, 3, 3, spawnOrder, null);
    }

    private static PieceState knight() {
        return new PieceState("K", PieceType.KNIGHT, 5, 0, 0, 0);
    }

    @Test
    void rookSlidesOrthogonallyAndStopsBeforeRockPieceAndKnight() {
        BattleState state = battle(Set.of(at("d6")));
        state.addPiece(knight(), at("d2"));
        state.addPiece(enemy("R", PieceType.ROOK, 1), at("d4"));
        state.addPiece(enemy("B", PieceType.BISHOP, 2), at("f4"));

        assertEquals(squares("a4", "b4", "c4", "d3", "d5", "e4"), EnemyRules.moveDestinations(state, "R"));
    }

    @Test
    void bishopSlidesDiagonallyAndStopsBeforeBlockers() {
        BattleState state = battle(Set.of(at("f6")));
        state.addPiece(knight(), at("b2"));
        state.addPiece(enemy("B", PieceType.BISHOP, 1), at("d4"));

        assertEquals(squares("a7", "b6", "c3", "c5", "e3", "e5", "f2", "g1"), EnemyRules.moveDestinations(state, "B"));
    }

    @Test
    void queenCombinesRookAndBishopRays() {
        BattleState state = battle(Set.of());
        state.addPiece(knight(), at("c3"));
        state.addPiece(enemy("KG", PieceType.KING, 1), at("h1"));
        state.addPiece(enemy("Q", PieceType.QUEEN, 2), at("a1"));

        List<Position> moves = EnemyRules.moveDestinations(state, "Q");

        assertEquals(squares("a2", "a3", "a4", "a5", "a6", "a7", "a8",
                "b1", "b2", "c1", "d1", "e1", "f1", "g1"), moves);
        assertFalse(moves.contains(at("c3")), "Queen never moves onto the Knight");
        assertFalse(moves.contains(at("h1")), "Queen never moves onto another Enemy");
    }

    @Test
    void kingStepsOneSquareOntoEmptySquaresOnly() {
        BattleState state = battle(Set.of(at("d6")));
        state.addPiece(knight(), at("f4"));
        state.addPiece(enemy("KG", PieceType.KING, 1), at("e5"));

        assertEquals(squares("d4", "d5", "e4", "e6", "f5", "f6"), EnemyRules.moveDestinations(state, "KG"));
    }

    @Test
    void attackRangeIsAdjacentOnlyAndDependsOnType() {
        BattleState state = battle(Set.of());
        state.addPiece(knight(), at("h8"));
        state.addPiece(enemy("R", PieceType.ROOK, 1), at("d4"));
        state.addPiece(enemy("B", PieceType.BISHOP, 2), at("b7"));
        state.addPiece(enemy("KG", PieceType.KING, 3), at("a1"));

        assertEquals(squares("c4", "d3", "d5", "e4"), EnemyRules.attackCells(state, "R"));
        assertEquals(squares("a6", "a8", "c6", "c8"), EnemyRules.attackCells(state, "B"));
        assertEquals(squares("a2", "b1", "b2"), EnemyRules.attackCells(state, "KG"));
    }

    @Test
    void canAttackKnightFollowsTypeGeometry() {
        BattleState diagonal = battle(Set.of());
        diagonal.addPiece(knight(), at("e5"));
        diagonal.addPiece(enemy("R", PieceType.ROOK, 1), at("d4"));
        diagonal.addPiece(enemy("B", PieceType.BISHOP, 2), at("f4"));
        assertFalse(EnemyRules.canAttackKnight(diagonal, "R"), "Rook attacks orthogonally adjacent squares only");
        assertTrue(EnemyRules.canAttackKnight(diagonal, "B"));

        BattleState sameFileTwoAway = battle(Set.of());
        sameFileTwoAway.addPiece(knight(), at("d6"));
        sameFileTwoAway.addPiece(enemy("R", PieceType.ROOK, 1), at("d4"));
        assertFalse(EnemyRules.canAttackKnight(sameFileTwoAway, "R"), "Rook moves far but attacks only adjacent");
    }

    @Test
    void validateAcceptsOnlyLegalEnemyMoves() {
        BattleState state = battle(Set.of(at("d6")));
        state.addPiece(knight(), at("d2"));
        state.addPiece(enemy("R", PieceType.ROOK, 1), at("d4"));
        state.addPiece(new PieceState("P", PieceType.PAWN, 1, 2, 2, 2), at("a7"));

        assertTrue(EnemyRules.validate(state, ActionRequest.move("R", at("d3"))).isValid());
        assertEquals(ActionError.OCCUPIED_BY_PIECE, EnemyRules.validate(state, ActionRequest.move("R", at("d2"))).error());
        assertEquals(ActionError.INVALID_GEOMETRY, EnemyRules.validate(state, ActionRequest.move("R", at("d1"))).error());
        assertEquals(ActionError.INVALID_GEOMETRY, EnemyRules.validate(state, ActionRequest.move("R", at("e5"))).error());
        assertEquals(ActionError.OCCUPIED_BY_ROCK, EnemyRules.validate(state, ActionRequest.move("R", at("d6"))).error());
        assertEquals(ActionError.OUT_OF_BOUNDS, EnemyRules.validate(state, ActionRequest.move("R", 3, 8)).error());
        assertEquals(ActionError.INVALID_GEOMETRY, EnemyRules.validate(state, ActionRequest.attack("R", at("d2"))).error());
        assertEquals(ActionError.UNSUPPORTED_ACTION, EnemyRules.validate(state, ActionRequest.sword("R", at("d3"))).error());
        assertEquals(ActionError.NOT_ENEMY_ACTOR, EnemyRules.validate(state, ActionRequest.move("K", at("c2"))).error());
        assertEquals(ActionError.UNKNOWN_ACTOR, EnemyRules.validate(state, ActionRequest.move("ghost", at("d3"))).error());
    }

    @Test
    void pawnStepsOneSquareInEightDirectionsAndAttacksItsFacingCone() {
        BattleState state = battle(Set.of(at("c4")));
        state.addPiece(knight(), at("d4"));
        state.addPiece(new PieceState("P", PieceType.PAWN, 1, 2, 2, 1, Direction.SOUTH), at("d5"));
        state.addPiece(enemy("KG", PieceType.KING, 2), at("e6"));

        // DEC-01: 8 neighbours, Rock/Knight/Enemy squares excluded; which one is used is decided by BFS.
        assertEquals(squares("c5", "c6", "d6", "e4", "e5"), EnemyRules.moveDestinations(state, "P"));
        // Facing SOUTH: d4 and the two 45-degree squares c4, e4 (Rock squares still belong to the area).
        assertEquals(squares("c4", "d4", "e4"), EnemyRules.attackCells(state, "P"));
        assertTrue(EnemyRules.canAttackKnight(state, "P"));
        assertFalse(EnemyRules.isGenericEnemy(PieceType.PAWN), "Pawn never gets a post-Move Attack");

        assertTrue(EnemyRules.validate(state, ActionRequest.move("P", at("c6"))).isValid());
        assertTrue(EnemyRules.validate(state, ActionRequest.attack("P", at("d4"))).isValid());
        assertEquals(ActionError.INVALID_GEOMETRY, EnemyRules.validate(state, ActionRequest.move("P", at("d7"))).error());
        assertEquals(ActionError.OCCUPIED_BY_PIECE, EnemyRules.validate(state, ActionRequest.move("P", at("d4"))).error());
    }

    @Test
    void pawnDoesNotAttackOutsideItsStoredDirection() {
        BattleState state = battle(Set.of());
        state.addPiece(knight(), at("d4"));
        state.addPiece(new PieceState("P", PieceType.PAWN, 1, 2, 2, 1, Direction.EAST), at("d5"));

        assertEquals(squares("e4", "e5", "e6"), EnemyRules.attackCells(state, "P"));
        assertFalse(EnemyRules.canAttackKnight(state, "P"), "the Knight directly south is outside an EAST cone");
        assertEquals(ActionError.INVALID_GEOMETRY, EnemyRules.validate(state, ActionRequest.attack("P", at("d4"))).error());
    }

    @Test
    void validateAttackRequiresKnightInsideAttackRange() {
        BattleState state = battle(Set.of());
        state.addPiece(knight(), at("d2"));
        state.addPiece(enemy("R", PieceType.ROOK, 1), at("d3"));
        state.addPiece(enemy("B", PieceType.BISHOP, 2), at("e3"));

        assertTrue(EnemyRules.validate(state, ActionRequest.attack("R", at("d2"))).isValid());
        assertEquals(ActionError.NO_TARGET_PIECE, EnemyRules.validate(state, ActionRequest.attack("R", at("d4"))).error());
        assertEquals(ActionError.NOT_ENEMY_TARGET, EnemyRules.validate(state, ActionRequest.attack("R", at("e3"))).error());
    }

    @Test
    void queriesAndValidationDoNotMutateState() {
        BattleState state = battle(Set.of(at("d6")));
        state.addPiece(knight(), at("d2"));
        state.addPiece(enemy("R", PieceType.ROOK, 1), at("d4"));
        BattleSnapshot before = state.snapshot();

        EnemyRules.moveDestinations(state, "R");
        EnemyRules.attackCells(state, "R");
        EnemyRules.canAttackKnight(state, "R");
        EnemyRules.validate(state, ActionRequest.move("R", at("d3")));
        EnemyRules.validate(state, ActionRequest.attack("R", at("d2")));

        assertEquals(before, state.snapshot());
    }

    @Test
    void playerCannotSubmitTheEnemyAttackKind() {
        BattleState state = battle(Set.of());
        state.addPiece(knight(), at("d4"));
        state.addPiece(enemy("KG", PieceType.KING, 1), at("h8"));
        state.addPiece(enemy("R", PieceType.ROOK, 2), at("d5"));
        BattleController controller = new BattleController(state);
        BattleSnapshot before = controller.snapshot();

        ActionResult result = controller.submit(ActionRequest.attack("K", at("d5")));

        assertFalse(result.accepted());
        assertEquals(ActionError.UNSUPPORTED_ACTION, result.error());
        assertEquals(before, controller.snapshot());
        assertTrue(controller.legalTargets("K", ActionKind.ATTACK).isEmpty());
    }
}
