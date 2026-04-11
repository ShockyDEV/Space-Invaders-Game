package com.example.spaceinvaders_activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all mutable game state: level, score, lives, XP, combo, active skills, timers.
 * Extracted from the engine to keep it manageable.
 */
public class GameState {

    // Game modes
    public static final int MODE_CAMPAIGN = 0;
    public static final int MODE_SURVIVAL = 1;
    public static final int MODE_TIME_ATTACK = 2;

    public int gameMode = MODE_CAMPAIGN;

    // Difficulty
    public int difficulty = GameConfig.DIFF_NORMAL;

    // Level progression
    public int currentLevel = 1;
    public GameConfig levelConfig;

    // Survival mode
    public int survivalWave = 0;
    public int survivalKillsThisWave = 0;
    public static final int SURVIVAL_KILLS_PER_WAVE = 15;

    // Time Attack mode
    public long timeAttackEndTime = 0;
    public static final long TIME_ATTACK_DURATION = 120_000; // 2 minutes
    public static final long TIME_ATTACK_DEATH_PENALTY = 10_000; // -10 seconds

    // Score & lives
    public int score = 0;
    public int lives = 3;
    public int startingLives = 3;

    // XP earned this game
    public int xpEarned = 0;
    public int enemiesKilled = 0;
    public int bossesKilled = 0;

    // Combo system
    public int comboCount = 0;
    public long lastKillTime = 0;
    public static final long COMBO_WINDOW = 2000; // 2 seconds
    public int maxCombo = 0;

    // Active skills for this game session
    public List<Integer> activeSkills = new ArrayList<>();

    // Game timer
    public long gameStartTime = 0;
    public long levelStartTime = 0;

    // State flags
    public boolean paused = true;
    public boolean levelTransition = false;
    public long levelTransitionStart = 0;
    public static final long LEVEL_TRANSITION_DURATION = 3000; // 3 seconds
    public boolean gameOver = false;
    public boolean isFirstRun = true;
    public boolean bombUsedThisLevel = false;

    // Tracking for achievements
    public int powerupsCollectedThisGame = 0;
    public int bombKillCount = 0; // kills from last bomb use

    // Power-up: freeze enemies
    public boolean enemiesFrozen = false;
    public long freezeEndTime = 0;
    public static final long FREEZE_DURATION = 4000; // 4 seconds

    // Star rating for current level
    public int starRating = 0; // 1-3

    public GameState() {
        levelConfig = GameConfig.getLevel(1);
    }

    public void startNewGame(List<Integer> skills, int extraLives) {
        startNewGame(skills, extraLives, GameConfig.DIFF_NORMAL, MODE_CAMPAIGN);
    }

    public void startNewGame(List<Integer> skills, int extraLives, int difficulty) {
        startNewGame(skills, extraLives, difficulty, MODE_CAMPAIGN);
    }

    public void startNewGame(List<Integer> skills, int extraLives, int difficulty, int mode) {
        this.difficulty = difficulty;
        this.gameMode = mode;
        currentLevel = 1;
        score = 0;
        lives = 3 + extraLives;
        if (difficulty == GameConfig.DIFF_EASY) lives += 2;
        if (difficulty == GameConfig.DIFF_HARD) lives = Math.max(1, lives - 1);
        startingLives = lives;
        xpEarned = 0;
        enemiesKilled = 0;
        bossesKilled = 0;
        comboCount = 0;
        maxCombo = 0;
        powerupsCollectedThisGame = 0;
        bombKillCount = 0;
        survivalWave = 0;
        survivalKillsThisWave = 0;
        gameStartTime = System.currentTimeMillis();
        levelStartTime = gameStartTime;
        gameOver = false;
        paused = false;
        levelTransition = false;
        bombUsedThisLevel = false;
        activeSkills = skills != null ? skills : new ArrayList<>();

        if (mode == MODE_SURVIVAL) {
            survivalWave = 1;
            levelConfig = GameConfig.getSurvivalWave(1).applyDifficulty(difficulty);
        } else if (mode == MODE_TIME_ATTACK) {
            timeAttackEndTime = gameStartTime + TIME_ATTACK_DURATION;
            levelConfig = GameConfig.getLevel(1).applyDifficulty(difficulty);
        } else {
            levelConfig = GameConfig.getLevel(1).applyDifficulty(difficulty);
        }
    }

