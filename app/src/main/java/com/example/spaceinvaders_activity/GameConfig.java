package com.example.spaceinvaders_activity;

/**
 * Configuration for each game level.
 * Levels 1-100 are generated procedurally by LevelGenerator across 10 zones.
 * Difficulty scales progressively with more enemies, faster speed, and more aggressive shooting.
 */
public class GameConfig {

    public static final int MAX_LEVEL = 100;

    // Difficulty presets
    public static final int DIFF_EASY = 0;
    public static final int DIFF_NORMAL = 1;
    public static final int DIFF_HARD = 2;

    // Enemy type indices
    // 0=normal, 1=scout, 2=tank, 3=boss, 4=shielded, 5=splitter, 6=kamikaze,
    // 7=healer, 8=cloaker, 9=bomber, 10=elite
    public static final int ENEMY_TYPE_COUNT = 11;

    // Level configuration data
    public final int level;
    public final int numColumns;
    public final int numRows;
    public final float invaderBaseSpeed;
    public final int shotChance;        // Lower = more shooting
    public final int numShelters;
    public final boolean hasBoss;
    public final int bossHealth;
    public final float bossSpeed;
    public final float speedMultiplierOnDrop;
    public final int maxInvaderBullets;
    public final String levelName;
    public final int[] enemyTypeWeights;
    public final int zone;              // 0-9 zone index
    public final int bossType;          // -2=none, -1=mini, 0-9=zone boss

    // Full constructor with zone and bossType
    public GameConfig(int level, int numColumns, int numRows, float invaderBaseSpeed,
                      int shotChance, int numShelters, boolean hasBoss, int bossHealth,
                      float bossSpeed, float speedMultiplierOnDrop, int maxInvaderBullets,
                      String levelName, int[] enemyTypeWeights, int zone, int bossType) {
        this.level = level;
        this.numColumns = numColumns;
        this.numRows = numRows;
        this.invaderBaseSpeed = invaderBaseSpeed;
        this.shotChance = shotChance;
        this.numShelters = numShelters;
        this.hasBoss = hasBoss;
        this.bossHealth = bossHealth;
        this.bossSpeed = bossSpeed;
        this.speedMultiplierOnDrop = speedMultiplierOnDrop;
        this.maxInvaderBullets = maxInvaderBullets;
        this.levelName = levelName;
        this.zone = zone;
        this.bossType = bossType;

        // Pad to ENEMY_TYPE_COUNT if shorter
        if (enemyTypeWeights != null && enemyTypeWeights.length >= ENEMY_TYPE_COUNT) {
            this.enemyTypeWeights = enemyTypeWeights.clone();
        } else if (enemyTypeWeights != null) {
            this.enemyTypeWeights = new int[ENEMY_TYPE_COUNT];
            System.arraycopy(enemyTypeWeights, 0, this.enemyTypeWeights, 0, enemyTypeWeights.length);
        } else {
            this.enemyTypeWeights = new int[]{100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        }
    }

    // Legacy constructor (zone 0, no boss type) — used by survival mode
    public GameConfig(int level, int numColumns, int numRows, float invaderBaseSpeed,
                      int shotChance, int numShelters, boolean hasBoss, int bossHealth,
                      float bossSpeed, float speedMultiplierOnDrop, int maxInvaderBullets,
                      String levelName, int[] enemyTypeWeights) {
        this(level, numColumns, numRows, invaderBaseSpeed, shotChance, numShelters,
                hasBoss, bossHealth, bossSpeed, speedMultiplierOnDrop, maxInvaderBullets,
                levelName, enemyTypeWeights, 0, hasBoss ? LevelGenerator.BOSS_MINI : LevelGenerator.BOSS_NONE);
    }

    /**
     * Returns a new GameConfig with values adjusted for the given difficulty.
     */
    public GameConfig applyDifficulty(int difficulty) {
        switch (difficulty) {
            case DIFF_EASY:
                return new GameConfig(
                        level, numColumns, numRows,
                        invaderBaseSpeed * 0.75f,
                        (int) (shotChance * 1.5f),
                        numShelters, hasBoss, bossHealth, bossSpeed,
                        speedMultiplierOnDrop, maxInvaderBullets,
                        levelName, enemyTypeWeights, zone, bossType);
            case DIFF_NORMAL:
                return this;
            case DIFF_HARD:
                return new GameConfig(
                        level, numColumns, numRows,
                        invaderBaseSpeed * 1.3f,
                        Math.max(1, (int) (shotChance * 0.7f)),
                        numShelters, hasBoss,
                        (int) (bossHealth * 1.5f),
                        bossSpeed,
                        speedMultiplierOnDrop, maxInvaderBullets,
                        levelName, enemyTypeWeights, zone, bossType);
            default:
                return this;
        }
    }

    /**
     * Deterministically assigns an enemy type based on row position and the weights array.
     * Supports 11 enemy types (0-10).
     */
    public int getEnemyType(int row, int numRows) {
        // Row bias multipliers (extended to 11 types)
        // Indices: normal, scout, tank, boss, shielded, splitter, kamikaze, healer, cloaker, bomber, elite
        float[] bias;
        if (row <= 1) {
            // Front rows: favor scouts, kamikaze, cloaker
            bias = new float[]{0.5f, 2.0f, 0.3f, 1.0f, 0.3f, 0.5f, 2.0f, 0.3f, 1.5f, 0.5f, 1.2f};
        } else if (row >= numRows - 1 || (numRows > 3 && row >= numRows - 2)) {
            // Back rows: favor tanks, shielded, healers, elite
            bias = new float[]{0.8f, 0.5f, 2.0f, 1.0f, 2.0f, 0.8f, 0.3f, 2.0f, 0.5f, 1.5f, 1.5f};
        } else {
            // Middle rows: favor splitters, bombers
            bias = new float[]{1.5f, 0.8f, 0.8f, 1.0f, 0.8f, 2.0f, 0.5f, 1.0f, 1.0f, 2.0f, 0.8f};
        }

        // Compute biased weights
        float[] biasedWeights = new float[ENEMY_TYPE_COUNT];
        float totalWeight = 0;
        for (int i = 0; i < ENEMY_TYPE_COUNT; i++) {
            biasedWeights[i] = enemyTypeWeights[i] * bias[i];
            totalWeight += biasedWeights[i];
        }

        if (totalWeight <= 0) {
            return 0; // fallback to normal
        }

        // Deterministic hash-based selection
        long hash = (long) row * 31 + (long) numRows * 97 + (long) level * 53;
        hash = (hash ^ (hash >>> 16)) * 0x45d9f3bL;
        hash = hash & 0x7FFFFFFFL;
        float selector = (hash % 10000) / 10000.0f * totalWeight;

        float cumulative = 0;
        for (int i = 0; i < ENEMY_TYPE_COUNT; i++) {
            cumulative += biasedWeights[i];
            if (selector < cumulative) {
                return i;
            }
        }

        return 0;
    }

    /**
     * Get configuration for a campaign level (1-100).
     * Delegates to LevelGenerator for procedural generation.
     */
    public static GameConfig getLevel(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) return LevelGenerator.generateEndless(level);
        return LevelGenerator.generate(level);
    }

