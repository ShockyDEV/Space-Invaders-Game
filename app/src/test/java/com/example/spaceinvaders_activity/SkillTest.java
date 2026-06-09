package com.example.spaceinvaders_activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for the {@link Skill} tree — pure logic, no Android dependencies.
 */
public class SkillTest {

    @Test
    public void allSkills_areTenWithUniqueIds() {
        List<Skill> skills = Skill.getAllSkills();
        assertEquals(10, skills.size());

        Set<Integer> ids = new HashSet<>();
        for (Skill s : skills) {
            ids.add(s.id);
        }
        assertEquals(10, ids.size());
        for (int id = 0; id <= 9; id++) {
            assertTrue("missing skill id " + id, ids.contains(id));
        }
    }

    @Test
    public void availableSkills_growWithPlayerLevel() {
        assertEquals(1, Skill.getAvailableSkills(1).size());
        assertEquals(5, Skill.getAvailableSkills(5).size());
        assertEquals(10, Skill.getAvailableSkills(10).size());
        // Beyond max level still returns the full set.
        assertEquals(10, Skill.getAvailableSkills(50).size());
    }

    @Test
    public void getSkillById_returnsExpectedOrNull() {
        Skill multiShot = Skill.getSkillById(Skill.MULTI_SHOT);
        assertNotNull(multiShot);
        assertEquals("Multi-Shot", multiShot.name);

        assertNull(Skill.getSkillById(99));
    }

    @Test
    public void maxActiveSkills_isTwo() {
        assertEquals(2, Skill.MAX_ACTIVE_SKILLS);
    }
}
