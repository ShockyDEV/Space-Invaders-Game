package com.example.spaceinvaders_activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link GameConfig} — pure level configuration logic, no Android dependencies.
 */
public class GameConfigTest {

    @Test
    public void getLevel_clampsBelowOne() {
        assertEquals(1, GameConfig.getLevel(0).level);
        assertEquals(1, GameConfig.getLevel(-5).level);
    }

    @Test
    public void getLevel_clampsAboveMax() {
        assertEquals(GameConfig.MAX_LEVEL, GameConfig.getLevel(99).level);
    }

    @Test
    public void bossLevels_haveBossAndHealth() {
        GameConfig l5 = GameConfig.getLevel(5);
        assertTrue(l5.hasBoss);
        assertEquals(15, l5.bossHealth);

        GameConfig l10 = GameConfig.getLevel(10);
        assertTrue(l10.hasBoss);
        assertEquals(30, l10.bossHealth);
    }

    @Test
    public void nonBossLevels_haveNoBoss() {
        GameConfig l1 = GameConfig.getLevel(1);
        assertFalse(l1.hasBoss);
        assertEquals(0, l1.bossHealth);
    }

    @Test
    public void totalInvaders_isColumnsTimesRows() {
        for (int lvl = 1; lvl <= GameConfig.MAX_LEVEL; lvl++) {
            GameConfig c = GameConfig.getLevel(lvl);
            assertEquals(c.numColumns * c.numRows, c.getTotalInvaders());
        }
    }

    @Test
    public void endlessLevels_scaleWithinBounds() {
        float previousSpeed = GameConfig.getLevel(GameConfig.MAX_LEVEL).invaderBaseSpeed;
        for (int lvl = 11; lvl <= 30; lvl++) {
            GameConfig c = GameConfig.getEndlessLevel(lvl);
            assertTrue("speed should increase", c.invaderBaseSpeed > previousSpeed);
            previousSpeed = c.invaderBaseSpeed;

            assertTrue("shotChance floored at 100", c.shotChance >= 100);
            assertTrue("columns capped at 8", c.numColumns <= 8);
            assertTrue("rows capped at 7", c.numRows <= 7);
            assertTrue("bullets capped at 20", c.maxInvaderBullets <= 20);
        }
    }

    @Test
    public void endlessLevels_haveBossEveryFifthLevel() {
        assertTrue(GameConfig.getEndlessLevel(15).hasBoss);
        assertTrue(GameConfig.getEndlessLevel(20).hasBoss);
        assertFalse(GameConfig.getEndlessLevel(16).hasBoss);
    }
}
