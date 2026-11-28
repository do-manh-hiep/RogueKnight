package rogueknight.core.view;

import org.junit.jupiter.api.Test;
import rogueknight.action.ActionKind;
import rogueknight.action.BattleEvent;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BattlePlaybackTest {

    @Test
    void appliesEventsOnlyWhenTheirPlaybackIntervalHasElapsed() {
        BattlePlayback playback = new BattlePlayback(
            BattlePresentationModel.from(snapshot()), 0.2f);
        playback.enqueue(List.of(
            new BattleEvent.PieceMoved("rook-1", Position.fromNotation("e4"), Position.fromNotation("e5")),
            new BattleEvent.PieceMoved("rook-1", Position.fromNotation("e5"), Position.fromNotation("e6"))
        ));

        assertEquals(0, playback.update(0.19f));
        assertEquals(BoardPosition.fromNotation("e4"),
            playback.model().piece("rook-1").orElseThrow().position());

        assertEquals(1, playback.update(0.01f));
        assertEquals(BoardPosition.fromNotation("e5"),
            playback.model().piece("rook-1").orElseThrow().position());

        assertEquals(1, playback.update(0.2f));
        assertEquals(BoardPosition.fromNotation("e6"),
            playback.model().piece("rook-1").orElseThrow().position());
        assertFalse(playback.isPlaying());
    }

    @Test
    void skippedActivationIsConsumedInOrderWithoutInventingAnAction() {
        BattlePlayback playback = new BattlePlayback(
            BattlePresentationModel.from(snapshot()), 0.1f);
        playback.enqueue(List.of(
            new BattleEvent.ActorActivated("rook-1", Position.fromNotation("e4"), 1),
            new BattleEvent.ActorActivated("rook-1", Position.fromNotation("e4"), 1),
            new BattleEvent.ActionPerformed(ActionKind.MOVE, "rook-1"),
            new BattleEvent.PieceMoved("rook-1", Position.fromNotation("e4"), Position.fromNotation("e5"))
        ));

        assertEquals(2, playback.update(0.2f));
        assertEquals(BoardPosition.fromNotation("e4"),
            playback.model().piece("rook-1").orElseThrow().position());
        assertEquals(2, playback.pendingEventCount());

        assertEquals(2, playback.update(0.2f));
        assertEquals(BoardPosition.fromNotation("e5"),
            playback.model().piece("rook-1").orElseThrow().position());
    }

    @Test
    void playsKnightDeathAndTerminalEventInSequence() {
        BattlePlayback playback = new BattlePlayback(
            BattlePresentationModel.from(snapshot()), 0.1f);
        playback.enqueue(List.of(
            new BattleEvent.PieceDamaged("knight", 5, 0),
            new BattleEvent.PieceRemoved("knight", Position.fromNotation("d4")),
            new BattleEvent.BattleEnded(rogueknight.battle.BattleResult.DEFEAT)
        ));

        assertEquals(1, playback.update(0.1f));
        assertEquals(0, playback.model().piece("knight").orElseThrow().hp());
        assertTrue(playback.model().result().isEmpty());

        assertEquals(1, playback.update(0.1f));
        assertTrue(playback.model().piece("knight").isEmpty());
        assertTrue(playback.model().result().isEmpty());

        assertEquals(1, playback.update(0.1f));
        assertEquals(rogueknight.battle.BattleResult.DEFEAT,
            playback.model().result().orElseThrow());
        assertFalse(playback.isPlaying());
    }

    @Test
    void fasterAndSlowerCycleThroughNormalFastAndInstant() {
        BattlePlayback playback = new BattlePlayback(
            BattlePresentationModel.from(snapshot()), 0.18f);

        assertEquals(BattlePlayback.Speed.NORMAL, playback.speed());
        playback.faster();
        assertEquals(BattlePlayback.Speed.FAST, playback.speed());
        playback.faster();
        assertEquals(BattlePlayback.Speed.INSTANT, playback.speed());
        playback.faster();
        assertEquals(BattlePlayback.Speed.INSTANT, playback.speed());

        playback.slower();
        assertEquals(BattlePlayback.Speed.FAST, playback.speed());
        playback.slower();
        assertEquals(BattlePlayback.Speed.NORMAL, playback.speed());
        playback.slower();
        assertEquals(BattlePlayback.Speed.NORMAL, playback.speed());
    }

    @Test
    void instantModeAppliesWholeQueueInOrderWithoutWaitingForIntervals() {
        BattlePlayback playback = new BattlePlayback(
            BattlePresentationModel.from(snapshot()), 0.18f);
        playback.setSpeed(BattlePlayback.Speed.INSTANT);
        playback.enqueue(List.of(
            new BattleEvent.PieceMoved("rook-1", Position.fromNotation("e4"), Position.fromNotation("e5")),
            new BattleEvent.PieceMoved("rook-1", Position.fromNotation("e5"), Position.fromNotation("e6"))
        ));

        assertEquals(2, playback.update(0f));
        assertEquals(BoardPosition.fromNotation("e6"),
            playback.model().piece("rook-1").orElseThrow().position());
        assertFalse(playback.isPlaying());
    }

    @Test
    void rejectsInvalidDeltaAndInvalidInterval() {
        assertThrows(IllegalArgumentException.class,
            () -> new BattlePlayback(BattlePresentationModel.from(snapshot()), 0f));
        BattlePlayback playback = new BattlePlayback(
            BattlePresentationModel.from(snapshot()), 0.1f);
        assertThrows(IllegalArgumentException.class, () -> playback.update(-0.1f));
    }

    private static BattleViewSnapshot snapshot() {
        BoardPosition knightPos = BoardPosition.fromNotation("d4");
        PresentationPiece knight = new PresentationPiece("knight", PieceType.KNIGHT, 5, knightPos);
        PresentationPiece rook = new PresentationPiece(
            "rook-1", PieceType.ROOK, 3, BoardPosition.fromNotation("e4"));
        return new BattleViewSnapshot(
            1, knightPos, true, Set.of(), Map.of(), Set.of(), "Turn 1", false,
            Map.of("knight", knight, "rook-1", rook)
        );
    }
}
