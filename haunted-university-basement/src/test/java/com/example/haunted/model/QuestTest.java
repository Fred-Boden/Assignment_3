package com.example.haunted.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuestTest {

    @Test
    void newQuestStartsWithNoObjectivesCompleted() {
        Quest quest = new Quest(
            "Recover the Gradebook", "Find it and defeat the phantom"
        );

        assertEquals("Recover the Gradebook", quest.getName());
        assertEquals(
            "Find it and defeat the phantom", quest.getDescription()
        );
        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
        assertFalse(quest.isGradebookRecovered());
        assertFalse(quest.isPhantomDefeated());
        assertFalse(quest.isComplete());

        quest.updateStatus();

        assertEquals(QuestStatus.NOT_STARTED, quest.getStatus());
        assertFalse(quest.isComplete());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void requiresBothObjectivesRegardlessOfOrder(boolean gradebookFirst) {
        Quest quest = new Quest("Quest", "Complete both objectives");

        // Complete the first objective.
        if (gradebookFirst) {
            quest.markGradebookRecovered();
        } else {
            quest.markPhantomDefeated();
        }

        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
        assertEquals(gradebookFirst, quest.isGradebookRecovered());
        assertEquals(!gradebookFirst, quest.isPhantomDefeated());
        assertFalse(quest.isComplete());

        // Repeating that same objective must not complete the quest.
        if (gradebookFirst) {
            quest.markGradebookRecovered();
        } else {
            quest.markPhantomDefeated();
        }

        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
        assertFalse(quest.isComplete());

        // Complete the remaining objective.
        if (gradebookFirst) {
            quest.markPhantomDefeated();
        } else {
            quest.markGradebookRecovered();
        }

        assertTrue(quest.isGradebookRecovered());
        assertTrue(quest.isPhantomDefeated());
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertTrue(quest.isComplete());

        // Further updates must preserve completion.
        quest.markGradebookRecovered();
        quest.markPhantomDefeated();
        quest.updateStatus();

        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertTrue(quest.isComplete());
    }

    @Test
    void rejectsNullName() {
        assertThrows(
            NullPointerException.class,
            () -> new Quest(null, "Description")
        );
    }

    @Test
    void rejectsNullDescription() {
        assertThrows(
            NullPointerException.class,
            () -> new Quest("Quest", null)
        );
    }
}