package rogueknight.core.view;

import org.junit.jupiter.api.Test;
import rogueknight.action.ActionKind;
import rogueknight.action.BattleEvent;
import rogueknight.battle.BattleResult;
import rogueknight.board.Position;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleEventQueueTest {

    @Test
    void preservesSubmittedEventOrderAcrossMultipleEnemyActivations() {
        BattleEventQueue queue = new BattleEventQueue();
        List<BattleEvent> events = List.of(
            new BattleEvent.TurnAdvanced(1, 2),
            new BattleEvent.ActorActivated("rook-a", Position.fromNotation("a4"), 2),
            new BattleEvent.ActionPerformed(ActionKind.MOVE, "rook-a"),
            new BattleEvent.PieceMoved("rook-a", Position.fromNotation("a4"), Position.fromNotation("a5")),
            // An activation with no following action represents a skipped activation.
            new BattleEvent.ActorActivated("bishop-b", Position.fromNotation("c3"), 2),
            new BattleEvent.ActorActivated("king-c", Position.fromNotation("h8"), 2),
            new BattleEvent.ActionPerformed(ActionKind.ATTACK, "king-c"),
            new BattleEvent.PieceDamaged("knight", 1, 4)
        );

        queue.enqueueAll(events);

        for (BattleEvent expected : events) {
            assertEquals(expected, queue.poll().orElseThrow());
        }
        assertTrue(queue.isEmpty());
    }

    @Test
    void acceptsKnightDeathSequenceAndBattleEndWithoutReordering() {
        BattleEventQueue queue = new BattleEventQueue();
        List<BattleEvent> events = List.of(
            new BattleEvent.ActorActivated("rook", Position.fromNotation("e4"), 3),
            new BattleEvent.ActionPerformed(ActionKind.ATTACK, "rook"),
            new BattleEvent.PieceDamaged("knight", 1, 0),
            new BattleEvent.PieceRemoved("knight", Position.fromNotation("d4")),
            new BattleEvent.BattleEnded(BattleResult.DEFEAT)
        );

        queue.enqueueAll(events);

        assertEquals(events, List.of(
            queue.poll().orElseThrow(),
            queue.poll().orElseThrow(),
            queue.poll().orElseThrow(),
            queue.poll().orElseThrow(),
            queue.poll().orElseThrow()
        ));
        assertTrue(queue.poll().isEmpty());
    }

    @Test
    void pollAndPeekDoNotReorderOrDuplicateEvents() {
        BattleEventQueue queue = new BattleEventQueue();
        BattleEvent event = new BattleEvent.ActorActivated(
            "pawn", Position.fromNotation("d7"), 2);

        queue.enqueue(event);

        assertEquals(event, queue.peek().orElseThrow());
        assertEquals(event, queue.peek().orElseThrow());
        assertEquals(1, queue.size());
        assertEquals(event, queue.poll().orElseThrow());
        assertTrue(queue.poll().isEmpty());
    }

    @Test
    void enqueueAllRejectsNullBatchWithoutPartiallyAppending() {
        BattleEventQueue queue = new BattleEventQueue();
        queue.enqueue(new BattleEvent.TurnAdvanced(1, 2));

        assertThrows(NullPointerException.class,
            () -> queue.enqueueAll(java.util.Arrays.asList(
                new BattleEvent.TurnAdvanced(2, 3), null)));
        assertEquals(1, queue.size());
        assertEquals(new BattleEvent.TurnAdvanced(1, 2), queue.poll().orElseThrow());
    }
}
