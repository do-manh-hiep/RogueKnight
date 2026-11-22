package rogueknight.action;

import java.util.Objects;

/*
 * EnemyDecision.java  |  T-03.01/T-03.02  |  TD: 4.5 (EnemyDecision), 4.7 (C-03)
 * Một bước quyết định của AI: một Action request, hoặc Skip kèm lý do.
 * Request vẫn phải qua EnemyRules.validate trước khi Resolver áp dụng.
 */
public sealed interface EnemyDecision {

    record Act(ActionRequest request) implements EnemyDecision {
        public Act {
            Objects.requireNonNull(request, "request");
        }
    }

    record Skip(String reason) implements EnemyDecision {
        public Skip {
            Objects.requireNonNull(reason, "reason");
        }
    }

    static EnemyDecision act(ActionRequest request) {
        return new Act(request);
    }

    static EnemyDecision skip(String reason) {
        return new Skip(reason);
    }
}
