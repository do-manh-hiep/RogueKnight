package rogueknight.core.input;

/** Player-facing action types. Keyboard numbers are assigned by their current list position. */
public enum ActionMode {
    WAIT("Wait"),
    MOVE("Move / Attack"),
    CANNON_MOVEMENT("Cannon Move"),
    CANNON_ATTACK("Cannon Attack"),
    DIAGONAL_SKEWER("Skewer");

    private final String label;

    ActionMode(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
