package com.example.haunted.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.example.haunted.model.Item;
import com.example.haunted.model.Monster;
import com.example.haunted.model.Quest;
import com.example.haunted.model.QuestStatus;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class QuestTrackerTest {

    private static class TestItem extends Item {
        TestItem(String name) {
            super(name, "Test item");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Lost Gradebook",
        "lost gradebook",
        "LOST GRADEBOOK"
    })
    void gradebookStartsQuestRegardlessOfCapitalization(String name) {
        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");

        tracker.updateQuestForItem(quest, new TestItem(name));

        assertTrue(quest.isGradebookRecovered());
        assertFalse(quest.isPhantomDefeated());
        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Notebook",
        "Lost Gradebook Copy",
        ""
    })
    void unrelatedItemDoesNotAdvanceQuest(String name) {
        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");

        tracker.updateQuestForItem(quest, new TestItem(name));

        assertFalse(quest.isGradebookRecovered());
        assertFalse(quest.isPhantomDefeated());
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @Test
    void nullItemLeavesQuestUnchanged() {
        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");

        tracker.updateQuestForItem(quest, null);

        assertFalse(quest.isGradebookRecovered());
        assertFalse(quest.isPhantomDefeated());
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @Test
    void findingGradebookCompletesQuestWhenPhantomAlreadyDefeated() {
        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");
        quest.markPhantomDefeated();

        tracker.updateQuestForItem(
            quest, new TestItem("Lost Gradebook")
        );

        assertTrue(quest.isGradebookRecovered());
        assertTrue(quest.isPhantomDefeated());
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertTrue(quest.isComplete());
    }

    @ParameterizedTest
    @CsvSource({
        "Final Exam Phantom,      20, true,  IN_PROGRESS",
        "final exam phantom,      20, true,  IN_PROGRESS",
        "FINAL EXAM PHANTOM,      20, true,  IN_PROGRESS",
        "Final Exam Phantom,       0, false, NOT_STARTED",
        "Final Exam Phantom,      19, false, NOT_STARTED",
        "Plagiarism Ghost,        20, false, NOT_STARTED",
        "Plagiarism Ghost,         0, false, NOT_STARTED",
        "Final Exam Phantom Copy, 20, false, NOT_STARTED"
    })
    void onlyDefeatedPhantomAdvancesQuest(
            String monsterName,
            int damage,
            boolean expectedDefeated,
            QuestStatus expectedStatus) {

        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");

        Monster monster = new Monster(
            monsterName, 20, 5, 2, List.of()
        );
        monster.takeDamage(damage);

        tracker.updateQuestForMonster(quest, monster);

        assertEquals(expectedDefeated, quest.isPhantomDefeated());
        assertFalse(quest.isGradebookRecovered());
        assertEquals(expectedStatus, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @Test
    void nullMonsterLeavesQuestUnchanged() {
        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");

        tracker.updateQuestForMonster(quest, null);

        assertFalse(quest.isPhantomDefeated());
        assertFalse(quest.isGradebookRecovered());
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @Test
    void defeatingPhantomCompletesQuestWhenGradebookAlreadyRecovered() {
        QuestTracker tracker = new QuestTracker();
        Quest quest = new Quest("Quest", "Recover and defeat");
        quest.markGradebookRecovered();

        Monster phantom = new Monster(
            "Final Exam Phantom", 20, 5, 2, List.of()
        );
        phantom.takeDamage(20);

        tracker.updateQuestForMonster(quest, phantom);

        assertTrue(quest.isGradebookRecovered());
        assertTrue(quest.isPhantomDefeated());
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertTrue(quest.isComplete());
    }
}