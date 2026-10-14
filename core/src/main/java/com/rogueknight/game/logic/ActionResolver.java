package com.rogueknight.game.logic;

public class ActionResolver {

    public ActionResult resolve(Action action) {
        if (action == null) {
            return ActionResult.failure("Hành động không hợp lệ!");
        }

        if (action instanceof WaitAction) {
            return ActionResult.success(action.getActorId() + " đã chờ qua lượt.");
        }

        return ActionResult.failure("Hành động chưa được hỗ trợ!");
    }
}