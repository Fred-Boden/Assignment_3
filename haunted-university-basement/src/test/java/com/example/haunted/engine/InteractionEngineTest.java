package com.example.haunted.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.example.haunted.events.InteractionResult;
import com.example.haunted.model.*;
import com.example.haunted.rules.QuestTracker;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InteractionEngineTest {

    private InteractionEngine engine;
    private GameEngine gameEngine;
    private Inventory inventory;
    private Player player;
    private Room room;
    private Quest quest;

    private static class TestItem extends Item {
        TestItem(String name) {
            super(name, "Test item");
        }
    }

    @BeforeEach
    void setUp() {
        engine = new InteractionEngine(new QuestTracker());
        inventory = new Inventory(2);
        player = new Player("Student", 100, 10, 5, inventory);
        room = new Room("room1", "Lecture Hall", "An empty classroom");
        player.setCurrentRoom(room);
        quest = new Quest("Recover Gradebook", "Find it and defeat phantom");
        gameEngine = new GameEngine(player, quest, null, null, engine);
    }
    
    @Test
    void mainCanBeConstructed() {
        assertNotNull(new com.example.haunted.Main());
    }
    
    @Test
    void mainRunsWithoutErrors() {
        assertDoesNotThrow(() ->
            com.example.haunted.Main.main(new String[0])
        );
    }
    
    @Test
    void playerReturnsNameProvidedAtConstruction() { assertEquals("Student", player.getName()); }

    @ParameterizedTest
    @CsvSource({
        "Lost Gradebook, false, IN_PROGRESS",
        "lost gradebook, false, IN_PROGRESS",
        "LOST GRADEBOOK, false, IN_PROGRESS",
        "Lost Gradebook, true,  COMPLETED"
    })
    void pickingUpGradebookTransfersItemAndUpdatesQuest(
            String searchName,
            boolean phantomAlreadyDefeated,
            QuestStatus expectedStatus) {

        Item gradebook = new TestItem("Lost Gradebook");
        Item notebook = new TestItem("Notebook");
        room.addItem(notebook);
        room.addItem(gradebook);

        if (phantomAlreadyDefeated) {
            quest.markPhantomDefeated();
        }

        InteractionResult result =
            engine.pickUpItem(player, quest, searchName);

        assertTrue(result.isSuccess());
        assertEquals("Picked up Lost Gradebook.", result.getMessage());
        assertEquals(List.of(gradebook), inventory.getItems());
        assertEquals(List.of(notebook), room.getItems());

        assertTrue(quest.isGradebookRecovered());
        assertEquals(phantomAlreadyDefeated, quest.isPhantomDefeated());
        assertEquals(expectedStatus, quest.getStatus());
        assertEquals(phantomAlreadyDefeated, quest.isComplete());
    }

    @Test
    void pickingUpOrdinaryItemDoesNotAdvanceQuest() {
        Item notebook = new TestItem("Notebook");
        room.addItem(notebook);

        InteractionResult result =
            engine.pickUpItem(player, quest, "Notebook");

        assertTrue(result.isSuccess());
        assertEquals("Picked up Notebook.", result.getMessage());
        assertEquals(List.of(notebook), inventory.getItems());
        assertTrue(room.getItems().isEmpty());

        assertFalse(quest.isGradebookRecovered());
        assertFalse(quest.isPhantomDefeated());
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
    }

    @Test
    void pickingUpMissingItemLeavesRoomInventoryAndQuestUnchanged() {
        Item notebook = new TestItem("Notebook");
        Item pencil = new TestItem("Pencil");
        room.addItem(notebook);
        inventory.addItem(pencil);

        InteractionResult result =
            engine.pickUpItem(player, quest, "Lost Gradebook");

        assertFalse(result.isSuccess());
        assertEquals("Item not found in the room.", result.getMessage());
        assertEquals(List.of(notebook), room.getItems());
        assertEquals(List.of(pencil), inventory.getItems());

        assertFalse(quest.isGradebookRecovered());
        assertFalse(quest.isPhantomDefeated());
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
    }

    @Test
    void fullInventoryRestoresItemToRoomWithoutAdvancingQuest() {
        Item notebook = new TestItem("Notebook");
        Item pencil = new TestItem("Pencil");
        Item gradebook = new TestItem("Lost Gradebook");

        inventory.addItem(notebook);
        inventory.addItem(pencil);
        room.addItem(gradebook);
        quest.markPhantomDefeated();

        InteractionResult result =
            engine.pickUpItem(player, quest, "Lost Gradebook");

        assertFalse(result.isSuccess());
        assertEquals("Inventory is full.", result.getMessage());
        assertEquals(List.of(notebook, pencil), inventory.getItems());
        assertEquals(List.of(gradebook), room.getItems());

        assertFalse(quest.isGradebookRecovered());
        assertTrue(quest.isPhantomDefeated());
        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @ParameterizedTest
    @CsvSource({
        "Coffee Potion, 40, 80",
        "coffee potion, 40, 80",
        "COFFEE POTION, 40, 80",
        "Coffee Potion,  0, 100"
    })
    void usingPotionHealsAndConsumesOnlyThatItem(
            String searchName, int damage, int expectedHealth) {

        Potion potion = new Potion("Coffee Potion", "Restores health", 20);
        Item notebook = new TestItem("Notebook");
        inventory.addItem(potion);
        inventory.addItem(notebook);
        player.takeDamage(damage);

        InteractionResult result = engine.useItem(player, searchName);

        assertTrue(result.isSuccess());
        assertEquals("Used Coffee Potion.", result.getMessage());
        assertEquals(expectedHealth, player.getHealth());
        assertEquals(List.of(notebook), inventory.getItems());
        assertFalse(inventory.contains("Coffee Potion"));
    }

    @ParameterizedTest
    @CsvSource({
        "Missing Item, Item not found in inventory.",
        "Rusty Sword, That item cannot be used."
    })
    void failedItemUsePreservesHealthAndInventory(
            String searchName, String expectedMessage) {

        Weapon sword = new Weapon("Rusty Sword", "An old sword", 7);
        Potion potion = new Potion("Coffee Potion", "Restores health", 20);
        inventory.addItem(sword);
        inventory.addItem(potion);
        player.takeDamage(40);

        InteractionResult result = engine.useItem(player, searchName);

        assertFalse(result.isSuccess());
        assertEquals(expectedMessage, result.getMessage());
        assertEquals(60, player.getHealth());
        assertEquals(List.of(sword, potion), inventory.getItems());
    }

    @Test
    void equippingWeaponReplacesOldWeaponAndKeepsItemsInInventory() {
        Weapon oldWeapon = new Weapon("Old Sword", "Worn sword", 2);
        Weapon newWeapon = new Weapon("Rusty Sword", "Stronger sword", 7);

        inventory.addItem(oldWeapon);
        inventory.addItem(newWeapon);
        player.equipWeapon(oldWeapon);

        InteractionResult result =
            engine.equipItem(player, "rusty sword");

        assertTrue(result.isSuccess());
        assertEquals("Equipped weapon Rusty Sword.", result.getMessage());
        assertSame(newWeapon, player.getEquippedWeapon());
        assertEquals(17, player.getAttackPower());
        assertEquals(List.of(oldWeapon, newWeapon), inventory.getItems());
        assertNull(player.getEquippedArmor());
    }

    @Test
    void equippingArmorReplacesOldArmorAndKeepsItemsInInventory() {
        Armor oldArmor = new Armor("Old Coat", "Worn coat", 1);
        Armor newArmor = new Armor("Lab Coat", "Protective coat", 4);

        inventory.addItem(oldArmor);
        inventory.addItem(newArmor);
        player.equipArmor(oldArmor);

        InteractionResult result =
            engine.equipItem(player, "lab coat");

        assertTrue(result.isSuccess());
        assertEquals("Equipped armor Lab Coat.", result.getMessage());
        assertSame(newArmor, player.getEquippedArmor());
        assertEquals(9, player.getDefensePower());
        assertEquals(List.of(oldArmor, newArmor), inventory.getItems());
        assertNull(player.getEquippedWeapon());
    }

    @ParameterizedTest
    @CsvSource({
        "Missing Item, Item not found in inventory.",
        "Coffee Potion, That item cannot be equipped."
    })
    void failedEquipPreservesExistingEquipmentAndInventory(
            String searchName, String expectedMessage) {

        Weapon sword = new Weapon("Rusty Sword", "An old sword", 7);
        Armor armor = new Armor("Lab Coat", "Protective coat", 4);
        Potion potion = new Potion("Coffee Potion", "Restores health", 20);

        player.equipWeapon(sword);
        player.equipArmor(armor);
        inventory.addItem(potion);

        InteractionResult result =
            engine.equipItem(player, searchName);

        assertFalse(result.isSuccess());
        assertEquals(expectedMessage, result.getMessage());
        assertSame(sword, player.getEquippedWeapon());
        assertSame(armor, player.getEquippedArmor());
        assertEquals(17, player.getAttackPower());
        assertEquals(9, player.getDefensePower());
        assertEquals(List.of(potion), inventory.getItems());
    }
    
    @Test
    void gameEnginePickUpTransfersItemAndUpdatesQuest() {
        Item gradebook = new TestItem("Lost Gradebook");
        room.addItem(gradebook);
        quest.markPhantomDefeated();

        InteractionResult result =
            gameEngine.pickUpItem("Lost Gradebook");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Picked up Lost Gradebook.", result.getMessage());
        assertEquals(List.of(gradebook), inventory.getItems());
        assertTrue(room.getItems().isEmpty());
        assertTrue(quest.isGradebookRecovered());
        assertTrue(quest.isPhantomDefeated());
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertTrue(quest.isComplete());
    }
    
    @Test
    void gameEngineEquipUpdatesWeaponAndAttack() {
        Weapon sword = new Weapon("Rusty Sword", "An old sword", 7);
        inventory.addItem(sword);

        InteractionResult result =
            gameEngine.equipItem("Rusty Sword");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Equipped weapon Rusty Sword.", result.getMessage());
        assertSame(sword, player.getEquippedWeapon());
        assertEquals(17, player.getAttackPower());
        assertEquals(List.of(sword), inventory.getItems());
    }
    
    @Test
    void gameEngineUseHealsAndConsumesPotion() {
        Potion potion =
            new Potion("Coffee Potion", "Restores health", 20);
        inventory.addItem(potion);
        player.takeDamage(40);
        assertEquals(60, player.getHealth());

        InteractionResult result =
            gameEngine.useItem("Coffee Potion");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Used Coffee Potion.", result.getMessage());
        assertEquals(80, player.getHealth());
        assertTrue(inventory.getItems().isEmpty());
    }
    
    @Test
    void gameEngineUnlockFailsWithoutConnectedRoom() {
        // No exits were added to this room.
        Direction direction = Direction.values()[0];

        InteractionResult result =
            gameEngine.unlockRoom(direction);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertSame(room, player.getCurrentRoom());
        assertTrue(inventory.getItems().isEmpty());
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
    }
    
    @Test
    void gameEngineReturnsOriginalQuest() { assertSame(quest, gameEngine.getQuest()); }
    
    @Test
    void gameIsNotWonWhenQuestIsIncomplete() {
        assertTrue(player.isAlive());
        assertFalse(quest.isComplete());
        assertFalse(gameEngine.isGameWon());
    }
    
    @Test
    void gameIsWonWhenQuestIsCompleteAndPlayerIsAlive() {
        room.addItem(new TestItem("Lost Gradebook"));
        quest.markPhantomDefeated();
        gameEngine.pickUpItem("Lost Gradebook");

        assertTrue(quest.isComplete());
        assertTrue(player.isAlive());
        assertTrue(gameEngine.isGameWon());
    }
    
    @Test
    void gameIsNotWonWhenQuestIsCompleteButPlayerIsDead() {
        room.addItem(new TestItem("Lost Gradebook"));
        quest.markPhantomDefeated();
        gameEngine.pickUpItem("Lost Gradebook");
        player.takeDamage(100);

        assertTrue(quest.isComplete());
        assertFalse(player.isAlive());
        assertFalse(gameEngine.isGameWon());
    }
    
    @Test
    void unlockingAlreadyUnlockedRoomSucceeds() {
        Room target = new Room("room2", "Library", "Dusty books");
        room.connect(Direction.EAST, target);

        InteractionResult result = engine.unlockRoom(player, Direction.EAST);

        assertTrue(result.isSuccess());
        assertEquals("The room is already unlocked.", result.getMessage());
        assertFalse(target.isLocked());
        assertSame(room, player.getCurrentRoom());
    }
    
    @Test
    void unlockingRoomWithoutRequiredKeyNameFails() {
        Room target = new Room("room2", "Library", "Dusty books");
        target.setLocked(true, null);
        room.connect(Direction.EAST, target);

        InteractionResult result = engine.unlockRoom(player, Direction.EAST);

        assertFalse(result.isSuccess());
        assertEquals("The room cannot be unlocked.", result.getMessage());
        assertTrue(target.isLocked());
    }
    
    @Test
    void unlockingRoomWithoutKeyFails() {
        Room target = new Room("room2", "Library", "Dusty books");
        target.setLocked(true, "Library Key");
        room.connect(Direction.EAST, target);

        InteractionResult result = engine.unlockRoom(player, Direction.EAST);

        assertFalse(result.isSuccess());
        assertEquals("You do not have the correct key.", result.getMessage());
        assertTrue(target.isLocked());
        assertTrue(inventory.getItems().isEmpty());
    }
    
    @Test
    void ordinaryItemWithMatchingKeyNameCannotUnlockRoom() {
        Room target = new Room("room2", "Library", "Dusty books");
        target.setLocked(true, "Library Key");
        room.connect(Direction.EAST, target);

        Item fakeKey = new TestItem("Library Key");
        inventory.addItem(fakeKey);

        InteractionResult result = engine.unlockRoom(player, Direction.EAST);

        assertFalse(result.isSuccess());
        assertEquals("You do not have the correct key.", result.getMessage());
        assertTrue(target.isLocked());
        assertEquals(List.of(fakeKey), inventory.getItems());
    }
    
    @Test
    void correctKeyUnlocksRoomAndRemainsInInventory() {
        Room target = new Room("room2", "Library", "Dusty books");
        target.setLocked(true, "Library Key");
        room.connect(Direction.EAST, target);

        Key key = new Key("Library Key", "Opens the library");
        inventory.addItem(key);

        InteractionResult result =
            engine.unlockRoom(player, Direction.EAST);

        assertTrue(result.isSuccess());
        assertEquals("Unlocked Library.", result.getMessage());
        assertFalse(target.isLocked());
        assertEquals(List.of(key), inventory.getItems());
        assertSame(room, player.getCurrentRoom());
    }
}
