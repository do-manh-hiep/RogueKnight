package com.rogueknight.game.logic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TurnCoordinatorTest {

    private ActionResolver resolver;
    private EnemyQueue enemyQueue;
    private TurnCoordinator coordinator;

    @BeforeEach
    void setUp() {
        resolver = new ActionResolver();
        enemyQueue = new EnemyQueue();
        coordinator = new TurnCoordinator(resolver, enemyQueue);
    }

    @Test
    void testWaitActionIncrementsTurnAndProcessesEnemyQueue() {
        Enemy goblin = new Enemy("Goblin_1");
        Enemy orc = new Enemy("Orc_1");
        enemyQueue.addEnemy(goblin);
        enemyQueue.addEnemy(orc);

        Assertions.assertEquals(1, coordinator.getTurnNumber());
        Assertions.assertEquals(2, enemyQueue.size());

        WaitAction waitAction = new WaitAction("Player");
        ActionResult result = coordinator.executePlayerAction(waitAction);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, coordinator.getTurnNumber());
    }

    @Test
    void testDeadEnemyRemovedFromQueue() {
        Enemy goblin = new Enemy("Goblin_1");
        Enemy deadSkeleton = new Enemy("Skeleton_Dead");
        deadSkeleton.setAlive(false);

        enemyQueue.addEnemy(goblin);
        enemyQueue.addEnemy(deadSkeleton);

        coordinator.executePlayerAction(new WaitAction("Player"));

        Assertions.assertEquals(1, enemyQueue.size());
        Assertions.assertEquals("Goblin_1", enemyQueue.getActiveEnemies().get(0).getId());
    }
}