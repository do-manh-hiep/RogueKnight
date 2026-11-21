package rogueknight.core.input;

/**
 * UI action mode selected by the player; maps one-to-one to a logic ActionKind in the adapter
 * (T-04.01, T-04.02). Each mode has a fixed keyboard key and a short label for the HUD.
 */
public enum ActionMode {
    MOVE(1, "Move"),
    CANNON_MOVEMENT(2, "Cannon Move"),
    CANNON_ATTACK(3, "Cannon Attack"),
    DIAGONAL_SKEWER(4, "Skewer");

    private final int key;
    private final String label;

    ActionMode(int key, String label) {
        this.key = key;
        this.label = label;
    }

    public int key() { return key; }

    public String label() { return label; }
}
