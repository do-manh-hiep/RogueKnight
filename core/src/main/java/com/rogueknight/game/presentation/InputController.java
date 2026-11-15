package com.rogueknight.game.presentation;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.rogueknight.game.logic.ActionResult;
import com.rogueknight.game.logic.TurnCoordinator;
import com.rogueknight.game.logic.WaitAction;

public class InputController extends InputAdapter {
    private final TurnCoordinator turnCoordinator;

    public InputController(TurnCoordinator turnCoordinator) {
        this.turnCoordinator = turnCoordinator;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.SPACE || keycode == Input.Keys.W) {
            ActionResult result = turnCoordinator.executePlayerAction(new WaitAction("Player"));
            System.out.println("[LOG] " + result.getMessage() + " -> Turn hien tai: " + turnCoordinator.getTurnNumber());
            return true;
        }
        return false;
    }
}