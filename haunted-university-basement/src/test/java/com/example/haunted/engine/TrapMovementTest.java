package com.example.haunted.engine; 

import com.example.haunted.events.MoveResult;
import com.example.haunted.model.Direction;
import com.example.haunted.model.Inventory;
import com.example.haunted.model.Player;
import com.example.haunted.model.Room;
import com.example.haunted.model.Trap;
import com.example.haunted.model.TrapType;
import com.example.haunted.rules.TrapResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapMovementTest {

	private MovementEngine movementEngine;
	private Player player;
	private Room hall;
	private Room trapRoom;

	@BeforeEach
	void setUp() {
		movementEngine = new MovementEngine(new TrapResolver());
		hall = new Room("hall", "Hall", "Safe room.");
		trapRoom = new Room("elevator", "Broken Elevator", "Dangerous room.");
		hall.connect(Direction.SOUTH, trapRoom);
		trapRoom.connect(Direction.NORTH, hall);
		player = new Player("Tester", 50, 7, 2, new Inventory(5));
		player.setCurrentRoom(hall);
	}

	@Test
	@DisplayName("Entering a room with an armed trap triggers it")
	void enteringTrappedRoomTriggersTrap() {
		trapRoom.setTrap(new Trap("Loose Wires Trap", TrapType.ELECTRIC, 8, true, true));

		MoveResult result = movementEngine.move(player, Direction.SOUTH);

		assertTrue(result.isSuccess());
		assertTrue(result.isTrapTriggered());
		assertEquals(8, result.getTrapDamage());
		assertEquals("Moved to Broken Elevator. Trap 'Loose Wires Trap' triggered for 8 damage.", result.getMessage());
		assertEquals(42, player.getHealth());
		assertSame(trapRoom, player.getCurrentRoom());
	}

	@Test
	@DisplayName("A one-time trap does not fire on the second visit")
	void oneTimeTrapFiresOnlyOnce() {
		Trap trap = new Trap("Loose Wires Trap", TrapType.ELECTRIC, 8, true, true);
		trapRoom.setTrap(trap);

		movementEngine.move(player, Direction.SOUTH);
		movementEngine.move(player, Direction.NORTH);
		MoveResult second = movementEngine.move(player, Direction.SOUTH);

		assertTrue(second.isSuccess());
		assertFalse(second.isTrapTriggered());
		assertEquals(0, second.getTrapDamage());
		assertEquals("Moved to Broken Elevator.", second.getMessage());
		assertEquals(42, player.getHealth());
		assertFalse(trap.isArmed());
	}

	@Test
	@DisplayName("A repeating trap fires on every visit")
	void repeatingTrapFiresEveryVisit() {
		trapRoom.setTrap(new Trap("Steam Vent", TrapType.STEAM, 5, true, false));

		for (int visit = 1; visit <= 3; visit++) {
			MoveResult result = movementEngine.move(player, Direction.SOUTH);
			assertTrue(result.isTrapTriggered());
			assertEquals(50 - 5 * visit, player.getHealth());
			movementEngine.move(player, Direction.NORTH);
		}
	}

	@Test
	@DisplayName("A disarmed trap in the room is ignored")
	void disarmedTrapIsIgnored() {
		trapRoom.setTrap(new Trap("Dead Vent", TrapType.STEAM, 20, false, false));

		MoveResult result = movementEngine.move(player, Direction.SOUTH);

		assertTrue(result.isSuccess());
		assertFalse(result.isTrapTriggered());
		assertEquals(0, result.getTrapDamage());
		assertEquals(50, player.getHealth());
	}

	@Test
	@DisplayName("Moving into a room with no trap reports no trap")
	void roomWithoutTrap() {
		MoveResult result = movementEngine.move(player, Direction.SOUTH);
		assertFalse(result.isTrapTriggered());
		assertEquals("Moved to Broken Elevator.", result.getMessage());
	}

	@Test
	@DisplayName("A locked trapped room is not entered, so its trap does not fire")
	void lockedRoomTrapDoesNotFire() {
		Trap trap = new Trap("Loose Wires Trap", TrapType.ELECTRIC, 8, true, true);
		trapRoom.setTrap(trap);
		trapRoom.setLocked(true, "Some Key");

		MoveResult result = movementEngine.move(player, Direction.SOUTH);

		assertFalse(result.isSuccess());
		assertFalse(result.isTrapTriggered());
		assertEquals(50, player.getHealth());
		assertTrue(trap.isArmed());
		assertSame(hall, player.getCurrentRoom());
	}

	@ParameterizedTest(name = "trap damage {0} on 50 HP player -> {1} HP, alive={2}")
	@CsvSource({ "49, 1, true", "50, 0, false", "51, 0, false" })
	@DisplayName("Trap damage boundaries on entering a room")
	void trapDamageBoundaries(int damage, int expectedHealth, boolean alive) {
		trapRoom.setTrap(new Trap("Pit", TrapType.STEAM, damage, true, true));

		MoveResult result = movementEngine.move(player, Direction.SOUTH);

		assertEquals(damage, result.getTrapDamage());
		assertEquals(expectedHealth, player.getHealth());
		assertEquals(alive, player.isAlive());
	}
}
