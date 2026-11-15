package com.rogueknight.game.logic;

public class Enemy {
    private final String id;
    private boolean alive;

    public Enemy(String id) {
        this.id = id;
        this.alive = true;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * Lấy hành động của quái trong lượt này (AI logic)
     */
    public Action takeTurn() {
        return new WaitAction(id);
    }
}