package rogueknight.action;

import java.util.List;
import java.util.Objects;
import rogueknight.battle.BattleSnapshot;

/*
 * ActionResult.java  |  T-02.01  |  TD: 4.6, 4.10, DEC-04
 *
 * Kết quả một lần submit.
 * - accepted=true: error null, after là snapshot sau toàn bộ xử lý, events theo thứ tự.
 * - accepted=false: error khác null, after == before, events rỗng.
 */
public record ActionResult(boolean accepted, ActionError error, BattleSnapshot before,
                           BattleSnapshot after, List<BattleEvent> events) {

    public ActionResult {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        events = List.copyOf(events);
        if (accepted == (error != null)) {
            throw new IllegalArgumentException("accepted results have no error; rejected results must have one");
        }
    }

    static ActionResult accepted(BattleSnapshot before, BattleSnapshot after, List<BattleEvent> events) {
        return new ActionResult(true, null, before, after, events);
    }

    static ActionResult rejected(ActionError error, BattleSnapshot before) {
        return new ActionResult(false, Objects.requireNonNull(error, "error"), before, before, List.of());
    }
}
