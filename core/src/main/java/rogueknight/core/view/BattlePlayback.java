package rogueknight.core.view;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import rogueknight.action.BattleEvent;

/**
 * Frame-driven, deterministic playback of the ordered domain event stream.
 * This class has no libGDX dependency and can be tested headlessly.
 */
public final class BattlePlayback {
    public enum Speed {
        NORMAL("Normal", 0.18f),
        FAST("Fast", 0.08f),
        INSTANT("Instant", 0f);

        private final String label;
        private final float secondsPerEvent;

        Speed(String label, float secondsPerEvent) {
            this.label = label;
            this.secondsPerEvent = secondsPerEvent;
        }

        public String label() {
            return label;
        }
    }

    private final BattlePresentationModel model;
    private final ArrayDeque<BattleEvent> events = new ArrayDeque<>();
    private Speed speed = Speed.NORMAL;
    private float elapsed;
    private final float customNormalInterval;

    public BattlePlayback(BattlePresentationModel model, float secondsPerEvent) {
        this.model = Objects.requireNonNull(model, "model");
        if (!Float.isFinite(secondsPerEvent) || secondsPerEvent <= 0f) {
            throw new IllegalArgumentException("secondsPerEvent must be finite and positive");
        }
        // Keep the constructor useful for deterministic tests/custom playback intervals.
        this.customNormalInterval = secondsPerEvent;
    }

    public BattlePresentationModel model() {
        return model;
    }

    public Speed speed() {
        return speed;
    }

    public String speedLabel() {
        return speed.label();
    }

    public void faster() {
        setSpeed(switch (speed) {
            case NORMAL -> Speed.FAST;
            case FAST, INSTANT -> Speed.INSTANT;
        });
    }

    public void slower() {
        setSpeed(switch (speed) {
            case NORMAL, FAST -> Speed.NORMAL;
            case INSTANT -> Speed.FAST;
        });
    }

    public void setSpeed(Speed newSpeed) {
        speed = Objects.requireNonNull(newSpeed, "newSpeed");
        elapsed = 0f;
    }

    public void enqueue(Collection<? extends BattleEvent> orderedEvents) {
        // Validate/copy the whole batch before mutation, so null cannot partially enqueue it.
        events.addAll(List.copyOf(Objects.requireNonNull(orderedEvents, "orderedEvents")));
    }

    /**
     * Advances playback by frame delta and applies every event whose time slot elapsed.
     * Instant mode applies the entire pending event stream in this frame, without
     * skipping domain events or changing their order.
     *
     * @return the number of events applied during this call
     */
    public int update(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        if (events.isEmpty()) {
            elapsed = 0f;
            return 0;
        }

        if (speed == Speed.INSTANT) {
            int applied = 0;
            while (!events.isEmpty()) {
                model.apply(events.removeFirst());
                applied++;
            }
            elapsed = 0f;
            return applied;
        }

        elapsed += deltaSeconds;
        int applied = 0;
        float secondsPerEvent = speed == Speed.NORMAL ? customNormalInterval : Speed.FAST.secondsPerEvent;
        while (!events.isEmpty() && elapsed >= secondsPerEvent) {
            model.apply(events.removeFirst());
            elapsed -= secondsPerEvent;
            applied++;
        }
        if (events.isEmpty()) elapsed = 0f;
        return applied;
    }

    /** Discards pending presentation events after a fatal runtime error. Domain state is untouched. */
    public void stop() {
        events.clear();
        elapsed = 0f;
    }

    public boolean isPlaying() {
        return !events.isEmpty();
    }

    public int pendingEventCount() {
        return events.size();
    }
}
