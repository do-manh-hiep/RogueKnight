package rogueknight.core.input;

/** Player-facing actions and their stable keyboard shortcuts. */
public enum ActionMode {
    WAIT(0, "Wait"),
    MOVE(1, "Move / Attack"),
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
