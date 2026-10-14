package com.rogueknight.game.logic;

public class TurnCoordinator {
    private int turnNumber;
    private final ActionResolver actionResolver;
    private final EnemyQueue enemyQueue;

    public TurnCoordinator(ActionResolver actionResolver, EnemyQueue enemyQueue) {
        this.turnNumber = 1;
        this.actionResolver = actionResolver;
        this.enemyQueue = enemyQueue;
    }

    /**
     * Xử lý trọn vẹn một vòng lặp lượt:
     * 1. Player hành động -> 2. Toàn bộ Enemy trong Queue hành động -> 3. Tăng Turn chung
     */
    public ActionResult executePlayerAction(Action action) {
        // 1. Thẩm định và thực thi lượt người chơi qua ActionResolver
        ActionResult playerResult = actionResolver.resolve(action);
        if (!playerResult.isSuccess()) {
            return playerResult; // Hành động thất bại, không tiêu tốn lượt
        }

        // 2. Kích hoạt lượt của quái vật theo EnemyQueue
        for (Enemy enemy : enemyQueue.getActiveEnemies()) {
            if (enemy.isAlive()) {
                Action enemyAction = enemy.takeTurn();
                actionResolver.resolve(enemyAction);
            }
        }

        // 3. Dọn quái đã chết ra khỏi hàng đợi
        enemyQueue.removeDeadEnemies();

        // 4. Kết thúc vòng lặp, tăng lượt
        turnNumber++;

        return playerResult;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public EnemyQueue getEnemyQueue() {
        return enemyQueue;
    }
}