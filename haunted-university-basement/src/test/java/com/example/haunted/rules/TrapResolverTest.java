package com.example.haunted.rules;

import com.example.haunted.events.InteractionResult;
import com.example.haunted.model.Inventory;
import com.example.haunted.model.Player;
import com.example.haunted.model.Trap;
import com.example.haunted.model.TrapType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapResolverTest {

	private TrapResolver resolver;
	private Player player;

	@BeforeEach
	void setUp() {
		resolver = new TrapResolver();
		player = new Player("Tester", 50, 5, 2, new Inventory(5));
	}

	@Test
	@DisplayName("A null trap is not triggered and deals no damage")
	void nullTrapDoesNothing() {
		InteractionResult result = resolver.resolveTrap(player, null);

		assertFalse(result.isSuccess());
		assertEquals("No trap was triggered.", result.getMessage());
		assertEquals(50, player.getHealth());
	}

	@Test
	@DisplayName("A disarmed trap is not triggered and deals no damage")
	void disarmedTrapDoesNothing() {
		Trap trap = new Trap("Dead Wires", TrapType.ELECTRIC, 10, false, true);

		InteractionResult result = resolver.resolveTrap(player, trap);

		assertFalse(result.isSuccess());
		assertEquals("No trap was triggered.", result.getMessage());
		assertEquals(50, player.getHealth());
		assertFalse(trap.isArmed());
	}

	@ParameterizedTest(name = "{0} trap, damage {1} -> player health {2}")
	@CsvSource({ "ELECTRIC, 8, 42", "STEAM,    1, 49", "STEAM,    0, 50", "ELECTRIC, 49, 1", "ELECTRIC, 50, 0",
			"STEAM,    75, 0" })
	@DisplayName("An armed trap triggers and damages the player")
	void armedTrapDamagesPlayer(TrapType type, int damage, int expectedHealth) {
		Trap trap = new Trap("Test Trap", type, damage, true, false);

		InteractionResult result = resolver.resolveTrap(player, trap);

		assertTrue(result.isSuccess());
		assertEquals("Trap 'Test Trap' triggered for " + damage + " damage.", result.getMessage());
		assertEquals(expectedHealth, player.getHealth());
		assertEquals(expectedHealth > 0, player.isAlive());
	}

	@Test
	@DisplayName("A one-time trap disarms itself after triggering and cannot fire again")
	void oneTimeTrapDisarmsAfterFirstTrigger() {
		Trap trap = new Trap("Loose Wires Trap", TrapType.ELECTRIC, 8, true, true);

		InteractionResult first = resolver.resolveTrap(player, trap);
		assertTrue(first.isSuccess());
		assertFalse(trap.isArmed());
		assertEquals(42, player.getHealth());

		InteractionResult second = resolver.resolveTrap(player, trap);
		assertFalse(second.isSuccess());
		assertEquals(42, player.getHealth());
	}

	@Test
	@DisplayName("A repeating trap stays armed and triggers every time")
	void repeatingTrapStaysArmed() {
		Trap trap = new Trap("Steam Vent", TrapType.STEAM, 5, true, false);

		for (int i = 1; i <= 3; i++) {
			assertTrue(resolver.resolveTrap(player, trap).isSuccess());
			assertTrue(trap.isArmed());
			assertEquals(50 - 5 * i, player.getHealth());
		}
	}

	@Test
	@DisplayName("Trap getters return constructor values")
	void trapStoresConstructorValues() {
		Trap trap = new Trap("Steam Vent", TrapType.STEAM, 6, true, false);

		assertEquals("Steam Vent", trap.getName());
		assertSame(TrapType.STEAM, trap.getType());
		assertEquals(6, trap.getDamage());
		assertTrue(trap.isArmed());
		assertFalse(trap.isOneTimeTrigger());
	}

	@Test
	@DisplayName("disarm() turns an armed trap off")
	void disarmTurnsTrapOff() {
		Trap trap = new Trap("Wires", TrapType.ELECTRIC, 3, true, true);
		trap.disarm();
		assertFalse(trap.isArmed());
	}

	@Test
	@DisplayName("Trap rejects null name and null type")
	void trapRejectsNulls() {
		assertThrows(NullPointerException.class, () -> new Trap(null, TrapType.STEAM, 1, true, true));
		assertThrows(NullPointerException.class, () -> new Trap("Trap", null, 1, true, true));
	}

	@ParameterizedTest
	@EnumSource(TrapType.class)
	@DisplayName("Every trap type can be resolved")
	void everyTrapTypeResolves(TrapType type) {
		Trap trap = new Trap("Any", type, 4, true, true);
		assertTrue(resolver.resolveTrap(player, trap).isSuccess());
		assertEquals(46, player.getHealth());
		assertEquals(type, TrapType.valueOf(type.name()));
	}
}
