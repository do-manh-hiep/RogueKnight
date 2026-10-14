package com.rogueknight.game.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnemyQueue {
    private final List<Enemy> enemies = new ArrayList<>();

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void removeDeadEnemies() {
        enemies.removeIf(enemy -> !enemy.isAlive());
    }

    public List<Enemy> getActiveEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public int size() {
        return enemies.size();
    }
}