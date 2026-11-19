package rogueknight.action;

/*
 * ActionError.java  |  T-02.01  |  TD: 4.6, DEC-04 (error codes)
 * Lý do từ chối một request. Mọi request bị từ chối không làm thay đổi BattleState.
 */
public enum ActionError {
    /** Request null hoặc thiếu actor/kind. */
    INVALID_PAYLOAD,
    /** Trận đã kết thúc (result khác ONGOING). */
    BATTLE_FINISHED,
    /** Không ở PLAYER_SELECT. */
    WRONG_PHASE,
    /** Actor không tồn tại trên Board. */
    UNKNOWN_ACTOR,
    /** Actor tồn tại nhưng không phải Knight của người chơi. */
    NOT_PLAYER_ACTOR,
    /** Kind chưa được hỗ trợ ở task hiện tại. */
    UNSUPPORTED_ACTION,
    /** Ô đích nằm ngoài 8x8. */
    OUT_OF_BOUNDS,
    /** Không phải hình L (1,2)/(2,1) hay một ô kề tám hướng. */
    INVALID_GEOMETRY,
    /** Ô đích là Rock. */
    OCCUPIED_BY_ROCK,
    /** Ô đích có quân; normal Move không tự chuyển thành Capture. */
    OCCUPIED_BY_PIECE
}
