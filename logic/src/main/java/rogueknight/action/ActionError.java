package rogueknight.action;

/** Reasons an Action request can be rejected without mutating battle state. */
public enum ActionError {
    INVALID_PAYLOAD,
    BATTLE_FINISHED,
    WRONG_PHASE,
    UNKNOWN_ACTOR,
    NOT_PLAYER_ACTOR,
    NOT_ENEMY_ACTOR,
    UNSUPPORTED_ACTION,
    OUT_OF_BOUNDS,
    INVALID_GEOMETRY,
    OCCUPIED_BY_ROCK,
    OCCUPIED_BY_PIECE,
    NO_TARGET_PIECE,
    NOT_ENEMY_TARGET,
    CAPTURE_TARGET_TOO_STRONG,
    EQUIPMENT_NOT_OWNED,
    NO_EQUIPMENT_CHARGES
}