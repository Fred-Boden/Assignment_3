package com.example.haunted.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RoomTest {

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room( "room1", "Lecture Hall", "An empty classroom" );
    }

    @Test
    void returnsDetailsProvidedAtConstruction() {
        assertEquals("room1", room.getId());
        assertEquals("Lecture Hall", room.getName());
        assertEquals("An empty classroom", room.getDescription());
    }

    @Test
    void exitsContainConnectedRoomAndCannotBeModified() {
        Room destination = new Room("room2", "Library", "Dusty books");

        room.connect(Direction.EAST, destination);

        assertEquals(1, room.getExits().size());
        assertSame(destination, room.getExits().get(Direction.EAST));
        assertThrows(UnsupportedOperationException.class,
            () -> room.getExits().put(Direction.EAST, room));
        assertSame(destination, room.getExit(Direction.EAST));
    }

    @Test
    void newRoomHasAnUnmodifiableEmptyMonsterList() {
        assertNotNull(room.getMonsters());
        assertTrue(room.getMonsters().isEmpty());
        assertThrows(UnsupportedOperationException.class,
            () -> room.getMonsters().add(null));
    }

    @Test
    void settingLockStoresRequiredKeyName() {
        room.setLocked(true, "Library Key");

        assertTrue(room.isLocked());
        assertEquals("Library Key", room.getRequiredKeyName());
    }

    @Test
    void unlockingAnAlreadyUnlockedRoomReturnsTrue() {
        assertFalse(room.isLocked());
        assertTrue(room.unlock("Any Key"));
        assertFalse(room.isLocked());
    }

    @Test
    void lockedRoomWithoutRequiredKeyNameCannotBeUnlocked() {
        room.setLocked(true, null);

        assertNull(room.getRequiredKeyName());
        assertFalse(room.unlock("Any Key"));
        assertTrue(room.isLocked());
    }

    @ParameterizedTest
    @CsvSource({
        "Library Key, true",
        "library key, true",
        "LIBRARY KEY, true",
        "Wrong Key, false"
    })
    void unlockingChecksKeyAndUpdatesLock(String suppliedKey, boolean expectedSuccess) {
        room.setLocked(true, "Library Key");

        assertEquals(expectedSuccess, room.unlock(suppliedKey));
        assertEquals(!expectedSuccess, room.isLocked());
        assertEquals("Library Key", room.getRequiredKeyName());
    }

    @Test
    void nullKeyCannotUnlockRoomThatRequiresAKey() {
        room.setLocked(true, "Library Key");

        assertFalse(room.unlock(null));
        assertTrue(room.isLocked());
    }
}