package com.rogueknight.game.logic;

public class WaitAction implements Action {
    private final String actorId;

    public WaitAction(String actorId) {
        this.actorId = actorId;
    }

    @Override
    public String getActorId() {
        return actorId;
    }
}