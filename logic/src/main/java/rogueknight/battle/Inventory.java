package rogueknight.battle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable two-item loadout. Exhausted equipment remains present at zero charges. */
public record Inventory(List<EquipmentState> equipment) {
    public static final int LOADOUT_SIZE = 2;
    public static final int INITIAL_CHARGES = 1;

    public Inventory {
        Objects.requireNonNull(equipment, "equipment");
        equipment = List.copyOf(equipment);
        if (equipment.size() != LOADOUT_SIZE)
            throw new IllegalArgumentException("Inventory must contain exactly two equipment items");
        var types = new HashSet<EquipmentType>();
        for (EquipmentState item : equipment) {
            Objects.requireNonNull(item, "equipment item");
            if (!types.add(item.type())) throw new IllegalArgumentException("Duplicate equipment type: " + item.type());
        }
    }

    public static Inventory initial(EquipmentType first, EquipmentType second) {
        return new Inventory(List.of(new EquipmentState(first, INITIAL_CHARGES),
                new EquipmentState(second, INITIAL_CHARGES)));
    }
    public Optional<EquipmentState> equipment(EquipmentType type) {
        Objects.requireNonNull(type, "type");
        return equipment.stream().filter(item -> item.type() == type).findFirst();
    }
    public int chargesOf(EquipmentType type) { return equipment(type).map(EquipmentState::charges).orElse(0); }
    public boolean contains(EquipmentType type) { return equipment(type).isPresent(); }
    public Inventory consumeOneCharge(EquipmentType type) {
        Objects.requireNonNull(type, "type");
        boolean found = false;
        var updated = new ArrayList<EquipmentState>(equipment.size());
        for (EquipmentState item : equipment) {
            if (item.type() == type) { updated.add(item.consumeOneCharge()); found = true; }
            else updated.add(item);
        }
        if (!found) throw new IllegalArgumentException("Equipment is not selected: " + type);
        return new Inventory(updated);
    }
}
