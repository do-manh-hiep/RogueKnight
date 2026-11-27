package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import rogueknight.core.view.BoardProjection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Handles user clicks in world coordinates, translates them into board actions,
 * submits requests to the controller port, and maintains UI feedback state.
 * Fully decoupled from graphics/rendering to enable headless JUnit tests.
 */
public final class BoardInputHandler {
    private final BoardProjection projection;
    private final BattleControllerPort controller;

    private BattleViewSnapshot currentSnapshot;
    private String feedbackMessage;
    private ActionMode mode = ActionMode.MOVE;

    public BoardInputHandler(BoardProjection projection, BattleControllerPort controller) {
        this.projection = Objects.requireNonNull(projection, "projection must not be null");
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
        this.currentSnapshot = controller.snapshot();
        this.feedbackMessage = "[1] Move / Attack | [0] Wait";
    }

    /**
     * Processes a mouse click at the given world coordinates (x, y).
     *
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @return true if the click was inside the board; false if out of bounds
     */
    public boolean handleWorldClick(float worldX, float worldY) {
        Optional<BoardPosition> clickedPosOpt = projection.worldToBoard(worldX, worldY);

        if (clickedPosOpt.isEmpty()) {
            this.feedbackMessage = "Click ngoài bàn cờ";
            return false;
        }

        BoardPosition target = clickedPosOpt.get();

        // Clicking the Knight only refreshes the selection feedback; its position is in the snapshot.
        if (target.equals(currentSnapshot.knightPosition())) {
            this.feedbackMessage = "Đã chọn Knight tại " + target.notation();
            return true;
        }

        boolean attackTarget = mode == ActionMode.MOVE && currentSnapshot.enemyPieces().containsKey(target);
        MoveResult result = controller.submitAction("knight", mode, target);

        if (result.accepted()) {
            this.currentSnapshot = result.after();
            String label = mode == ActionMode.MOVE
                ? (attackTarget ? "Sword Attack" : "Di chuyển Knight")
                : "Dùng " + mode.label();
            this.feedbackMessage = label + ": " + result.before().knightPosition().notation()
                + " -> " + target.notation() + " (Turn " + currentSnapshot.globalTurn() + ")";
            this.mode = ActionMode.MOVE;
        } else {
            // Rejected: DO NOT update to a potentially fabricated state.
            this.currentSnapshot = result.before();
            this.feedbackMessage = "Nước đi không hợp lệ tới " + target.notation()
                + " [" + result.errorReason() + "]";
        }

        return true;
    }

    /**
     * Selects a numbered action. Wait executes immediately; selecting an active
     * equipment action again falls back to the always-available Move/Attack mode.
     */
    public void selectMode(ActionMode newMode) {
        if (newMode == null) {
            this.mode = ActionMode.MOVE;
            this.feedbackMessage = "Chế độ: " + this.mode.label();
            return;
        }
        List<ActionMode> availableModes = getAvailableModes();
        if (!availableModes.contains(newMode)) {
            this.feedbackMessage = "Action không khả dụng: " + newMode.label();
            return;
        }
        if (newMode == ActionMode.WAIT) {
            MoveResult result = controller.submitWait("knight");
            if (result.accepted()) {
                currentSnapshot = result.after();
                feedbackMessage = "Wait: Turn " + currentSnapshot.globalTurn();
            } else {
                currentSnapshot = result.before();
                feedbackMessage = "Wait không hợp lệ [" + result.errorReason() + "]";
            }
            mode = ActionMode.MOVE;
            return;
        }
        this.mode = (newMode == this.mode) ? ActionMode.MOVE : newMode;
        this.feedbackMessage = "Chế độ: " + this.mode.label();
    }

    /**
     * Selects an action by its current displayed/keyboard index. The list is
     * rebuilt from available actions, so exhausted equipment never leaves a gap.
     */
    public void selectAction(int index) {
        List<ActionMode> availableModes = getAvailableModes();
        if (index < 0 || index >= availableModes.size()) {
            feedbackMessage = "Action [" + index + "] không khả dụng";
            return;
        }
        selectMode(availableModes.get(index));
    }

    /** Modes usable now; used-up equipment is absent. */
    public List<ActionMode> getAvailableModes() {
        return controller.availableModes();
    }

    /** Returns the current displayed index for an action, or -1 if unavailable. */
    public int getActionIndex(ActionMode action) {
        return getAvailableModes().indexOf(action);
    }

    public ActionMode getMode() {
        return mode;
    }

    /** Tiles to highlight for the active mode; MOVE uses the snapshot, others query the controller. */
    public Set<BoardPosition> getHighlightTargets() {
        if (mode == ActionMode.WAIT) return Set.of();
        return controller.legalTargets("knight", mode);
    }

    public BattleViewSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    public String getFeedbackMessage() {
        return feedbackMessage;
    }

    public BoardProjection getProjection() {
        return projection;
    }
}
