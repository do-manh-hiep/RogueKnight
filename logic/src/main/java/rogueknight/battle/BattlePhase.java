package rogueknight.battle;

/*
 * BattlePhase.java  |  T-02.01  |  TD: 5.2
 * Pha điều phối của một trận. Chỉ PLAYER_SELECT nhận Player Action;
 * FINISHED khi result khác ONGOING.
 */
public enum BattlePhase {
    PLAYER_SELECT, PLAYER_RESOLVE, ENEMY_RESOLVE, FINISHED
}
