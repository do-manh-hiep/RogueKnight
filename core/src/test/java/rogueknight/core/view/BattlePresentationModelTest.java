package rogueknight.core.view;

import org.junit.jupiter.api.Test;
import rogueknight.action.BattleEvent;
import rogueknight.action.ActionKind;
import rogueknight.battle.BattleResult;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BattlePresentationModelTest {

    @Test
    void bootstrapsAllPiecesByStableId() {
        BattlePresentationModel model = BattlePresentationModel.from(snapshot());

        assertEquals(1, model.globalTurn());
        assertEquals(BoardPosition.fromNotation("d4"),
            model.piece("knight").orElseThrow().position());
        assertEquals(PieceType.ROOK, model.piece("rook-17").orElseThrow().type());
        assertEquals(2, model.piecesById().size());
    }

    @Test
    void moveUpdatesPositionByPieceIdNotByTypeOrSquare() {
        BattlePresentationModel model = BattlePresentationModel.from(snapshot());

        model.apply(new BattleEvent.ActionPerformed(ActionKind.MOVE, "rook-17"));
        model.apply(new BattleEvent.PieceMoved(
            "rook-17", Position.fromNotation("e4"), Position.fromNotation("e5")));

        assertEquals(BoardPosition.fromNotation("e5"),
            model.piece("rook-17").orElseThrow().position());
        assertEquals(BoardPosition.fromNotation("d4"),
            model.piece("knight").orElseThrow().position());
    }

    @Test
    void damageThenRemovalDeletesOnlyTheIdentifiedPiece() {
        BattlePresentationModel model = BattlePresentationModel.from(snapshot());

        model.apply(new BattleEvent.PieceDamaged("rook-17", 2, 0));
        model.apply(new BattleEvent.PieceRemoved("rook-17", Position.fromNotation("e4")));

        assertTrue(model.piece("rook-17").isEmpty());
        assertTrue(model.piece("knight").isPresent());
    }

    @Test
    void promotionChangesTypeAndPreservesPieceId() {
        BattlePresentationModel model = BattlePresentationModel.from(snapshot());

        model.apply(new BattleEvent.PiecePromoted(
            "rook-17", PieceType.ROOK, PieceType.QUEEN, Position.fromNotation("e4")));

        var promoted = model.piece("rook-17").orElseThrow();
        assertEquals("rook-17", promoted.id());
        assertEquals(PieceType.QUEEN, promoted.type());
    }

    @Test
    void advancesTurnAndRecordsTerminalResult() {
        BattlePresentationModel model = BattlePresentationModel.from(snapshot());

        model.apply(new BattleEvent.TurnAdvanced(1, 2));
        model.apply(new BattleEvent.BattleEnded(BattleResult.DEFEAT));

        assertEquals(2, model.globalTurn());
        assertEquals(BattleResult.DEFEAT, model.result().orElseThrow());
        assertThrows(IllegalStateException.class,
            () -> model.apply(new BattleEvent.ActorActivated("rook-17", Position.fromNotation("e4"), 2)));
    }

    @Test
    void rejectsOutOfOrderMovementInsteadOfGuessingPieceIdentity() {
        BattlePresentationModel model = BattlePresentationModel.from(snapshot());

        assertThrows(IllegalStateException.class, () -> model.apply(new BattleEvent.PieceMoved(
            "rook-17", Position.fromNotation("a1"), Position.fromNotation("a2"))));
    }

    private static BattleViewSnapshot snapshot() {
        BoardPosition knightPosition = BoardPosition.fromNotation("d4");
        PresentationPiece knight = new PresentationPiece("knight", PieceType.KNIGHT, 5, knightPosition);
        PresentationPiece rook = new PresentationPiece(
            "rook-17", PieceType.ROOK, 3, BoardPosition.fromNotation("e4"));

        return new BattleViewSnapshot(
            1, knightPosition, true, Set.of(), Map.of(BoardPosition.fromNotation("e4"), "ROOK"),
            Set.of(), "Turn 1", false, Map.of("knight", knight, "rook-17", rook)
        );
    }
}
