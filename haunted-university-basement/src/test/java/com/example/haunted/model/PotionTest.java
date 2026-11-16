package com.example.haunted.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PotionTest {

    @Test
    void storesPotionProperties() {
        Potion potion = new Potion(
            "Coffee Potion", "Restores health", 20
        );

        assertEquals("Coffee Potion", potion.getName());
        assertEquals("Restores health", potion.getDescription());
        assertEquals(20, potion.getHealingAmount());
    }

    @ParameterizedTest(
        name = "damage={0}, healing={1}, expected health={2}"
    )
    @CsvSource({
        "40, 20, 80",
        "40, 39, 99",
        "40, 40, 100",
        "40, 41, 100",
        "0,  20, 100",
        "40,  0, 60",
        "40, -5, 60",
        "99,  1, 2"
    })
    void usingPotionRestoresExpectedHealth(
            int damage, int healingAmount, int expectedHealth) {

        // create a player and reduce their health.
        Player player = new Player(
            "Student", 100, 10, 5, new Inventory(3)
        );
        player.takeDamage(damage);

        Potion potion = new Potion(
            "Coffee Potion", "Restores health", healingAmount
        );

        // use the potion.
        potion.use(player);

        // verify the exact resulting health.
        assertEquals(expectedHealth, player.getHealth());
        assertEquals(100, player.getMaxHealth());
    }
}