package rogueknight.core.input;

import org.junit.jupiter.api.Test;
import rogueknight.action.BattleController;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import rogueknight.data.BattleFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleControllerAdapterTest {

    @Test
    void convertsInitialBattleToViewSnapshot() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        BattleViewSnapshot snapshot = adapter.snapshot();

        assertFalse(snapshot.isFixtureDemo());
        assertEquals(1, snapshot.globalTurn());
        assertEquals(new BoardPosition(3, 0), snapshot.knightPosition());

        assertEquals(6, snapshot.rocks().size());
        assertTrue(snapshot.rocks().contains(new BoardPosition(3, 2)));

        // Có sáu quân tổng cộng: một Knight và năm quân địch.
        assertEquals(5, snapshot.enemyPieces().size());
        assertEquals(
            "KING",
            snapshot.enemyPieces().get(new BoardPosition(3, 7))
        );
        assertEquals(
            "PAWN",
            snapshot.enemyPieces().get(new BoardPosition(2, 6))
        );
        assertEquals(
            "BISHOP",
            snapshot.enemyPieces().get(new BoardPosition(2, 7))
        );

        // e3 hợp lệ; d3 có đá nên không được highlight.
        assertTrue(snapshot.legalMoves().contains(new BoardPosition(4, 2)));
        assertFalse(snapshot.legalMoves().contains(new BoardPosition(3, 2)));
        assertEquals(adapter.legalMoves("knight"), snapshot.legalMoves());
    }

    @Test
    void forwardsValidMoveToLogicController() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        MoveResult result = adapter.submitMove(
            "knight",
            new BoardPosition(4, 2)
        );

        assertTrue(result.accepted());
        assertEquals(1, result.before().globalTurn());
        assertEquals(
            new BoardPosition(3, 0),
            result.before().knightPosition()
        );

        assertEquals(2, result.after().globalTurn());
        assertEquals(
            new BoardPosition(4, 2),
            result.after().knightPosition()
        );
        assertEquals(result.after(), adapter.snapshot());
    }

    @Test
    void rejectsMoveOntoRockWithoutChangingState() throws IOException {
        BattleControllerAdapter adapter = createAdapter();

        // Đưa Knight tới e3 trước.
        MoveResult firstMove = adapter.submitMove(
            "knight",
            new BoardPosition(4, 2)
        );
        assertTrue(firstMove.accepted());

        BattleViewSnapshot before = adapter.snapshot();

        // Từ e3 sang d3 là một ô kề nhưng bị đá chặn.
        MoveResult result = adapter.submitMove(
            "knight",
            new BoardPosition(3, 2)
        );

        assertFalse(result.accepted());
        assertNotNull(result.errorReason());
        assertEquals(before, result.before());
        assertEquals(before, result.after());
        assertEquals(before, adapter.snapshot());
    }

    @Test
    void rejectsNullTargetWithoutChangingState() throws IOException {
        BattleControllerAdapter adapter = createAdapter();
        BattleViewSnapshot before = adapter.snapshot();

        MoveResult result = adapter.submitMove("knight", null);

        assertFalse(result.accepted());
        assertEquals("OUT_OF_BOUNDS", result.errorReason());
        assertEquals(before, result.before());
        assertEquals(before, result.after());   
        assertEquals(before, adapter.snapshot());
    }

    @Test
    void handlesKnightDeathGracefullyWithoutCrashing() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );
        // Reduce knight HP to 1 and attack with Pawn
        battle.replacePiece(battle.pieceState("knight").orElseThrow().withHp(1));
        BattleController controller = new BattleController(battle, new rogueknight.action.PawnEnemyPhase());
        BattleControllerAdapter adapter = new BattleControllerAdapter(controller);

        // Knight moves near pawn so Pawn attacks Knight to 0 HP
        // Knight at d1 (3,0). Move to e3 (4,2).
        adapter.submitMove("knight", new BoardPosition(4, 2));

        // When knight reaches 0 HP, snapshot should indicate DEAD without throwing exception
        battle.removePiece("knight");
        battle.finish(rogueknight.battle.BattleResult.DEFEAT);

        BattleViewSnapshot snapshot = adapter.snapshot();
        assertTrue(snapshot.statusMessage().contains("DEAD"));
        assertTrue(snapshot.statusMessage().contains("DEFEAT"));
        assertTrue(snapshot.legalMoves().isEmpty());
    }

    private BattleControllerAdapter createAdapter() throws IOException {
        BattleState battle = new BattleFactory().createMap01(
            EquipmentType.CANNON_MOVEMENT,
            EquipmentType.DIAGONAL_SKEWER
        );

        BattleController controller = new BattleController(battle);

        return new BattleControllerAdapter(controller);
    }
}