package com.example.spaceinvaders_activity;

/**
 * Configuration for each of the 10 game levels.
 * Difficulty scales progressively with more enemies, faster speed, and more aggressive shooting.
 */
public class GameConfig {

    public static final int MAX_LEVEL = 10;

    // Difficulty presets
    public static final int DIFF_EASY = 0;
    public static final int DIFF_NORMAL = 1;
    public static final int DIFF_HARD = 2;

    // Enemy type indices
    // 0=normal, 1=scout, 2=tank, 3=boss, 4=shielded, 5=splitter, 6=kamikaze
    private static final int ENEMY_TYPE_COUNT = 7;

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
    public final float speedMultiplierOnDrop; // How much faster on each drop
    public final int maxInvaderBullets;
    public final String levelName;
    public final int[] enemyTypeWeights;

    private GameConfig(int level, int numColumns, int numRows, float invaderBaseSpeed,
                       int shotChance, int numShelters, boolean hasBoss, int bossHealth,
                       float bossSpeed, float speedMultiplierOnDrop, int maxInvaderBullets,
                       String levelName, int[] enemyTypeWeights) {
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
        this.enemyTypeWeights = enemyTypeWeights != null
                ? enemyTypeWeights.clone()
                : new int[]{100, 0, 0, 0, 0, 0, 0};
    }

    public int getTotalInvaders() {
        return numColumns * numRows;
    }

