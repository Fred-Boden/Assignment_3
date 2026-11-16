package com.example.haunted.engine;

import com.example.haunted.config.DungeonFactory;
import com.example.haunted.events.CombatResult;
import com.example.haunted.events.MoveResult;
import com.example.haunted.model.Direction;
import com.example.haunted.model.Monster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineCombatTrapIntegrationTest {

	private GameEngine game;

	@BeforeEach
	void setUp() {
		game = DungeonFactory.createGame();
	}

	@Test
	@DisplayName("Attacking in a room with no such monster returns 'Monster not found.'")
	void attackMissingMonster() {
		CombatResult result = game.attack("Sleep-Deprived TA"); 

		assertFalse(result.isSuccess());
		assertEquals("Monster not found.", result.getMessage());
		assertEquals(50, game.getPlayer().getHealth());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Sleep-Deprived TA", "sleep-deprived ta", "SLEEP-DEPRIVED TA" })
	@DisplayName("Monster lookup by name is case-insensitive")
	void attackIsCaseInsensitive(String name) {
		game.move(Direction.EAST);

		CombatResult result = game.attack(name);

		assertTrue(result.isSuccess());
		assertEquals(6, result.getDamageToMonster());
		assertEquals(4, result.getDamageToPlayer());
	}

	@Test
	@DisplayName("Defeating the TA drops a Coffee Potion next to the existing one")
	void defeatTaInLectureHall() {
		game.move(Direction.EAST);
		CombatResult last = null;
		for (int i = 0; i < 3; i++) {
			last = game.attack("Sleep-Deprived TA");
		}

		assertTrue(last.isMonsterDefeated());
		assertEquals("Defeated Sleep-Deprived TA.", last.getMessage());
		assertEquals(42, game.getPlayer().getHealth());
		assertEquals(2, game.getCurrentRoom().getItems().size()); 
		assertFalse(game.getCurrentRoom().hasLivingMonsters());
		assertFalse(game.isGameOver());

		CombatResult again = game.attack("Sleep-Deprived TA");
		assertEquals("Monster is already defeated.", again.getMessage());
	}

	@Test
	@DisplayName("The Broken Elevator trap fires once for 8 damage")
	void brokenElevatorTrap() {
		game.move(Direction.EAST);

		MoveResult first = game.move(Direction.SOUTH);
		assertTrue(first.isTrapTriggered());
		assertEquals(8, first.getTrapDamage());
		assertEquals("Moved to Broken Elevator. Trap 'Loose Wires Trap' triggered for 8 damage.", first.getMessage());
		assertEquals(42, game.getPlayer().getHealth());

		game.move(Direction.NORTH);
		MoveResult second = game.move(Direction.SOUTH);
		assertFalse(second.isTrapTriggered());
		assertEquals(42, game.getPlayer().getHealth());
	}

	@Test
	@DisplayName("Player dies after enough damage and the game is over, not won")
	void playerDeathEndsGame() {
		game.move(Direction.EAST);
		game.getPlayer().takeDamage(46); 

		CombatResult result = game.attack("Sleep-Deprived TA");

		assertEquals(4, result.getDamageToPlayer());
		assertEquals(0, game.getPlayer().getHealth());
		assertTrue(game.isGameOver());
		assertFalse(game.isGameWon());

		Monster ta = game.getCurrentRoom().findMonster("Sleep-Deprived TA").orElseThrow();
		CombatResult afterDeath = game.attack("Sleep-Deprived TA");
		assertEquals("Player is defeated.", afterDeath.getMessage());
		assertEquals(12, ta.getHealth());
	}
}
