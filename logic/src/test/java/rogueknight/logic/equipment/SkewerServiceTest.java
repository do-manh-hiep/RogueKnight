package rogueknight.logic.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkewerServiceTest {

    @Test
    void skewerHitsTwoConsecutiveEnemies_deals1DamageEachAndConsumes1Charge() {
        CannonServiceTest.FakeBoard board = new CannonServiceTest.FakeBoard("c2")
            .addEnemy("d3", "pawn_1", 1, false)
            .addEnemy("e4", "knight_enemy", 2, false);

        SkewerService service = new SkewerService();
        SkewerService.SkewerResult res = service.executeSkewer(board, 2, 1, 1);

        assertTrue(res.valid());
        assertEquals(2, res.traversedTiles().size());
        assertEquals(2, res.enemyHits().size());
        assertEquals("pawn_1", res.enemyHits().get(0).enemy().id());
        assertTrue(res.enemyHits().get(0).defeated());
        assertEquals("knight_enemy", res.enemyHits().get(1).enemy().id());
        assertFalse(res.enemyHits().get(1).defeated());
        assertEquals(1, res.remainingCharge());
    }

    @Test
    void skewerBlockedByRock_stopsBeforeRock() {
        CannonServiceTest.FakeBoard board = new CannonServiceTest.FakeBoard("c2")
            .addRock("d3")
            .addEnemy("e4", "pawn_1", 1, false);

        SkewerService service = new SkewerService();
        SkewerService.SkewerResult res = service.executeSkewer(board, 2, 1, 1);

        assertFalse(res.valid());
        assertEquals(GridPos.fromNotation("c2"), res.finalKnightPos());
        assertEquals(2, res.remainingCharge());
    }

    @Test
    void skewerDefeatingKing_triggersImmediateBattleWin() {
        CannonServiceTest.FakeBoard board = new CannonServiceTest.FakeBoard("c2")
            .addEnemy("d3", "king", 1, true);

        SkewerService service = new SkewerService(true);
        SkewerService.SkewerResult res = service.executeSkewer(board, 1, 1, 1);

        assertTrue(res.valid());
        assertTrue(res.battleWon());
        assertEquals(0, res.remainingCharge());
    }

    @Test
    void skewerWithZeroCharge_isRejected() {
        CannonServiceTest.FakeBoard board = new CannonServiceTest.FakeBoard("c2")
            .addEnemy("d3", "pawn_1", 1, false);

        SkewerService service = new SkewerService();
        SkewerService.SkewerResult res = service.executeSkewer(board, 0, 1, 1);

        assertFalse(res.valid());
        assertEquals(0, res.remainingCharge());
    }
}