    /**
     * Returns a new GameConfig with values adjusted for the given difficulty.
     *
     * @param difficulty one of DIFF_EASY, DIFF_NORMAL, or DIFF_HARD
     * @return a new GameConfig (or this if NORMAL)
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
                        levelName, enemyTypeWeights);
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
                        levelName, enemyTypeWeights);
            default:
                return this;
        }
    }

    /**
     * Deterministically assigns an enemy type based on row position and the weights array.
     * Front rows (0, 1) favour scouts/kamikaze, back rows favour tanks/shielded,
     * middle rows favour normal/splitter.
     *
     * @param row     the row index (0 = front / closest to player)
     * @param numRows the total number of rows in the grid
     * @return an enemy type index (0-6)
     */
    public int getEnemyType(int row, int numRows) {
        // Build row-biased weights from the base weights.
        // Bias multipliers per zone:
        //   front  (rows 0,1):  normal x0.5, scout x2.0, tank x0.3, boss x1, shielded x0.3, splitter x0.5, kamikaze x2.0
        //   middle:             normal x1.5, scout x0.8, tank x0.8, boss x1, shielded x0.8, splitter x2.0, kamikaze x0.5
        //   back:               normal x0.8, scout x0.5, tank x2.0, boss x1, shielded x2.0, splitter x0.8, kamikaze x0.3

        float[] bias;
        if (row <= 1) {
            // Front rows
            bias = new float[]{0.5f, 2.0f, 0.3f, 1.0f, 0.3f, 0.5f, 2.0f};
        } else if (row >= numRows - 1 || (numRows > 3 && row >= numRows - 2)) {
            // Back rows
            bias = new float[]{0.8f, 0.5f, 2.0f, 1.0f, 2.0f, 0.8f, 0.3f};
        } else {
            // Middle rows
            bias = new float[]{1.5f, 0.8f, 0.8f, 1.0f, 0.8f, 2.0f, 0.5f};
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

        // Deterministic selection using a hash of row and numRows
        // Use a simple but effective hash to get a value in [0, totalWeight)
        long hash = (long) row * 31 + (long) numRows * 97 + (long) level * 53;
        hash = (hash ^ (hash >>> 16)) * 0x45d9f3bL;
        hash = hash & 0x7FFFFFFFL; // ensure positive
        float selector = (hash % 10000) / 10000.0f * totalWeight;

        float cumulative = 0;
        for (int i = 0; i < ENEMY_TYPE_COUNT; i++) {
            cumulative += biasedWeights[i];
            if (selector < cumulative) {
                return i;
            }
        }

        return 0; // fallback
    }

    public static GameConfig getLevel(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;

        switch (level) {
            case 1:
                return new GameConfig(1, 4, 3, 60f, 1200, 4,
                        false, 0, 0, 1.08f, 6,
                        "First Contact",
                        new int[]{70, 20, 10, 0, 0, 0, 0});
            case 2:
                return new GameConfig(2, 5, 3, 80f, 1000, 4,
                        false, 0, 0, 1.10f, 8,
                        "Scout Fleet",
                        new int[]{70, 20, 10, 0, 0, 0, 0});
            case 3:
                return new GameConfig(3, 5, 4, 100f, 800, 4,
                        false, 0, 0, 1.10f, 8,
                        "Invasion Begins",
                        new int[]{70, 20, 10, 0, 0, 0, 0});
            case 4:
                return new GameConfig(4, 6, 4, 120f, 650, 4,
                        false, 0, 0, 1.11f, 10,
                        "Rising Threat",
                        new int[]{70, 20, 10, 0, 0, 0, 0});
            case 5:
                return new GameConfig(5, 5, 4, 130f, 550, 3,
                        true, 15, 80f, 1.12f, 10,
                        "Commander Appears",
                        new int[]{50, 15, 10, 0, 15, 5, 5});
            case 6:
                return new GameConfig(6, 6, 5, 150f, 450, 3,
                        false, 0, 0, 1.12f, 12,
                        "Deep Space",
                        new int[]{45, 15, 10, 0, 15, 10, 5});
            case 7:
                return new GameConfig(7, 7, 5, 170f, 350, 3,
                        false, 0, 0, 1.13f, 12,
                        "Alien Armada",
                        new int[]{40, 10, 10, 0, 15, 15, 10});
            case 8:
                return new GameConfig(8, 7, 5, 190f, 280, 3,
                        false, 0, 0, 1.14f, 14,
                        "War Zone",
                        new int[]{35, 10, 10, 0, 15, 15, 15});
            case 9:
                return new GameConfig(9, 7, 6, 210f, 220, 2,
                        false, 0, 0, 1.15f, 14,
                        "Last Stand",
                        new int[]{30, 10, 10, 0, 15, 15, 20});
            case 10:
                return new GameConfig(10, 6, 5, 230f, 180, 2,
                        true, 30, 120f, 1.16f, 16,
                        "Final Battle",
                        new int[]{25, 10, 10, 0, 20, 15, 20});
            default:
                return getLevel(1);
        }
    }

    // For survival mode (continuous waves, no bosses, increasing speed)
    public static GameConfig getSurvivalWave(int wave) {
        float speed = 80f + wave * 15f;
        int shot = Math.max(150, 1000 - wave * 80);
        int cols = Math.min(7, 4 + wave / 3);
        int rows = Math.min(5, 3 + wave / 4);
        int maxBullets = Math.min(16, 6 + wave);

        // Gradually introduce harder enemy types
        int normalW = Math.max(20, 60 - wave * 4);
        int scoutW = Math.min(20, 10 + wave);
        int tankW = Math.min(15, wave * 2);
        int bossW = 0;
        int shieldedW = wave >= 3 ? Math.min(20, (wave - 3) * 3) : 0;
        int splitterW = wave >= 5 ? Math.min(15, (wave - 5) * 3) : 0;
        int kamikazeW = wave >= 4 ? Math.min(20, (wave - 4) * 3) : 0;

        return new GameConfig(wave, cols, rows, speed, shot, 2,
                false, 0, 0,
                1.10f + wave * 0.005f,
                maxBullets,
                "Wave " + wave,
                new int[]{normalW, scoutW, tankW, bossW, shieldedW, splitterW, kamikazeW});
    }

    // For endless mode beyond level 10
    public static GameConfig getEndlessLevel(int level) {
        float speed = 230f + (level - 10) * 20f;
        int shot = Math.max(100, 180 - (level - 10) * 15);
        int cols = Math.min(8, 6 + (level - 10) / 3);
        int rows = Math.min(7, 5 + (level - 10) / 4);
        boolean boss = (level % 5 == 0);
        int bossHp = boss ? 30 + (level - 10) * 5 : 0;

        // Endless mode progressively shifts toward harder enemy types
        int normalW = Math.max(10, 25 - (level - 10));
        int scoutW = 10;
        int tankW = 10;
        int bossW = 0;
        int shieldedW = Math.min(25, 20 + (level - 10) / 2);
        int splitterW = Math.min(25, 15 + (level - 10) / 2);
        int kamikazeW = Math.min(30, 20 + (level - 10));

        return new GameConfig(level, cols, rows, speed, shot, 2,
                boss, bossHp, 120f + (level - 10) * 10f,
                1.16f + (level - 10) * 0.01f,
                Math.min(20, 16 + (level - 10)),
                "Endless Wave " + (level - 10),
                new int[]{normalW, scoutW, tankW, bossW, shieldedW, splitterW, kamikazeW});
    }
}
