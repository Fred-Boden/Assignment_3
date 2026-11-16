package com.example.haunted.rules;

import com.example.haunted.model.Armor;
import com.example.haunted.model.BossMonster;
import com.example.haunted.model.Inventory;
import com.example.haunted.model.Monster;
import com.example.haunted.model.Player;
import com.example.haunted.model.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageCalculatorTest {

	private DamageCalculator calculator;

	@BeforeEach
	void setUp() {
		calculator = new DamageCalculator();
	}

	private static Player player(int attack, int defense) {
		return new Player("Tester", 50, attack, defense, new Inventory(5));
	}

	private static Monster monster(int attack, int defense) {
		return new Monster("Dummy", 20, attack, defense, List.of());
	}

	@ParameterizedTest(name = "player atk {0} vs monster def {1} -> {2} damage")
	@CsvSource({ "10, 3, 7", "5,  3, 2", "4,  3, 1", "3,  3, 1", "2,  3, 1", "0, 10, 1", "7,  0, 7" })
	@DisplayName("Player damage = max(1, attack - monster defense)")
	void playerDamageUsesAttackMinusDefenseWithFloorOfOne(int attack, int defense, int expected) {
		assertEquals(expected, calculator.calculatePlayerDamage(player(attack, 0), monster(0, defense)));
	}

	@ParameterizedTest(name = "base atk {0} + weapon {1} vs def {2} -> {3}")
	@CsvSource({ "7, 4, 1, 10", "7, 6, 4, 9", "1, 2, 3, 1", "1, 3, 3, 1" })
	@DisplayName("Equipped weapon bonus is added to player damage")
	void equippedWeaponIncreasesPlayerDamage(int baseAttack, int weaponBonus, int monsterDefense, int expected) {
		Player p = player(baseAttack, 0);
		p.equipWeapon(new Weapon("Stapler", "test", weaponBonus));
		assertEquals(expected, calculator.calculatePlayerDamage(p, monster(0, monsterDefense)));
	}

	@Test
	@DisplayName("Player armor does not affect damage dealt by the player")
	void playerArmorDoesNotChangeOutgoingDamage() {
		Player p = player(8, 0);
		p.equipArmor(new Armor("Shield", "test", 50));
		assertEquals(6, calculator.calculatePlayerDamage(p, monster(100, 2)));
	}

	@ParameterizedTest(name = "monster atk {0} vs player def {1} -> {2} damage")
	@CsvSource({ "9, 2, 7", "4, 2, 2", "3, 2, 1", "2, 2, 1", "1, 2, 1", "6, 0, 6" })
	@DisplayName("Monster damage = max(1, monster attack - player defense)")
	void monsterDamageUsesAttackMinusDefenseWithFloorOfOne(int attack, int defense, int expected) {
		assertEquals(expected, calculator.calculateMonsterDamage(monster(attack, 0), player(0, defense)));
	}

	@ParameterizedTest(name = "base def {0} + armor {1} vs monster atk {2} -> {3}")
	@CsvSource({ "2, 3, 9, 4", "2, 5, 10, 3", "2, 3, 5, 1", "2, 3, 6, 1", "2, 3, 7, 2" })
	@DisplayName("Equipped armor bonus reduces monster damage")
	void equippedArmorReducesMonsterDamage(int baseDefense, int armorBonus, int monsterAttack, int expected) {
		Player p = player(0, baseDefense);
		p.equipArmor(new Armor("Gown", "test", armorBonus));
		assertEquals(expected, calculator.calculateMonsterDamage(monster(monsterAttack, 0), p));
	}

	@Test
	@DisplayName("Player weapon does not reduce incoming damage")
	void playerWeaponDoesNotChangeIncomingDamage() {
		Player p = player(0, 2);
		p.equipWeapon(new Weapon("Sword", "test", 50));
		assertEquals(5, calculator.calculateMonsterDamage(monster(7, 0), p));
	}

	@ParameterizedTest(name = "boss hp {0}/40 -> damage {1}")
	@CsvSource({ "40, 8", "21, 8", "20, 11", "19, 11", "1, 11" })
	@DisplayName("Boss uses enraged attack once at or below half health")
	void bossMonsterDamageUsesCurrentAttack(int remainingHealth, int expected) {
		BossMonster boss = new BossMonster("Final Exam Phantom", 40, 10, 4, List.of(), 3);
		boss.takeDamage(40 - remainingHealth);
		assertEquals(expected, calculator.calculateMonsterDamage(boss, player(0, 2)));
	}

	@Test
	@DisplayName("Enraged boss damage still respects the floor of 1")
	void enragedBossDamageStillFloorsAtOne() {
		BossMonster boss = new BossMonster("Boss", 10, 2, 0, List.of(), 1);
		boss.takeDamage(5);
		assertEquals(1, calculator.calculateMonsterDamage(boss, player(0, 50)));
	}

	@Test
	@DisplayName("Boss defense is used when the player attacks it")
	void playerDamageAgainstBossUsesBossDefense() {
		BossMonster boss = new BossMonster("Boss", 40, 10, 4, List.of(), 3);
		assertEquals(3, calculator.calculatePlayerDamage(player(7, 0), boss));
	}
}
