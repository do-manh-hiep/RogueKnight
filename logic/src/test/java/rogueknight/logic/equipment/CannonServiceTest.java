package rogueknight.logic.equipment;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CannonServiceTest {

    static class FakeBoard implements EquipmentBoardView {
        private final GridPos knightPos;
        private final Set<GridPos> rocks = new HashSet<>();
        private final Map<GridPos, EnemyInfo> enemies = new HashMap<>();

        FakeBoard(String knightNotation) {
            this.knightPos = GridPos.fromNotation(knightNotation);
        }

        FakeBoard addRock(String notation) {
            rocks.add(GridPos.fromNotation(notation));
            return this;
        }

        FakeBoard addEnemy(String notation, String id, int hp, boolean isKing) {
            enemies.put(GridPos.fromNotation(notation), new EnemyInfo(id, hp, isKing));
            return this;
        }

        @Override
        public GridPos getKnightPosition() {
            return knightPos;
        }

        @Override
        public boolean isRockAt(GridPos pos) {
            return rocks.contains(pos);
        }

        @Override
        public Optional<EnemyInfo> getEnemyAt(GridPos pos) {
            return Optional.ofNullable(enemies.get(pos));
        }
    }

    @Test
    void map01Fixture_cannonMoveAndCapture_respectsScreenAndTerminalBlocker() {
        // Knight d1, Rock d3 (screen), Pawn d7 (HP 1), King d8
        FakeBoard board = new FakeBoard("d1")
            .addRock("d3")
            .addEnemy("d7", "pawn_1", 1, false)
            .addEnemy("d8", "king", 3, true);

        CannonService service = new CannonService();
        List<CannonService.CannonMoveOption> options = service.getLegalMoveOptions(board, 2);

        List<String> notations = options.stream().map(o -> o.target().notation()).toList();
        assertEquals(List.of("d4", "d5", "d6", "d7"), notations);
        assertFalse(notations.contains("d8"), "Cannot jump past terminal blocker to d8");

        // Capture Pawn at d7
        CannonService.CannonActionResult res = service.executeCannonMove(board, 2, GridPos.fromNotation("d7"));
        assertTrue(res.valid());
        assertEquals(GridPos.fromNotation("d7"), res.newKnightPos());
        assertTrue(res.enemyDefeated());
        assertEquals(1, res.remainingCharge());
    }

    @Test
    void adjacentScreenAndEnemy_allowsCaptureWithoutEmptyGap() {
        FakeBoard board = new FakeBoard("d1")
            .addRock("d3")
            .addEnemy("d4", "goblin", 2, false);

        CannonService service = new CannonService();
        CannonService.CannonActionResult res = service.executeCannonMove(board, 1, GridPos.fromNotation("d4"));

        assertTrue(res.valid());
        assertEquals(GridPos.fromNotation("d4"), res.newKnightPos());
        assertTrue(res.enemyDefeated());
        assertEquals(0, res.remainingCharge());
    }

    @Test
    void targetWith3Hp_cannotBeCapturedByMove_butCanBeDamagedByCannonAttack() {
        FakeBoard board = new FakeBoard("d1")
            .addRock("d3")
            .addEnemy("d6", "brute", 3, false);

        CannonService service = new CannonService();

        // Cannon Move capture fails (HP > 2) and does not consume charge
        CannonService.CannonActionResult moveRes = service.executeCannonMove(board, 2, GridPos.fromNotation("d6"));
        assertFalse(moveRes.valid());
        assertEquals(2, moveRes.remainingCharge());

        // Cannon Attack succeeds: Knight stays at d1, deals 1 damage, consumes 1 charge
        CannonService.CannonActionResult atkRes = service.executeCannonAttack(board, 2, GridPos.fromNotation("d6"));
        assertTrue(atkRes.valid());
        assertEquals(GridPos.fromNotation("d1"), atkRes.newKnightPos());
        assertEquals(1, atkRes.damageDealt());
        assertFalse(atkRes.enemyDefeated());
        assertEquals(1, atkRes.remainingCharge());
    }

    @Test
    void zeroCharge_blocksBothCannonMoveAndAttack() {
        FakeBoard board = new FakeBoard("d1")
            .addRock("d3")
            .addEnemy("d6", "pawn", 1, false);

        CannonService service = new CannonService();

        assertTrue(service.getLegalMoveOptions(board, 0).isEmpty());
        assertFalse(service.executeCannonMove(board, 0, GridPos.fromNotation("d4")).valid());
        assertFalse(service.executeCannonAttack(board, 0, GridPos.fromNotation("d6")).valid());
    }
}