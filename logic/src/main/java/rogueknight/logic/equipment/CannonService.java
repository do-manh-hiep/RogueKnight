package rogueknight.logic.equipment;

import java.util.ArrayList;
import java.util.List;

public class CannonService {

    public enum MoveKind {
        MOVE,
        CAPTURE
    }

    public record CannonMoveOption(
        GridPos target,
        MoveKind kind,
        EquipmentBoardView.EnemyInfo targetEnemy
    ) {}

    public record CannonAttackOption(
        GridPos target,
        EquipmentBoardView.EnemyInfo targetEnemy
    ) {}

    public record CannonActionResult(
        boolean valid,
        String reason,
        GridPos newKnightPos,
        EquipmentBoardView.EnemyInfo hitEnemy,
        int damageDealt,
        boolean enemyDefeated,
        boolean battleWon,
        int remainingCharge
    ) {
        public static CannonActionResult invalid(String reason, GridPos currentPos, int charge) {
            return new CannonActionResult(false, reason, currentPos, null, 0, false, false, charge);
        }
    }

    private final boolean allowMoveWithoutTerminalBlocker;

    public CannonService() {
        this(true);
    }

    public CannonService(boolean allowMoveWithoutTerminalBlocker) {
        this.allowMoveWithoutTerminalBlocker = allowMoveWithoutTerminalBlocker;
    }

    /**
     * T-04.01: Sinh danh sách các nước đi / capture hợp lệ của Cannon.
     */
    public List<CannonMoveOption> getLegalMoveOptions(EquipmentBoardView board, int currentCharge) {
        if (currentCharge <= 0) {
            return List.of();
        }
        List<CannonMoveOption> options = new ArrayList<>();
        for (CannonRayScanner.RayResult ray : CannonRayScanner.scanAllDirections(board)) {
            if (!ray.hasScreen()) {
                continue;
            }
            if (ray.hasTerminalBlocker() || allowMoveWithoutTerminalBlocker) {
                for (GridPos emptyPos : ray.emptySquaresAfterScreen()) {
                    options.add(new CannonMoveOption(emptyPos, MoveKind.MOVE, null));
                }
            }
            if (ray.hasTerminalEnemy() && ray.terminalEnemy().hp() <= 2) {
                options.add(new CannonMoveOption(
                    ray.terminalBlockerPos(),
                    MoveKind.CAPTURE,
                    ray.terminalEnemy()
                ));
            }
        }
        return List.copyOf(options);
    }

    /**
     * T-04.01: Thực thi Cannon Movement (di chuyển hoặc capture Enemy HP <= 2, tốn 1 charge).
     */
    public CannonActionResult executeCannonMove(
        EquipmentBoardView board,
        int currentCharge,
        GridPos destination
    ) {
        if (currentCharge <= 0) {
            return CannonActionResult.invalid("Cannon has 0 charge", board.getKnightPosition(), currentCharge);
        }
        for (CannonMoveOption option : getLegalMoveOptions(board, currentCharge)) {
            if (option.target().equals(destination)) {
                int nextCharge = currentCharge - 1;
                if (option.kind() == MoveKind.MOVE) {
                    return new CannonActionResult(
                        true, "OK", destination, null, 0, false, false, nextCharge
                    );
                }
                EquipmentBoardView.EnemyInfo enemy = option.targetEnemy();
                return new CannonActionResult(
                    true, "OK", destination, enemy, enemy.hp(), true, enemy.isKing(), nextCharge
                );
            }
        }
        return CannonActionResult.invalid("Illegal Cannon move target", board.getKnightPosition(), currentCharge);
    }

    /**
     * T-04.02a: Sinh danh sách mục tiêu hợp lệ của Cannon Attack (Enemy đứng sau đúng 1 screen).
     */
    public List<CannonAttackOption> getLegalAttackOptions(EquipmentBoardView board, int currentCharge) {
        if (currentCharge <= 0) {
            return List.of();
        }
        List<CannonAttackOption> options = new ArrayList<>();
        for (CannonRayScanner.RayResult ray : CannonRayScanner.scanAllDirections(board)) {
            if (ray.hasScreen() && ray.hasTerminalEnemy()) {
                options.add(new CannonAttackOption(ray.terminalBlockerPos(), ray.terminalEnemy()));
            }
        }
        return List.copyOf(options);
    }

    /**
     * T-04.02a: Thực thi Cannon Attack (đứng yên, gây 1 damage vào terminal Enemy, tốn 1 charge).
     */
    public CannonActionResult executeCannonAttack(
        EquipmentBoardView board,
        int currentCharge,
        GridPos targetPos
    ) {
        GridPos knightPos = board.getKnightPosition();
        if (currentCharge <= 0) {
            return CannonActionResult.invalid("Cannon has 0 charge", knightPos, currentCharge);
        }
        for (CannonAttackOption option : getLegalAttackOptions(board, currentCharge)) {
            if (option.target().equals(targetPos)) {
                EquipmentBoardView.EnemyInfo enemy = option.targetEnemy();
                int remainingHp = enemy.hp() - 1;
                boolean defeated = remainingHp <= 0;
                boolean battleWon = defeated && enemy.isKing();
                return new CannonActionResult(
                    true, "OK", knightPos, enemy, 1, defeated, battleWon, currentCharge - 1
                );
            }
        }
        return CannonActionResult.invalid("Illegal Cannon attack target", knightPos, currentCharge);
    }
}