package com.example.haunted.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class InventoryTest {
    @Test
    void newInventoryStartsEmptyWithGivenCapacity() {
        Inventory inventory = new Inventory(3);

        assertEquals(3, inventory.getCapacity());
        assertTrue(inventory.getItems().isEmpty());
        assertFalse(inventory.isFull());
    }

    @Test
    void zeroCapacityInventoryStartsFull() {
        Inventory inventory = new Inventory(0);

        assertTrue(inventory.isFull());
        assertTrue(inventory.getItems().isEmpty());
    }

    @Test
    void searchingForMissingItemReturnsEmpty() {
        Inventory inventory = new Inventory(3);

        assertTrue(inventory.findItem("Missing Item").isEmpty());
        assertFalse(inventory.contains("Missing Item"));
    }

    @Test
    void removingMissingItemReturnsNullAndLeavesInventoryEmpty() {
        Inventory inventory = new Inventory(3);

        assertNull(inventory.removeItem("Missing Item"));
        assertTrue(inventory.getItems().isEmpty());
    }
    
    private static class TestItem extends Item {
        TestItem(String name) {
            super(name, "An item used in tests");
        }
    }

    @Test
    void addingItemAtCapacityLimitRejectsAnotherItem() {
        Inventory inventory = new Inventory(1);
        Item first = new TestItem("Notebook");
        Item second = new TestItem("Pencil");

        assertTrue(inventory.addItem(first));
        assertTrue(inventory.isFull());

        assertFalse(inventory.addItem(second));

        assertEquals(1, inventory.getItems().size());
        assertSame(first, inventory.getItems().get(0));
        assertFalse(inventory.contains("Pencil"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Notebook", "notebook", "NOTEBOOK"})
    void findsItemRegardlessOfCapitalization(String searchName) {
        Inventory inventory = new Inventory(3);
        Item notebook = new TestItem("Notebook");

        inventory.addItem(new TestItem("Pencil"));
        inventory.addItem(notebook);

        assertSame(notebook, inventory.findItem(searchName).orElseThrow());
        assertTrue(inventory.contains(searchName));
    }

    @Test
    void removingExistingItemReturnsItAndFreesSpace() {
        Inventory inventory = new Inventory(1);
        Item notebook = new TestItem("Notebook");
        inventory.addItem(notebook);

        Item removed = inventory.removeItem("Notebook");

        assertSame(notebook, removed);
        assertFalse(inventory.contains("Notebook"));
        assertTrue(inventory.getItems().isEmpty());
        assertFalse(inventory.isFull());

        Item pencil = new TestItem("Pencil");
        assertTrue(inventory.addItem(pencil));
        assertSame(pencil, inventory.getItems().get(0));
    }

    @Test
    void returnedItemListCannotBeModifiedDirectly() {
        Inventory inventory = new Inventory(3);
        Item notebook = new TestItem("Notebook");
        inventory.addItem(notebook);

        assertThrows(
            UnsupportedOperationException.class,
            () -> inventory.getItems().clear()
        );

        assertEquals(1, inventory.getItems().size());
        assertSame(notebook, inventory.getItems().get(0));
    }
}
