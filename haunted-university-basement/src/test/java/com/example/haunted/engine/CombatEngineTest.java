package com.example.haunted.engine;

import com.example.haunted.events.CombatResult;
import com.example.haunted.model.Armor;
import com.example.haunted.model.BossMonster;
import com.example.haunted.model.Inventory;
import com.example.haunted.model.Item;
import com.example.haunted.model.Monster;
import com.example.haunted.model.Player;
import com.example.haunted.model.Potion;
import com.example.haunted.model.Quest;
import com.example.haunted.model.QuestStatus;
import com.example.haunted.model.Room;
import com.example.haunted.model.Weapon;
import com.example.haunted.rules.DamageCalculator;
import com.example.haunted.rules.QuestTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatEngineTest {

	private CombatEngine combatEngine;
	private Player player;
	private Quest quest;
	private Room room;

	@BeforeEach
	void setUp() {
		combatEngine = new CombatEngine(new DamageCalculator(), new QuestTracker());
		player = new Player("Student Explorer", 50, 7, 2, new Inventory(8));
		room = new Room("arena", "Arena", "A test room.");
		player.setCurrentRoom(room);
		quest = new Quest("Escape the Basement", "test quest");
	}

	private static Monster ta() {
		return new Monster("Sleep-Deprived TA", 18, 6, 1,
				List.of(new Potion("Coffee Potion", "A backup coffee drop.", 15)));
	}

	@Test
	@DisplayName("Attacking a null monster fails with 'Monster not found.'")
	void attackNullMonster() {
		CombatResult result = combatEngine.attack(player, quest, null);

		assertFalse(result.isSuccess());
		assertEquals("Monster not found.", result.getMessage());
		assertEquals(0, result.getDamageToMonster());
		assertEquals(0, result.getDamageToPlayer());
		assertFalse(result.isMonsterDefeated());
		assertTrue(result.getDroppedItems().isEmpty());
		assertEquals(50, player.getHealth());
	}

	@Test
	@DisplayName("A defeated player cannot attack and the monster is untouched")
	void deadPlayerCannotAttack() {
		Monster monster = ta();
		player.takeDamage(50);

		CombatResult result = combatEngine.attack(player, quest, monster);

		assertFalse(result.isSuccess());
		assertEquals("Player is defeated.", result.getMessage());
		assertEquals(0, result.getDamageToMonster());
		assertEquals(0, result.getDamageToPlayer());
		assertFalse(result.isMonsterDefeated());
		assertEquals(18, monster.getHealth());
	}

	@Test
	@DisplayName("Attacking an already defeated monster fails and drops no loot again")
	void attackAlreadyDefeatedMonster() {
		Monster monster = ta();
		monster.takeDamage(18);

		CombatResult result = combatEngine.attack(player, quest, monster);

		assertFalse(result.isSuccess());
		assertEquals("Monster is already defeated.", result.getMessage());
		assertEquals(0, result.getDamageToMonster());
		assertEquals(0, result.getDamageToPlayer());
		assertTrue(result.isMonsterDefeated());
		assertTrue(result.getDroppedItems().isEmpty());
		assertTrue(room.getItems().isEmpty());
		assertEquals(50, player.getHealth());
	}

	@Test
	@DisplayName("A player at exactly 1 HP can still attack")
	void playerWithOneHealthCanAttack() {
		player.takeDamage(49);
		CombatResult result = combatEngine.attack(player, quest, ta());
		assertTrue(result.isSuccess());
		assertEquals(0, player.getHealth());
		assertFalse(player.isAlive());
	}

	@Test
	@DisplayName("A non-lethal hit damages the monster and triggers a counter-attack")
	void nonLethalHitTriggersCounterAttack() {
		Monster monster = ta();

		CombatResult result = combatEngine.attack(player, quest, monster);

		assertTrue(result.isSuccess());
		assertEquals("Attacked Sleep-Deprived TA.", result.getMessage());
		assertEquals(6, result.getDamageToMonster());
		assertEquals(4, result.getDamageToPlayer());
		assertFalse(result.isMonsterDefeated());
		assertTrue(result.getDroppedItems().isEmpty());
		assertEquals(12, monster.getHealth());
		assertEquals(46, player.getHealth());
		assertTrue(room.getItems().isEmpty());
	}

	@ParameterizedTest(name = "monster HP {0}: defeated={1}, player HP after={2}")
	@CsvSource({ "7, false, 46", "6, true,  50", "5, true,  50", "1, true,  50" })
	@DisplayName("Kill boundary: monster dies when damage >= remaining HP")
	void killBoundary(int monsterHealth, boolean defeated, int playerHealthAfter) {
		Monster monster = new Monster("Target", monsterHealth, 6, 1, List.of());

		CombatResult result = combatEngine.attack(player, quest, monster);

		assertTrue(result.isSuccess());
		assertEquals(defeated, result.isMonsterDefeated());
		assertEquals(defeated, !monster.isAlive());
		assertEquals(6, result.getDamageToMonster());
		assertEquals(defeated ? 0 : 4, result.getDamageToPlayer());
		assertEquals(playerHealthAfter, player.getHealth());
		assertEquals(defeated ? "Defeated Target." : "Attacked Target.", result.getMessage());
	}

	@Test
	@DisplayName("Fighting the TA to the end takes three rounds and drops its loot")
	void fullFightAgainstTa() {
		Monster monster = ta();

		CombatResult r1 = combatEngine.attack(player, quest, monster);
		CombatResult r2 = combatEngine.attack(player, quest, monster);
		CombatResult r3 = combatEngine.attack(player, quest, monster);

		assertFalse(r1.isMonsterDefeated());
		assertFalse(r2.isMonsterDefeated());
		assertTrue(r3.isMonsterDefeated());
		assertEquals(0, monster.getHealth());
		assertEquals(42, player.getHealth());
		assertEquals(1, r3.getDroppedItems().size());
		assertEquals("Coffee Potion", r3.getDroppedItems().get(0).getName());
		assertTrue(room.findItem("Coffee Potion").isPresent());
	}

	@Test
	@DisplayName("All loot items are dropped into the player's current room in order")
	void allLootDroppedIntoRoom() {
		Weapon sword = new Weapon("Dry Erase Sword", "d", 6);
		Armor gown = new Armor("Graduation Gown Armor", "d", 5);
		Monster monster = new Monster("Looter", 1, 1, 0, List.of(sword, gown));

		CombatResult result = combatEngine.attack(player, quest, monster);

		assertEquals(List.of(sword, gown), result.getDroppedItems());
		assertEquals(List.of(sword, gown), room.getItems());
		assertSame(sword, room.findItem("Dry Erase Sword").orElseThrow());
	}

	@Test
	@DisplayName("Minimum damage of 1 still applies against a heavily armored monster")
	void minimumDamageAgainstArmoredMonster() {
		Monster tank = new Monster("Tank", 3, 1, 100, List.of());

		combatEngine.attack(player, quest, tank);
		combatEngine.attack(player, quest, tank);
		assertEquals(1, tank.getHealth());

		CombatResult finalHit = combatEngine.attack(player, quest, tank);
		assertEquals(1, finalHit.getDamageToMonster());
		assertTrue(finalHit.isMonsterDefeated());
	}

	@Test
	@DisplayName("A counter-attack can defeat the player")
	void counterAttackCanKillPlayer() {
		Player weak = new Player("Weak", 3, 1, 0, new Inventory(1));
		weak.setCurrentRoom(room);
		Monster brute = new Monster("Brute", 100, 10, 0, List.of());

		CombatResult result = combatEngine.attack(weak, quest, brute);

		assertTrue(result.isSuccess());
		assertEquals(10, result.getDamageToPlayer());
		assertEquals(0, weak.getHealth());
		assertFalse(weak.isAlive());

		CombatResult afterDeath = combatEngine.attack(weak, quest, brute);
		assertEquals("Player is defeated.", afterDeath.getMessage());
		assertEquals(99, brute.getHealth());
	}

	@Test
	@DisplayName("Equipped gear changes damage in both directions")
	void gearAffectsCombat() {
		player.equipWeapon(new Weapon("Stapler of Justice", "d", 4));
		player.equipArmor(new Armor("Calculator Shield", "d", 3));
		Monster golem = new Monster("Spreadsheet Golem", 28, 7, 4, List.of());

		CombatResult result = combatEngine.attack(player, quest, golem);

		assertEquals(7, result.getDamageToMonster());
		assertEquals(2, result.getDamageToPlayer());
		assertEquals(21, golem.getHealth());
		assertEquals(48, player.getHealth());
	}

	@ParameterizedTest(name = "player attack {0} -> boss HP {1}, counter {2}")
	@CsvSource({ "23, 21, 8", "24, 20, 11", "25, 19, 11" })
	@DisplayName("Boss counter-attack uses enraged attack after the hit lands")
	void bossEnragesWithinSameRound(int playerAttack, int bossHealthAfter, int expectedCounter) {
		Player strong = new Player("Strong", 50, playerAttack, 2, new Inventory(1));
		strong.setCurrentRoom(room);
		BossMonster boss = new BossMonster("Final Exam Phantom", 40, 10, 4, List.of(), 3);

		CombatResult result = combatEngine.attack(strong, quest, boss);

		assertEquals(bossHealthAfter, boss.getHealth());
		assertEquals(expectedCounter, result.getDamageToPlayer());
		assertEquals(50 - expectedCounter, strong.getHealth());
	}

	@Test
	@DisplayName("Defeating the Final Exam Phantom marks the quest in progress")
	void defeatingPhantomUpdatesQuest() {
		BossMonster boss = new BossMonster("Final Exam Phantom", 1, 10, 0, List.of(), 3);

		CombatResult result = combatEngine.attack(player, quest, boss);

		assertTrue(result.isMonsterDefeated());
		assertTrue(quest.isPhantomDefeated());
		assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
		assertFalse(quest.isComplete());
	}

	@Test
	@DisplayName("Defeating the Phantom after recovering the gradebook completes the quest")
	void defeatingPhantomWithGradebookCompletesQuest() {
		quest.markGradebookRecovered();
		BossMonster boss = new BossMonster("Final Exam Phantom", 1, 10, 0, List.of(), 3);

		combatEngine.attack(player, quest, boss);

		assertTrue(quest.isComplete());
		assertEquals(QuestStatus.COMPLETED, quest.getStatus());
	}

	@Test
	@DisplayName("Damaging but not killing the Phantom does not update the quest")
	void woundingPhantomDoesNotUpdateQuest() {
		BossMonster boss = new BossMonster("Final Exam Phantom", 40, 10, 4, List.of(), 3);

		combatEngine.attack(player, quest, boss);

		assertTrue(boss.isAlive());
		assertFalse(quest.isPhantomDefeated());
		assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Sleep-Deprived TA", "Registrar Wraith", "Final Exam Phantom Jr." })
	@DisplayName("Defeating any other monster does not affect the quest")
	void defeatingOtherMonstersDoesNotUpdateQuest(String name) {
		combatEngine.attack(player, quest, new Monster(name, 1, 1, 0, List.of()));

		assertFalse(quest.isPhantomDefeated());
		assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
	}

	@Test
	@DisplayName("QuestTracker ignores a Phantom that is still alive (or a null monster)")
	void questTrackerIgnoresLivingPhantom() {
		QuestTracker tracker = new QuestTracker();
		tracker.updateQuestForMonster(quest, new BossMonster("Final Exam Phantom", 40, 10, 4, List.of(), 3));
		tracker.updateQuestForMonster(quest, null);
		assertFalse(quest.isPhantomDefeated());
		assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
	}

	@Test
	@DisplayName("Phantom name match is case-insensitive")
	void phantomNameIsCaseInsensitive() {
		combatEngine.attack(player, quest, new Monster("final exam phantom", 1, 1, 0, List.of()));
		assertTrue(quest.isPhantomDefeated());
	}

	@Test
	@DisplayName("CombatResult copies the dropped items list and exposes it read-only")
	void combatResultDroppedItemsAreDefensiveAndUnmodifiable() {
		Potion potion = new Potion("Coffee Potion", "d", 15);
		List<Item> source = new ArrayList<>(List.of(potion));
		CombatResult result = new CombatResult(true, "msg", 1, 2, true, source);
		source.clear();

		assertEquals(List.of(potion), result.getDroppedItems());
		assertThrows(UnsupportedOperationException.class, () -> result.getDroppedItems().add(potion));
	}
}
