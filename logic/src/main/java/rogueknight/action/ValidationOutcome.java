package rogueknight.action;

import java.util.Objects;

/*
 * ValidationOutcome.java  |  T-02.01
 * Kết quả validate nội bộ: hoặc có plan, hoặc có error, không bao giờ cả hai.
 */
record ValidationOutcome(MovePlan plan, ActionError error) {

    ValidationOutcome {
        if ((plan == null) == (error == null)) {
            throw new IllegalArgumentException("exactly one of plan or error is required");
        }
    }

    static ValidationOutcome valid(MovePlan plan) {
        return new ValidationOutcome(Objects.requireNonNull(plan), null);
    }

    static ValidationOutcome invalid(ActionError error) {
        return new ValidationOutcome(null, Objects.requireNonNull(error));
    }

    boolean isValid() {
        return plan != null;
    }
}
