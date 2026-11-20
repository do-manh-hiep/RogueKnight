package rogueknight.action;

import java.util.Objects;

/** Internal validation result: exactly one of plan or error is present. */
record ValidationOutcome(ActionPlan plan, ActionError error) {
    ValidationOutcome {
        if ((plan == null) == (error == null)) {
            throw new IllegalArgumentException("exactly one of plan or error is required");
        }
    }

    static ValidationOutcome valid(ActionPlan plan) {
        return new ValidationOutcome(Objects.requireNonNull(plan), null);
    }

    static ValidationOutcome invalid(ActionError error) {
        return new ValidationOutcome(null, Objects.requireNonNull(error));
    }

    boolean isValid() {
        return plan != null;
    }
}
