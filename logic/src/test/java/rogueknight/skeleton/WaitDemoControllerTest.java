package rogueknight.skeleton;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitDemoControllerTest {
    /** Verifies that each accepted WAIT advances exactly one turn. */
    @Test
    void waitAdvancesOneTurnPerRequest() {
        var controller = new WaitDemoController();
        var request = new WaitDemoController.Request("knight", "WAIT");

        var first = controller.submit(request);
        var second = controller.submit(request);

        assertTrue(first.accepted());
        assertNull(first.error());
        assertEquals(1, first.before().globalTurn());
        assertEquals(2, first.after().globalTurn());
        assertEquals(2, second.before().globalTurn());
        assertEquals(3, second.after().globalTurn());
    }

    /** Verifies that invalid requests neither advance nor otherwise change state. */
    @Test
    void rejectedRequestsPreserveState() {
        var controller = new WaitDemoController();
        var before = controller.snapshot();

        var requests = new WaitDemoController.Request[] {
            null,
            new WaitDemoController.Request(null, "WAIT"),
            new WaitDemoController.Request("knight", null),
            new WaitDemoController.Request("enemy", "WAIT"),
            new WaitDemoController.Request("knight", "MOVE")
        };
        var errors = new WaitDemoController.Error[] {
            WaitDemoController.Error.INVALID_PAYLOAD,
            WaitDemoController.Error.INVALID_PAYLOAD,
            WaitDemoController.Error.INVALID_PAYLOAD,
            WaitDemoController.Error.UNKNOWN_ACTOR,
            WaitDemoController.Error.UNSUPPORTED_ACTION
        };

        for (int i = 0; i < requests.length; i++) {
            var result = controller.submit(requests[i]);
            assertFalse(result.accepted());
            assertEquals(errors[i], result.error());
            assertEquals(before, result.before());
            assertEquals(before, result.after());
            assertEquals(before, controller.snapshot());
        }
    }

    /** Verifies that a retained snapshot stays unchanged after another action. */
    @Test
    void previousSnapshotRemainsUnchanged() {
        var controller = new WaitDemoController();
        var saved = controller.snapshot();

        controller.submit(new WaitDemoController.Request("knight", "WAIT"));

        assertEquals(1, saved.globalTurn());
        assertEquals(2, controller.snapshot().globalTurn());
    }
}
