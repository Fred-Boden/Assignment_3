package com.example.haunted.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.haunted.rules.QuestTracker;
import com.example.haunted.model.Player;
import com.example.haunted.model.Inventory;
import com.example.haunted.model.Room;
import com.example.haunted.model.Direction;
import com.example.haunted.model.Key;
import com.example.haunted.events.InteractionResult;

class InteractionEngineTest {
	private QuestTracker questTracker;
	private InteractionEngine interactionEngine;
	private Player testPlayer;
	private Inventory inv;
	private Room startRoom;
	private Room targetRoom;
	private Key testKey;

	@BeforeEach
	void Setup()
	{
		questTracker = new QuestTracker();
		interactionEngine = new InteractionEngine(questTracker);
		startRoom = new Room("s", "Start", "Start Description");
		targetRoom = new Room("t", "Target", "Target Description");
		startRoom.connect(Direction.NORTH, targetRoom);
		inv = new Inventory(10);
		testKey = new Key("Test Key", "Test Key Description");
		inv.addItem(testKey);
		testPlayer = new Player("Test", 10, 10, 10, inv);
		testPlayer.setCurrentRoom(startRoom);
	}

	@Test
	void NoExitInDirection() {
		targetRoom.setLocked(true, testKey.getName());
		InteractionResult interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.EAST);
		assertFalse(interactionResult.isSuccess());
		assertEquals("There is no room in that direction.", interactionResult.getMessage());
		assertTrue(targetRoom.isLocked());
	}
	
	@Test
	void RoomAlreadyUnlocked()
	{
		InteractionResult interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.NORTH);
		assertTrue(interactionResult.isSuccess());
		assertEquals("The room is already unlocked.", interactionResult.getMessage());
		assertFalse(targetRoom.isLocked());
		
		inv.removeItem(testKey.getName());
		interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.NORTH);
		assertTrue(interactionResult.isSuccess());
		assertEquals("The room is already unlocked.", interactionResult.getMessage());
		assertFalse(targetRoom.isLocked());
	}
	
	@Test
	void LockingWithNullKey()
	{
		targetRoom.setLocked(true, null);
		InteractionResult interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.NORTH);
		assertFalse(interactionResult.isSuccess());
		assertEquals("The room cannot be unlocked.", interactionResult.getMessage());
	}
	
	@Test
	void PlayerDoesNotHaveKey()
	{
		targetRoom.setLocked(true, testKey.getName());
		inv.removeItem(testKey.getName());
		InteractionResult interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.NORTH);
		assertFalse(interactionResult.isSuccess());
		assertEquals("You do not have the correct key.", interactionResult.getMessage());
		assertTrue(targetRoom.isLocked());
		
		inv.addItem(new Key("Fake Key", "Fake Key Description"));
		interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.NORTH);
		assertFalse(interactionResult.isSuccess());
		assertEquals("You do not have the correct key.", interactionResult.getMessage());
		assertTrue(targetRoom.isLocked());
	}
	
	@Test
	void PlayerHasKey()
	{
		targetRoom.setLocked(true, testKey.getName());
		InteractionResult interactionResult = interactionEngine.unlockRoom(testPlayer, Direction.NORTH);
		assertTrue(interactionResult.isSuccess());
		assertEquals("Unlocked Target.", interactionResult.getMessage());
		assertFalse(targetRoom.isLocked());
	}

}
