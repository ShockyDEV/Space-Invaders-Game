package com.example.spaceinvaders_activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages achievement definitions and unlock logic.
 * Checks game state and persistent data to determine which achievements
 * have been earned, and returns newly unlocked ones.
 */
public class AchievementManager {

    private final GameData gameData;

    // --- Inner class ---

    public static class Achievement {
        public final int id;
        public final String name;
        public final String description;
        public final int xpReward;

        public Achievement(int id, String name, String description, int xpReward) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.xpReward = xpReward;
        }
    }

    // --- Achievement definitions ---

    public static List<Achievement> getAllAchievements() {
        List<Achievement> achievements = new ArrayList<>();
        // Original achievements (IDs 0-14)
        achievements.add(new Achievement(0, "First Blood", "Destroy your first enemy", 50));
        achievements.add(new Achievement(1, "Centurion", "Destroy 100 total enemies", 200));
        achievements.add(new Achievement(2, "Sharpshooter", "Reach combo x10 in a single game", 300));
        achievements.add(new Achievement(3, "Combo King", "Reach combo x20 in a single game", 500));
        achievements.add(new Achievement(4, "Boss Slayer", "Defeat a boss", 200));
        achievements.add(new Achievement(5, "Boss Hunter", "Defeat 5 bosses total", 500));
        achievements.add(new Achievement(6, "Survivor", "Complete level 5", 300));
        achievements.add(new Achievement(7, "Veteran", "Complete level 10", 500));
        achievements.add(new Achievement(8, "Untouchable", "Complete a level without losing a life", 400));
        achievements.add(new Achievement(9, "Speed Demon", "Complete a level in under 30 seconds", 300));
        achievements.add(new Achievement(10, "High Roller", "Reach score 10,000", 200));
        achievements.add(new Achievement(11, "Score Legend", "Reach score 50,000", 500));
        achievements.add(new Achievement(12, "Bomb Master", "Kill 20+ enemies with a single bomb", 300));
        achievements.add(new Achievement(13, "Endless Warrior", "Reach level 15", 500));
        achievements.add(new Achievement(14, "Collector", "Collect 50 total power-ups", 200));

        // Zone completion achievements (IDs 15-24)
        achievements.add(new Achievement(15, "Asteroid Dodger", "Clear Zone 2 (Level 20)", 400));
        achievements.add(new Achievement(16, "Nebula Navigator", "Clear Zone 3 (Level 30)", 500));
        achievements.add(new Achievement(17, "Solar Survivor", "Clear Zone 4 (Level 40)", 600));
        achievements.add(new Achievement(18, "Ice Breaker", "Clear Zone 5 (Level 50)", 700));
        achievements.add(new Achievement(19, "Storm Chaser", "Clear Zone 6 (Level 60)", 800));
        achievements.add(new Achievement(20, "Void Walker", "Clear Zone 7 (Level 70)", 900));
        achievements.add(new Achievement(21, "Hive Destroyer", "Clear Zone 8 (Level 80)", 1000));
        achievements.add(new Achievement(22, "Warp Master", "Clear Zone 9 (Level 90)", 1200));
        achievements.add(new Achievement(23, "Galaxy Savior", "Complete Level 100", 2000));

        // Expanded milestones (IDs 24-34)
        achievements.add(new Achievement(24, "Destroyer", "Destroy 500 total enemies", 500));
        achievements.add(new Achievement(25, "Annihilator", "Destroy 2000 total enemies", 1000));
        achievements.add(new Achievement(26, "Score Master", "Reach score 100,000", 800));
        achievements.add(new Achievement(27, "Score God", "Reach score 500,000", 1500));
        achievements.add(new Achievement(28, "Combo Legend", "Reach combo x50 in a single game", 1000));
        achievements.add(new Achievement(29, "Boss Nemesis", "Defeat 10 bosses total", 800));
        achievements.add(new Achievement(30, "Skill Collector", "Purchase 10 skills", 500));
        achievements.add(new Achievement(31, "Skill Master", "Purchase 25 skills", 1000));
        achievements.add(new Achievement(32, "Power Hoarder", "Collect 200 total power-ups", 600));
        achievements.add(new Achievement(33, "Marathon Runner", "Play for 30 minutes in a single game", 500));
        achievements.add(new Achievement(34, "Perfectionist", "Complete 5 levels without losing a life", 800));
        return achievements;
    }

    // --- Constructor ---

    public AchievementManager(GameData gameData) {
        this.gameData = gameData;
    }

    // --- Instance convenience method ---

    public List<Achievement> getAll() {
        return getAllAchievements();
    }

    // --- Unlock checking ---

    /**
     * Checks all achievement conditions against the current game state and
     * persistent data. Returns a list of achievements that were NEWLY unlocked
     * by this call (i.e., not previously unlocked).
     */
    public List<Achievement> checkAndUnlock(GameState state, GameData data) {
        List<Achievement> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : getAllAchievements()) {
            if (data.isAchievementUnlocked(achievement.id)) {
                continue;
            }

            boolean earned = false;

            switch (achievement.id) {
                case 0: // First Blood - Destroy your first enemy
                    earned = state.enemiesKilled >= 1 || data.getTotalKills() >= 1;
                    break;

                case 1: // Centurion - Destroy 100 total enemies
                    earned = data.getTotalKills() >= 100;
                    break;

                case 2: // Sharpshooter - Reach combo x10 in a single game
                    earned = state.maxCombo >= 10;
                    break;

                case 3: // Combo King - Reach combo x20 in a single game
                    earned = state.maxCombo >= 20;
                    break;

                case 4: // Boss Slayer - Defeat a boss
                    earned = state.bossesKilled >= 1;
                    break;

                case 5: // Boss Hunter - Defeat 5 bosses total
                    earned = state.bossesKilled >= 5;
                    break;

                case 6: // Survivor - Complete level 5
                    earned = state.currentLevel > 5;
                    break;

                case 7: // Veteran - Complete level 10
                    earned = state.currentLevel > 10;
                    break;

                case 8: // Untouchable - Complete a level without losing a life
                    earned = state.lives >= state.startingLives;
                    break;

                case 9: // Speed Demon - Complete a level in under 30 seconds
                    long levelTime = System.currentTimeMillis() - state.levelStartTime;
                    earned = levelTime < 30_000 && state.levelStartTime > 0;
                    break;

                case 10: // High Roller - Reach score 10,000
                    earned = state.score >= 10_000 || data.getHighScore() >= 10_000;
                    break;

                case 11: // Score Legend - Reach score 50,000
                    earned = state.score >= 50_000 || data.getHighScore() >= 50_000;
                    break;

                case 12: // Bomb Master - Kill 20+ enemies with a single bomb
                    earned = state.bombKillCount >= 20;
                    break;

                case 13: // Endless Warrior - Reach level 15
                    earned = state.currentLevel >= 15;
                    break;

                case 14: // Collector - Collect 50 total power-ups
                    earned = data.getTotalPowerups() >= 50;
                    break;

                // Zone completion achievements
                case 15: earned = state.currentLevel > 20 || data.getHighestLevel() > 20; break;
                case 16: earned = state.currentLevel > 30 || data.getHighestLevel() > 30; break;
                case 17: earned = state.currentLevel > 40 || data.getHighestLevel() > 40; break;
                case 18: earned = state.currentLevel > 50 || data.getHighestLevel() > 50; break;
                case 19: earned = state.currentLevel > 60 || data.getHighestLevel() > 60; break;
                case 20: earned = state.currentLevel > 70 || data.getHighestLevel() > 70; break;
                case 21: earned = state.currentLevel > 80 || data.getHighestLevel() > 80; break;
                case 22: earned = state.currentLevel > 90 || data.getHighestLevel() > 90; break;
                case 23: earned = state.currentLevel > 100 || data.getHighestLevel() > 100; break;

                // Expanded milestones
                case 24: earned = data.getTotalKills() >= 500; break;
                case 25: earned = data.getTotalKills() >= 2000; break;
                case 26: earned = state.score >= 100_000 || data.getHighScore() >= 100_000; break;
                case 27: earned = state.score >= 500_000 || data.getHighScore() >= 500_000; break;
                case 28: earned = state.maxCombo >= 50; break;
                case 29: earned = state.bossesKilled >= 10; break;
                case 30: earned = data.getPurchasedSkillCount() >= 10; break;
                case 31: earned = data.getPurchasedSkillCount() >= 25; break;
                case 32: earned = data.getTotalPowerups() >= 200; break;
                case 33: earned = state.getGameDuration() >= 30 * 60 * 1000L; break;
                case 34: earned = state.perfectLevelCount >= 5; break;
            }

            if (earned) {
                data.unlockAchievement(achievement.id);
                newlyUnlocked.add(achievement);
            }
        }

        return newlyUnlocked;
    }
}
