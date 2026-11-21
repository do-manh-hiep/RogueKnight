package rogueknight.logic.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkewerService {

    public static final int[][] DIAGONAL_DIRS = {
        {1, 1},   // Up-Right
        {1, -1},  // Down-Right
        {-1, 1},  // Up-Left
        {-1, -1}  // Down-Left
    };

    public record EnemyHit(
        GridPos position,
        EquipmentBoardView.EnemyInfo enemy,
        int damage,
        boolean defeated
    ) {}

    public record SkewerResult(
        boolean valid,
        String reason,
        GridPos startPos,
        GridPos finalKnightPos,
        List<GridPos> traversedTiles,
        List<EnemyHit> enemyHits,
        boolean battleWon,
        int remainingCharge
    ) {
        public static SkewerResult invalid(String reason, GridPos startPos, int charge) {
            return new SkewerResult(false, reason, startPos, startPos, List.of(), List.of(), false, charge);
        }
    }

    private final boolean allowEmptySkewer;

    public SkewerService() {
        this(false);
    }

    public SkewerService(boolean allowEmptySkewer) {
        this.allowEmptySkewer = allowEmptySkewer;
    }

    /**
     * T-04.02b: Lướt tối đa 2 ô liên tiếp theo hướng chéo (dCol, dRow), mỗi giá trị thuộc {-1, 1}.
     * Rock hoặc biên bàn cờ chặn đường; Enemy không chặn đường lướt và nhận 1 damage mỗi con.
     */
    public SkewerResult executeSkewer(
        EquipmentBoardView board,
        int currentCharge,
        int dCol,
        int dRow
    ) {
        GridPos start = board.getKnightPosition();
        if (currentCharge <= 0) {
            return SkewerResult.invalid("Skewer has 0 charge", start, currentCharge);
        }
        if (Math.abs(dCol) != 1 || Math.abs(dRow) != 1) {
            return SkewerResult.invalid("Skewer direction must be diagonal", start, currentCharge);
        }

        List<GridPos> traversed = new ArrayList<>(2);
        List<EnemyHit> hits = new ArrayList<>(2);
        GridPos landingPos = start;
        boolean battleWon = false;

        GridPos cursor = start;
        for (int step = 1; step <= 2; step++) {
            cursor = cursor.step(dCol, dRow);
            if (!cursor.isValid() || board.isRockAt(cursor)) {
                break;
            }
            traversed.add(cursor);

            Optional<EquipmentBoardView.EnemyInfo> enemyOpt = board.getEnemyAt(cursor);
            if (enemyOpt.isPresent()) {
                EquipmentBoardView.EnemyInfo enemy = enemyOpt.get();
                boolean defeated = (enemy.hp() - 1) <= 0;
                hits.add(new EnemyHit(cursor, enemy, 1, defeated));
                if (defeated && enemy.isKing()) {
                    battleWon = true;
                }
                if (defeated) {
                    landingPos = cursor;
                }
            } else {
                landingPos = cursor;
            }
        }

        if (traversed.isEmpty()) {
            return SkewerResult.invalid("Skewer path is completely blocked", start, currentCharge);
        }
        if (hits.isEmpty() && !allowEmptySkewer) {
            return SkewerResult.invalid("Empty Skewer is disallowed by rules", start, currentCharge);
        }

        return new SkewerResult(
            true,
            "OK",
            start,
            landingPos,
            List.copyOf(traversed),
            List.copyOf(hits),
            battleWon,
            currentCharge - 1
        );
    }
}