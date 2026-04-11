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
            }

            if (earned) {
                data.unlockAchievement(achievement.id);
                newlyUnlocked.add(achievement);
            }
        }

        return newlyUnlocked;
    }
}
