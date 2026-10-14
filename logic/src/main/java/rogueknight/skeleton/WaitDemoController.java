package rogueknight.skeleton;

/**
 * Demonstrates the input-to-logic boundary for WAIT.
 * This is not a production battle controller: it has no board or enemy queue.
 */
public final class WaitDemoController {
    public record Request(String actorId, String kind) {}

    public record Snapshot(int globalTurn) {}

    public enum Error {
        INVALID_PAYLOAD,
        UNKNOWN_ACTOR,
        UNSUPPORTED_ACTION
    }

    public record Result(
        boolean accepted,
        Error error,
        Snapshot before,
        Snapshot after
    ) {}

    private int globalTurn = 1;

    /** Returns an immutable view of the demo's current turn. */
    public Snapshot snapshot() {
        return new Snapshot(globalTurn);
    }

    /**
     * Accepts only a Knight WAIT request and advances the demo turn once.
     * Rejected requests leave the demo state unchanged.
     *
     * @param request request supplied by the caller; may be null
     * @return acceptance status, error if rejected, and before/after snapshots
     */
    public Result submit(Request request) {
        var before = snapshot();

        if (request == null || request.actorId() == null || request.kind() == null) {
            return rejected(Error.INVALID_PAYLOAD, before);
        }
        if (!"knight".equals(request.actorId())) {
            return rejected(Error.UNKNOWN_ACTOR, before);
        }
        if (!"WAIT".equals(request.kind())) {
            return rejected(Error.UNSUPPORTED_ACTION, before);
        }

        globalTurn++;
        return new Result(true, null, before, snapshot());
    }

    private Result rejected(Error error, Snapshot before) {
        return new Result(false, error, before, before);
    }
}
