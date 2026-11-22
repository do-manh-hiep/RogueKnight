package rogueknight.action;

/**
 * Action kinds resolved by the battle controller.
 * SWORD is the Knight's adjacent attack; ATTACK is the basic Enemy attack (T-03.02),
 * which Player requests are never allowed to use.
 */
public enum ActionKind {
    MOVE,
    SWORD,
    CAPTURE,
    WAIT,
    CANNON_MOVEMENT,
    CANNON_ATTACK,
    DIAGONAL_SKEWER,
    ATTACK
}
