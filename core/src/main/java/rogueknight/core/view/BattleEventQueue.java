package rogueknight.core.view;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.action.BattleEvent;

/**
 * FIFO queue for events awaiting presentation playback.
 *
 * <p>The queue preserves the domain-emitted order exactly. It does not infer
 * actions from activation events or alter event semantics.</p>
 */
public final class BattleEventQueue {
    private final ArrayDeque<BattleEvent> events = new ArrayDeque<>();

    public void enqueue(BattleEvent event) {
        events.addLast(Objects.requireNonNull(event, "event"));
    }

    public void enqueueAll(Collection<? extends BattleEvent> orderedEvents) {
        Objects.requireNonNull(orderedEvents, "orderedEvents");
        // Copy first so a failed/null-containing batch cannot partially mutate the queue.
        List<BattleEvent> copy = List.copyOf(orderedEvents);
        events.addAll(copy);
    }

    public Optional<BattleEvent> poll() {
        return Optional.ofNullable(events.pollFirst());
    }

    public Optional<BattleEvent> peek() {
        return Optional.ofNullable(events.peekFirst());
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public void clear() {
        events.clear();
    }
}
