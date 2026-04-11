package com.example.spaceinvaders_activity;

/**
 * Configuration for each of the 10 game levels.
 * Difficulty scales progressively with more enemies, faster speed, and more aggressive shooting.
 */
public class GameConfig {

    public static final int MAX_LEVEL = 10;

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

    private GameConfig(int level, int numColumns, int numRows, float invaderBaseSpeed,
                       int shotChance, int numShelters, boolean hasBoss, int bossHealth,
                       float bossSpeed, float speedMultiplierOnDrop, int maxInvaderBullets,
                       String levelName) {
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
    }

    public int getTotalInvaders() {
        return numColumns * numRows;
    }

    public static GameConfig getLevel(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;

        switch (level) {
            case 1:
                return new GameConfig(1, 4, 3, 60f, 1200, 4,
                        false, 0, 0, 1.08f, 6,
                        "First Contact");
            case 2:
                return new GameConfig(2, 5, 3, 80f, 1000, 4,
                        false, 0, 0, 1.10f, 8,
                        "Scout Fleet");
            case 3:
                return new GameConfig(3, 5, 4, 100f, 800, 4,
                        false, 0, 0, 1.10f, 8,
                        "Invasion Begins");
            case 4:
                return new GameConfig(4, 6, 4, 120f, 650, 4,
                        false, 0, 0, 1.11f, 10,
                        "Rising Threat");
            case 5:
                return new GameConfig(5, 5, 4, 130f, 550, 3,
                        true, 15, 80f, 1.12f, 10,
                        "Commander Appears");
            case 6:
                return new GameConfig(6, 6, 5, 150f, 450, 3,
                        false, 0, 0, 1.12f, 12,
                        "Deep Space");
            case 7:
                return new GameConfig(7, 7, 5, 170f, 350, 3,
                        false, 0, 0, 1.13f, 12,
                        "Alien Armada");
            case 8:
                return new GameConfig(8, 7, 5, 190f, 280, 3,
                        false, 0, 0, 1.14f, 14,
                        "War Zone");
            case 9:
                return new GameConfig(9, 7, 6, 210f, 220, 2,
                        false, 0, 0, 1.15f, 14,
                        "Last Stand");
            case 10:
                return new GameConfig(10, 6, 5, 230f, 180, 2,
                        true, 30, 120f, 1.16f, 16,
                        "Final Battle");
            default:
                return getLevel(1);
        }
    }

    // For endless mode beyond level 10
    public static GameConfig getEndlessLevel(int level) {
        float speed = 230f + (level - 10) * 20f;
        int shot = Math.max(100, 180 - (level - 10) * 15);
        int cols = Math.min(8, 6 + (level - 10) / 3);
        int rows = Math.min(7, 5 + (level - 10) / 4);
        boolean boss = (level % 5 == 0);
        int bossHp = boss ? 30 + (level - 10) * 5 : 0;

        return new GameConfig(level, cols, rows, speed, shot, 2,
                boss, bossHp, 120f + (level - 10) * 10f,
                1.16f + (level - 10) * 0.01f,
                Math.min(20, 16 + (level - 10)),
                "Endless Wave " + (level - 10));
    }
}
