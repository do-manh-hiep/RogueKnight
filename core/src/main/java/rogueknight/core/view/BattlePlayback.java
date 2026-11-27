package rogueknight.core.view;

import java.util.Collection;
import java.util.Objects;
import rogueknight.action.BattleEvent;

/**
 * Frame-driven, deterministic playback of the ordered domain event stream.
 * This class has no libGDX dependency and can be tested headlessly.
 */
public final class BattlePlayback {
    private final BattlePresentationModel model;
    private final BattleEventQueue queue = new BattleEventQueue();
    private final float secondsPerEvent;
    private float elapsed;

    public BattlePlayback(BattlePresentationModel model, float secondsPerEvent) {
        this.model = Objects.requireNonNull(model, "model");
        if (!Float.isFinite(secondsPerEvent) || secondsPerEvent <= 0f) {
            throw new IllegalArgumentException("secondsPerEvent must be finite and positive");
        }
        this.secondsPerEvent = secondsPerEvent;
    }

    public BattlePresentationModel model() {
        return model;
    }

    public void enqueue(Collection<? extends BattleEvent> events) {
        queue.enqueueAll(events);
    }

    /**
     * Advances playback by frame delta and applies every event whose time slot elapsed.
     * Returns the number of events applied during this call.
     */
    public int update(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        if (queue.isEmpty()) {
            elapsed = 0f;
            return 0;
        }

        elapsed += deltaSeconds;
        int applied = 0;
        while (!queue.isEmpty() && elapsed >= secondsPerEvent) {
            BattleEvent event = queue.poll().orElseThrow();
            model.apply(event);
            elapsed -= secondsPerEvent;
            applied++;
        }
        if (queue.isEmpty()) elapsed = 0f;
        return applied;
    }

    public boolean isPlaying() {
        return !queue.isEmpty();
    }

    public int pendingEventCount() {
        return queue.size();
    }
}
