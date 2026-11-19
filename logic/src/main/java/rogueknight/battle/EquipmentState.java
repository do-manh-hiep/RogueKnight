package rogueknight.battle;

import java.util.Objects;

/** Immutable runtime state for one selected equipment item. */
public record EquipmentState(EquipmentType type, int charges) {
    public EquipmentState {
        Objects.requireNonNull(type, "type");
        if (charges < 0) throw new IllegalArgumentException("Equipment charges must be >= 0");
    }
    public EquipmentState withCharges(int newCharges) { return new EquipmentState(type, newCharges); }
    public EquipmentState consumeOneCharge() {
        if (charges == 0) throw new IllegalStateException("Equipment has no remaining charges: " + type);
        return withCharges(charges - 1);
    }
}
