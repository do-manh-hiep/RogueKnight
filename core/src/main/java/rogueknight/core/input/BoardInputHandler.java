package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;
import rogueknight.core.view.BoardProjection;

import java.util.Objects;
import java.util.Optional;

/**
 * Handles user clicks in world coordinates, translates them into board actions,
 * submits move requests to the controller port, and updates the local UI feedback state.
 * Fully decoupled from graphics/rendering to enable headless JUnit tests.
 */
public final class BoardInputHandler {
    private final BoardProjection projection;
    private final BattleControllerPort controller;

    private BattleViewSnapshot currentSnapshot;
    private BoardPosition selectedPosition;
    private BoardPosition lastClickedPosition;
    private String feedbackMessage;
    private int submitInvocationCount = 0;

    public BoardInputHandler(BoardProjection projection, BattleControllerPort controller) {
        this.projection = Objects.requireNonNull(projection, "projection must not be null");
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
        this.currentSnapshot = controller.snapshot();
        this.selectedPosition = currentSnapshot.knightPosition();
        this.feedbackMessage = "Click một ô highlight xanh để di chuyển Knight.";
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
            this.lastClickedPosition = null;
            this.feedbackMessage = "Click ngoài bàn cờ";
            return false;
        }

        BoardPosition target = clickedPosOpt.get();
        this.lastClickedPosition = target;

        // If clicking on the Knight itself, re-select
        if (target.equals(currentSnapshot.knightPosition())) {
            this.selectedPosition = target;
            this.feedbackMessage = "Đã chọn Knight tại " + target.notation();
            return true;
        }

        // Submit move request to controller
        submitInvocationCount++;
        MoveResult result = controller.submitMove("knight", target);

        if (result.accepted()) {
            this.currentSnapshot = result.after();
            this.selectedPosition = currentSnapshot.knightPosition();
            this.feedbackMessage = "Di chuyển Knight: " + result.before().knightPosition().notation()
                + " -> " + target.notation() + " (Turn " + currentSnapshot.globalTurn() + ")";
        } else {
            // Rejected: DO NOT update snapshot, preserve previous state
            this.currentSnapshot = result.before();
            this.feedbackMessage = "Nước đi không hợp lệ tới " + target.notation()
                + " [" + result.errorReason() + "]";
        }

        return true;
    }

    public BattleViewSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    public BoardPosition getSelectedPosition() {
        return selectedPosition;
    }

    public BoardPosition getLastClickedPosition() {
        return lastClickedPosition;
    }

    public String getFeedbackMessage() {
        return feedbackMessage;
    }

    public int getSubmitInvocationCount() {
        return submitInvocationCount;
    }

    public BoardProjection getProjection() {
        return projection;
    }

    public BattleControllerPort getController() {
        return controller;
    }
}
