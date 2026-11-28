package rogueknight.core.input;

import rogueknight.board.Position;
import rogueknight.action.BattleEvent;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Handles board input and delegates every gameplay decision to the controller port. */
public final class BoardInputHandler {
    private final BoardProjection projection;
    private final BattleControllerPort controller;
    private BattleViewSnapshot currentSnapshot;
    private String feedbackMessage;
    private final List<BattleEvent> pendingEvents = new ArrayList<>();
    private ActionMode mode = ActionMode.MOVE;

    public BoardInputHandler(BoardProjection projection, BattleControllerPort controller) {
        this.projection = Objects.requireNonNull(projection, "projection must not be null");
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
        currentSnapshot = controller.snapshot();
        feedbackMessage = "[1] Move / Attack | [0] Wait";
    }

    public boolean handleWorldClick(float worldX, float worldY) {
        Optional<Position> clicked = projection.worldToBoard(worldX, worldY);
        if (clicked.isEmpty()) {
            feedbackMessage = "Click ngoài bàn cờ";
            return false;
        }

        Position target = clicked.get();
        if (target.equals(currentSnapshot.knightPosition())) {
            feedbackMessage = "Đã chọn Knight tại " + target.toNotation();
            return true;
        }

        boolean attackTarget = mode == ActionMode.MOVE && currentSnapshot.hasEnemyAt(target);
        UiActionResult result = controller.submitAction("knight", mode, target);
        if (result.accepted()) {
            currentSnapshot = result.after();
            pendingEvents.addAll(result.events());
            String label = mode == ActionMode.MOVE
                ? (attackTarget ? "Sword Attack" : "Di chuyển Knight")
                : "Dùng " + mode.label();
            feedbackMessage = label + ": " + result.before().knightPosition().toNotation()
                + " -> " + target.toNotation() + " (Turn " + currentSnapshot.globalTurn() + ")";
            mode = ActionMode.MOVE;
        } else {
            currentSnapshot = result.before();
            feedbackMessage = "Nước đi không hợp lệ tới " + target.toNotation()
                + " [" + result.errorReason() + "]";
        }
        return true;
    }

    /** Wait executes immediately; selecting an active equipment action again returns to Move. */
    public void selectMode(ActionMode newMode) {
        if (newMode == null) {
            mode = ActionMode.MOVE;
            feedbackMessage = "Chế độ: " + mode.label();
            return;
        }
        List<ActionMode> availableModes = getAvailableModes();
        if (!availableModes.contains(newMode)) {
            feedbackMessage = "Action không khả dụng: " + newMode.label();
            return;
        }
        if (newMode == ActionMode.WAIT) {
            UiActionResult result = controller.submitWait("knight");
            if (result.accepted()) {
                currentSnapshot = result.after();
                pendingEvents.addAll(result.events());
                feedbackMessage = "Wait: Turn " + currentSnapshot.globalTurn();
            } else {
                currentSnapshot = result.before();
                feedbackMessage = "Wait không hợp lệ [" + result.errorReason() + "]";
            }
            mode = ActionMode.MOVE;
            return;
        }
        mode = newMode == mode ? ActionMode.MOVE : newMode;
        feedbackMessage = "Chế độ: " + mode.label();
    }

    /** Selects an action by its current displayed/keyboard index. */
    public void selectAction(int index) {
        List<ActionMode> availableModes = getAvailableModes();
        if (index < 0 || index >= availableModes.size()) {
            feedbackMessage = "Action [" + index + "] không khả dụng";
            return;
        }
        selectMode(availableModes.get(index));
    }

    public List<ActionMode> getAvailableModes() {
        return controller.availableModes();
    }

    public int getActionIndex(ActionMode action) {
        return getAvailableModes().indexOf(action);
    }

    public ActionMode getMode() {
        return mode;
    }

    public Set<Position> getHighlightTargets() {
        if (mode == ActionMode.WAIT) return Set.of();
        return controller.legalTargets("knight", mode);
    }

    public BattleViewSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    /** Returns and clears ordered domain events accumulated since the previous drain. */
    public List<BattleEvent> drainEvents() {
        List<BattleEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    public String getFeedbackMessage() {
        return feedbackMessage;
    }

    public BoardProjection getProjection() {
        return projection;
    }
}
