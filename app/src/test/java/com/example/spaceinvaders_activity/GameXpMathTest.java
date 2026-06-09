package com.example.spaceinvaders_activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the pure XP math extracted from {@link GameData}.
 * These methods take primitives and have no SharedPreferences/Android dependency.
 */
public class GameXpMathTest {

    @Test
    public void xpForNextLevel_isLevelTimes500() {
        assertEquals(500, GameData.xpForNextLevel(1));
        assertEquals(1000, GameData.xpForNextLevel(2));
        assertEquals(5000, GameData.xpForNextLevel(10));
    }

    @Test
    public void levelUp_happensAtExactThresholds() {
        // Level 1 -> 2 at 500 total XP
        assertEquals(1, GameData.levelForTotalXP(499, 1));
        assertEquals(2, GameData.levelForTotalXP(500, 1));

        // Level 2 -> 3 at 1500 total XP (500 + 1000)
        assertEquals(2, GameData.levelForTotalXP(1499, 1));
        assertEquals(3, GameData.levelForTotalXP(1500, 1));
    }

    @Test
    public void currentLevelXP_isRemainderWithinLevel() {
        // At 500 total the player is level 2 with 0 XP into that level.
        assertEquals(0, GameData.currentLevelXP(500, 2));
        // At 700 total, level 2, that's 700 - 500 = 200 into the level.
        assertEquals(200, GameData.currentLevelXP(700, 2));
    }

    @Test
    public void currentLevelXP_isConsistentForResolvedLevels() {
        for (int total = 0; total <= 6000; total += 137) {
            int level = GameData.levelForTotalXP(total, 1);
            int within = GameData.currentLevelXP(total, level);
            assertTrue("non-negative within-level XP at total=" + total, within >= 0);
            assertTrue("within-level XP below next threshold at total=" + total,
                    within < GameData.xpForNextLevel(level));
        }
    }
}
