package com.example.haunted.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthBoundaryTest {

	private static Player newPlayer() {
		return new Player("Tester", 50, 7, 2, new Inventory(5));
	}

	@ParameterizedTest(name = "player takes {0} damage -> {1} HP, alive={2}")
	@CsvSource({ "0,   50, true", "1,   49, true", "49,   1, true", "50,   0, false", "51,   0, false",
			"500,  0, false", "-5,  50, true", "-1,  50, true" })
	@DisplayName("Player.takeDamage clamps health to [0, max] and ignores negatives")
	void playerTakeDamageBoundaries(int amount, int expectedHealth, boolean expectedAlive) {
		Player p = newPlayer();
		p.takeDamage(amount);
		assertEquals(expectedHealth, p.getHealth());
		assertEquals(expectedAlive, p.isAlive());
	}

	@ParameterizedTest(name = "player at {0} HP heals {1} -> {2}")
	@CsvSource({ "20, 10, 40", "20, 30, 50", "20, 31, 50", "20, 100, 50", "20, 0, 30", "20, -10, 30", "1, 1, 50",
			"49, 1, 2" })
	@DisplayName("Player.heal caps at max health and ignores negatives")
	void playerHealBoundaries(int damageTaken, int healAmount, int expectedHealth) {
		Player p = newPlayer();
		p.takeDamage(damageTaken);
		p.heal(healAmount);
		assertEquals(expectedHealth, p.getHealth());
	}

	@Test
	@DisplayName("A new player starts at full health and is alive")
	void newPlayerStartsAtMaxHealth() {
		Player p = newPlayer();
		assertEquals(50, p.getHealth());
		assertEquals(50, p.getMaxHealth());
		assertEquals(7, p.getBaseAttack());
		assertEquals(2, p.getBaseDefense());
		assertTrue(p.isAlive());
	}

	@Test
	@DisplayName("Healing a player at 0 HP restores health without changing max health")
	void healAfterDeathRestoresHealthButMaxUnchanged() {
		Player p = newPlayer();
		p.takeDamage(50);
		assertFalse(p.isAlive());
		p.heal(15);
		assertEquals(15, p.getHealth());
		assertEquals(50, p.getMaxHealth());
	}

	@Test
	@DisplayName("Attack and defense power include equipped gear, and gear can be replaced")
	void attackAndDefensePowerIncludeGear() {
		Player p = newPlayer();
		assertEquals(7, p.getAttackPower());
		assertEquals(2, p.getDefensePower());

		p.equipWeapon(new Weapon("Stapler", "d", 4));
		p.equipArmor(new Armor("Shield", "d", 3));
		assertEquals(11, p.getAttackPower());
		assertEquals(5, p.getDefensePower());

		p.equipWeapon(new Weapon("Sword", "d", 6));
		p.equipArmor(new Armor("Gown", "d", 5));
		assertEquals(13, p.getAttackPower());
		assertEquals(7, p.getDefensePower());
	}

	@Test
	@DisplayName("Equipping null gear is rejected")
	void equippingNullThrows() {
		Player p = newPlayer();
		assertThrows(NullPointerException.class, () -> p.equipWeapon(null));
		assertThrows(NullPointerException.class, () -> p.equipArmor(null));
	}

	@ParameterizedTest(name = "monster (18 HP) takes {0} -> {1} HP, alive={2}")
	@CsvSource({ "0,  18, true", "17,  1, true", "18,  0, false", "19,  0, false", "-3, 18, true" })
	@DisplayName("Monster.takeDamage clamps health and ignores negatives")
	void monsterTakeDamageBoundaries(int amount, int expectedHealth, boolean expectedAlive) {
		Monster m = new Monster("Sleep-Deprived TA", 18, 6, 1, List.of());
		m.takeDamage(amount);
		assertEquals(expectedHealth, m.getHealth());
		assertEquals(18, m.getMaxHealth());
		assertEquals(expectedAlive, m.isAlive());
	}

	@Test
	@DisplayName("Monster loot is a defensive, unmodifiable copy")
	void monsterLootIsUnmodifiable() {
		Potion potion = new Potion("Coffee Potion", "d", 15);
		java.util.ArrayList<Item> source = new java.util.ArrayList<>(List.of(potion));
		Monster m = new Monster("TA", 18, 6, 1, source);
		source.clear();

		assertEquals(List.of(potion), m.getLoot());
		assertThrows(UnsupportedOperationException.class, () -> m.getLoot().add(potion));
	}

	@ParameterizedTest(name = "boss max {0}, damage {1} -> attack {2}")
	@CsvSource({ "40, 0, 10", "40, 19, 10", "40, 20, 13", "40, 39, 13", "41, 20, 10", "41, 21, 13" })
	@DisplayName("BossMonster enrages at or below half its max health")
	void bossEnrageThreshold(int maxHealth, int damage, int expectedAttack) {
		BossMonster boss = new BossMonster("Final Exam Phantom", maxHealth, 10, 4, List.of(), 3);
		boss.takeDamage(damage);
		assertEquals(expectedAttack, boss.getCurrentAttack());
		assertEquals(10, boss.getAttack());
	}
}