    /**
     * For survival mode (continuous waves, no bosses, increasing speed).
     */
    public static GameConfig getSurvivalWave(int wave) {
        float speed = 80f + wave * 15f;
        int shot = Math.max(150, 1000 - wave * 80);
        int cols = Math.min(7, 4 + wave / 3);
        int rows = Math.min(5, 3 + wave / 4);
        int maxBullets = Math.min(16, 6 + wave);

        int normalW = Math.max(20, 60 - wave * 4);
        int scoutW = Math.min(20, 10 + wave);
        int tankW = Math.min(15, wave * 2);
        int bossW = 0;
        int shieldedW = wave >= 3 ? Math.min(20, (wave - 3) * 3) : 0;
        int splitterW = wave >= 5 ? Math.min(15, (wave - 5) * 3) : 0;
        int kamikazeW = wave >= 4 ? Math.min(20, (wave - 4) * 3) : 0;
        int healerW = wave >= 8 ? Math.min(10, (wave - 8) * 2) : 0;
        int cloakerW = wave >= 12 ? Math.min(10, (wave - 12) * 2) : 0;
        int bomberW = wave >= 15 ? Math.min(8, (wave - 15) * 2) : 0;
        int eliteW = wave >= 18 ? Math.min(10, (wave - 18) * 2) : 0;

        return new GameConfig(wave, cols, rows, speed, shot, 2,
                false, 0, 0,
                1.10f + wave * 0.005f,
                maxBullets,
                "Wave " + wave,
                new int[]{normalW, scoutW, tankW, bossW, shieldedW, splitterW,
                        kamikazeW, healerW, cloakerW, bomberW, eliteW});
    }
}
