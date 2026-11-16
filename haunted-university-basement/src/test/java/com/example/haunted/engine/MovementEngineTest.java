package com.example.haunted.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.haunted.rules.TrapResolver;
import com.example.haunted.model.Player;
import com.example.haunted.model.Inventory;
import com.example.haunted.model.Direction;
import com.example.haunted.model.Room;
import com.example.haunted.events.MoveResult;

class MovementEngineTest {
	private TrapResolver trapResolver;
	private MovementEngine movementEngine;
	private Room startRoom;
	private Room targetRoom;
	private Inventory inv;
	private Player testPlayer;

	@BeforeEach
	void Setup() {
		trapResolver = new TrapResolver();
		movementEngine = new MovementEngine(trapResolver);
		startRoom = new Room("s", "Start", "Start Description");
		targetRoom = new Room("t", "Target", "Target Description");
		startRoom.connect(Direction.NORTH, targetRoom);
		inv = new Inventory(10);
		testPlayer = new Player("Test", 10, 10, 10, inv);
		testPlayer.setCurrentRoom(startRoom);
	}
	
	@Test
	void NoExitInDirection()
	{
		MoveResult moveResult = movementEngine.move(testPlayer, Direction.EAST);
		assertFalse(moveResult.isSuccess());
		assertEquals("There is no room in that direction.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), startRoom);
		
		moveResult = movementEngine.move(testPlayer, Direction.SOUTH);
		assertFalse(moveResult.isSuccess());
		assertEquals("There is no room in that direction.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), startRoom);
		
		moveResult = movementEngine.move(testPlayer, Direction.WEST);
		assertFalse(moveResult.isSuccess());
		assertEquals("There is no room in that direction.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), startRoom);
	}
	
	@Test
	void LockedTarget()
	{
		targetRoom.setLocked(true, "TestKey");
		MoveResult moveResult = movementEngine.move(testPlayer, Direction.NORTH);
		assertFalse(moveResult.isSuccess());
		assertEquals("The room is locked.", moveResult.getMessage());
		assertFalse(moveResult.isTrapTriggered());
		assertEquals(moveResult.getTrapDamage(), 0);
		assertEquals(testPlayer.getCurrentRoom(), startRoom);
		
		targetRoom.unlock("FakeKey");
		moveResult = movementEngine.move(testPlayer, Direction.NORTH);
		assertFalse(moveResult.isSuccess());
		assertEquals("The room is locked.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), startRoom);
		
		targetRoom.unlock("TestKey");
		moveResult = movementEngine.move(testPlayer, Direction.NORTH);
		assertTrue(moveResult.isSuccess());
		assertEquals("Moved to Target.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), targetRoom);
	}
	
	@Test
	void SuccessfulMove()
	{
		MoveResult moveResult = movementEngine.move(testPlayer, Direction.NORTH);
		assertTrue(moveResult.isSuccess());
		assertEquals("Moved to Target.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), targetRoom);
		
		Room newRoom = new Room("r", "New Room", "New Room Description");
		targetRoom.connect(Direction.EAST, newRoom);
		moveResult = movementEngine.move(testPlayer, Direction.EAST);
		assertTrue(moveResult.isSuccess());
		assertEquals("Moved to New Room.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), newRoom);
		
		newRoom.connect(Direction.WEST, targetRoom);
		moveResult = movementEngine.move(testPlayer, Direction.WEST);
		assertTrue(moveResult.isSuccess());
		assertEquals("Moved to Target.", moveResult.getMessage());
		assertEquals(testPlayer.getCurrentRoom(), targetRoom);
	}
}