    public void advanceLevel() {
        currentLevel++;
        if (currentLevel > GameConfig.MAX_LEVEL) {
            levelConfig = GameConfig.getEndlessLevel(currentLevel).applyDifficulty(difficulty);
        } else {
            levelConfig = GameConfig.getLevel(currentLevel).applyDifficulty(difficulty);
        }
        levelStartTime = System.currentTimeMillis();
        levelTransition = true;
        levelTransitionStart = System.currentTimeMillis();
        bombUsedThisLevel = false;
    }

    // Returns the score multiplier from combo + skills
    public int getScoreMultiplier() {
        int mult = 1;
        if (comboCount >= 3) mult = 2;
        if (comboCount >= 6) mult = 3;
        if (comboCount >= 10) mult = 4;
        return mult;
    }

    public void registerKill(int basePoints, int playerScoreMultiplier) {
        long now = System.currentTimeMillis();
        if (now - lastKillTime < COMBO_WINDOW) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastKillTime = now;
        if (comboCount > maxCombo) maxCombo = comboCount;

        int totalMult = getScoreMultiplier() * playerScoreMultiplier;
        int points = basePoints * totalMult;
        score += points;
        xpEarned += GameData.XP_PER_KILL;
        enemiesKilled++;

        // Track survival wave kills
        if (gameMode == MODE_SURVIVAL) {
            survivalKillsThisWave++;
        }
    }

    public void registerBossKill(int playerScoreMultiplier) {
        int totalMult = getScoreMultiplier() * playerScoreMultiplier;
        score += 500 * totalMult;
        xpEarned += GameData.XP_PER_BOSS_KILL;
        bossesKilled++;
        enemiesKilled++;
    }

    public void onLevelComplete() {
        xpEarned += GameData.XP_PER_LEVEL_COMPLETE * currentLevel;

        // Star rating: based on lives remaining vs starting
        float lifePercent = (float) lives / startingLives;
        long levelTime = System.currentTimeMillis() - levelStartTime;

        if (lifePercent >= 0.8f && levelTime < 60000) {
            starRating = 3;
        } else if (lifePercent >= 0.5f) {
            starRating = 2;
        } else {
            starRating = 1;
        }
    }

    public void onPlayerHit() {
        if (gameMode == MODE_TIME_ATTACK) {
            // Time attack: death costs time, not lives
            timeAttackEndTime -= TIME_ATTACK_DEATH_PENALTY;
            comboCount = 0;
            if (getRemainingTimeAttackMs() <= 0) {
                gameOver = true;
                paused = true;
            }
        } else {
            lives--;
            comboCount = 0;
            if (lives <= 0) {
                gameOver = true;
                paused = true;
            }
        }
    }

    // --- Survival mode ---

    public void advanceSurvivalWave() {
        survivalWave++;
        survivalKillsThisWave = 0;
        levelConfig = GameConfig.getSurvivalWave(survivalWave).applyDifficulty(difficulty);
        bombUsedThisLevel = false;
    }

    public void registerSurvivalKill() {
        survivalKillsThisWave++;
    }

    public boolean isSurvivalWaveComplete() {
        return survivalKillsThisWave >= SURVIVAL_KILLS_PER_WAVE;
    }

    // --- Time Attack mode ---

    public long getRemainingTimeAttackMs() {
        return timeAttackEndTime - System.currentTimeMillis();
    }

    public boolean isTimeAttackExpired() {
        return gameMode == MODE_TIME_ATTACK && System.currentTimeMillis() >= timeAttackEndTime;
    }

    public void freezeEnemies() {
        enemiesFrozen = true;
        freezeEndTime = System.currentTimeMillis() + FREEZE_DURATION;
    }

    public void updateFreeze() {
        if (enemiesFrozen && System.currentTimeMillis() > freezeEndTime) {
            enemiesFrozen = false;
        }
    }

    public long getGameDuration() {
        return System.currentTimeMillis() - gameStartTime;
    }

    public boolean isLevelTransitionDone() {
        return System.currentTimeMillis() - levelTransitionStart > LEVEL_TRANSITION_DURATION;
    }
}
