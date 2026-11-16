package rogueknight.battle;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {
    @Test void initialLoadoutHasTwoDistinctItemsWithOneChargeEach() {
        Inventory inventory = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        assertEquals(2, inventory.equipment().size());
        assertEquals(1, inventory.chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertEquals(1, inventory.chargesOf(EquipmentType.CANNON_ATTACK));
        assertFalse(inventory.contains(EquipmentType.DIAGONAL_SKEWER));
    }
    @Test void rejectsLoadoutWithWrongNumberOfItems() {
        assertThrows(IllegalArgumentException.class, () -> new Inventory(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Inventory(List.of(new EquipmentState(EquipmentType.CANNON_MOVEMENT, 1))));
        assertThrows(IllegalArgumentException.class, () -> new Inventory(List.of(
                new EquipmentState(EquipmentType.CANNON_MOVEMENT, 1), new EquipmentState(EquipmentType.CANNON_ATTACK, 1),
                new EquipmentState(EquipmentType.DIAGONAL_SKEWER, 1))));
    }
    @Test void rejectsDuplicateEquipmentTypes() {
        assertThrows(IllegalArgumentException.class, () -> new Inventory(List.of(
                new EquipmentState(EquipmentType.CANNON_MOVEMENT, 1), new EquipmentState(EquipmentType.CANNON_MOVEMENT, 1))));
    }
    @Test void rejectsNegativeCharges() {
        assertThrows(IllegalArgumentException.class, () -> new EquipmentState(EquipmentType.CANNON_ATTACK, -1));
    }
    @Test void consumingChargeReturnsNewInventoryAndKeepsExhaustedEntry() {
        Inventory original = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.DIAGONAL_SKEWER);
        Inventory consumed = original.consumeOneCharge(EquipmentType.CANNON_MOVEMENT);
        assertEquals(1, original.chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertEquals(0, consumed.chargesOf(EquipmentType.CANNON_MOVEMENT));
        assertTrue(consumed.contains(EquipmentType.CANNON_MOVEMENT));
        assertEquals(1, consumed.chargesOf(EquipmentType.DIAGONAL_SKEWER));
    }
    @Test void rejectsConsumptionOfUnselectedOrExhaustedEquipment() {
        Inventory inventory = Inventory.initial(EquipmentType.CANNON_MOVEMENT, EquipmentType.CANNON_ATTACK);
        assertThrows(IllegalArgumentException.class, () -> inventory.consumeOneCharge(EquipmentType.DIAGONAL_SKEWER));
        Inventory exhausted = inventory.consumeOneCharge(EquipmentType.CANNON_ATTACK);
        assertThrows(IllegalStateException.class, () -> exhausted.consumeOneCharge(EquipmentType.CANNON_ATTACK));
    }
    @Test void equipmentListIsDefensivelyCopiedAndImmutable() {
        var source = new java.util.ArrayList<>(List.of(new EquipmentState(EquipmentType.CANNON_MOVEMENT, 1),
                new EquipmentState(EquipmentType.CANNON_ATTACK, 1)));
        Inventory inventory = new Inventory(source); source.clear();
        assertEquals(2, inventory.equipment().size());
        assertThrows(UnsupportedOperationException.class, () -> inventory.equipment().clear());
    }
}
