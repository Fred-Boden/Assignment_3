package com.example.haunted.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ItemTest {

    private static class TestItem extends Item {
        TestItem(String name, String description) {
            super(name, description);
        }
    }

    @Test
    void preservesNameAndDescription() {
        Item item = new TestItem("Notebook", "Contains lecture notes");

        assertEquals("Notebook", item.getName());
        assertEquals("Contains lecture notes", item.getDescription());
    }

    @Test
    void toStringReturnsItemName() {
        Item item = new TestItem("Notebook", "Contains lecture notes");

        assertEquals("Notebook", item.toString());
    }

    @Test
    void rejectsNullName() {
        assertThrows(
            NullPointerException.class,
            () -> new TestItem(null, "Contains lecture notes")
        );
    }

    @Test
    void rejectsNullDescription() {
        assertThrows(
            NullPointerException.class,
            () -> new TestItem("Notebook", null)
        );
    }
}