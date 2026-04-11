package com.example.spaceinvaders_activity;

/**
 * Procedural level generator for 100 campaign levels across 10 zones.
 * Replaces the old hand-crafted switch/case with a formulaic approach.
 * Each zone (10 levels) has a theme, environment effect, and boss.
 */
public class LevelGenerator {

    public static final int MAX_LEVEL = 100;
    public static final int LEVELS_PER_ZONE = 10;
    public static final int ZONE_COUNT = 10;

    // Zone names
    private static final String[] ZONE_NAMES = {
            "Near Earth",       // Zone 0 (L1-10)
            "Asteroid Belt",    // Zone 1 (L11-20)
            "Nebula",           // Zone 2 (L21-30)
            "Solar Flare",      // Zone 3 (L31-40)
            "Ice Field",        // Zone 4 (L41-50)
            "Electric Storm",   // Zone 5 (L51-60)
            "Dark Matter",      // Zone 6 (L61-70)
            "Alien Hive",       // Zone 7 (L71-80)
            "Warp Zone",        // Zone 8 (L81-90)
            "Final Frontier"    // Zone 9 (L91-100)
    };

    // Sub-level names within each zone (position 0-9)
    private static final String[] LEVEL_SUBTITLES = {
            "Arrival",
            "Recon",
            "First Wave",
            "Escalation",
            "Ambush",         // mini-boss at position 4 (every X5 level)
            "Deep Patrol",
            "Reinforcements",
            "Siege",
            "Onslaught",
            "Showdown"        // boss at position 9 (every X0 level)
    };

    // Boss type constants (matches zone index)
    public static final int BOSS_NONE = -2;
    public static final int BOSS_MINI = -1;
    public static final int BOSS_SENTINEL = 0;
    public static final int BOSS_ROCK_GOLEM = 1;
    public static final int BOSS_NEBULA_WRAITH = 2;
    public static final int BOSS_SOLAR_DRAKE = 3;
    public static final int BOSS_CRYO_TITAN = 4;
    public static final int BOSS_STORM_LORD = 5;
    public static final int BOSS_VOID_REAPER = 6;
    public static final int BOSS_HIVE_QUEEN = 7;
    public static final int BOSS_CHRONO_LORD = 8;
    public static final int BOSS_OVERLORD = 9;

    /**
     * Generate a GameConfig for the given campaign level (1-100+).
     * Levels beyond 100 use endless scaling.
     */
    public static GameConfig generate(int level) {
        if (level < 1) level = 1;

        int zone = getZone(level);
        int posInZone = (level - 1) % LEVELS_PER_ZONE;

        // Grid size: grows from 4x3 to 8x7
        int cols = Math.min(8, 4 + (level - 1) / 12);
        int rows = Math.min(7, 3 + (level - 1) / 16);

        // Speed: 60 at L1, grows ~2.5/level, caps at 310
        float speed = Math.min(310f, 60f + (level - 1) * 2.5f);

        // Shot chance (lower = more shots): 1200 at L1, down to 80 at L100
        int shotChance = Math.max(80, 1200 - (level - 1) * 11);

        // Shelters: decrease in later zones
        int shelters;
        if (zone <= 2) shelters = 4;
        else if (zone <= 5) shelters = 3;
        else shelters = 2;

        // Boss at every 10th level, mini-boss at every 5th (excluding 10ths)
        boolean isBossLevel = (level % LEVELS_PER_ZONE == 0);
        boolean isMiniBoss = (level % 5 == 0 && !isBossLevel);
        boolean hasBoss = isBossLevel || isMiniBoss;

        int bossHP;
        float bossSpd;
        int bossType;
        if (isBossLevel) {
            bossHP = 15 + zone * 8;
            bossSpd = 80f + zone * 15f;
            bossType = zone;
        } else if (isMiniBoss) {
            bossHP = 8 + zone * 3;
            bossSpd = 100f + zone * 10f;
            bossType = BOSS_MINI;
        } else {
            bossHP = 0;
            bossSpd = 0;
            bossType = BOSS_NONE;
        }

        // Speed multiplier on drop: 1.08 to 1.20
        float speedMult = 1.08f + level * 0.0012f;

        // Max invader bullets: 6 to 20
        int maxBullets = Math.min(20, 6 + level / 5);

        // Enemy type weights
        int[] weights = computeEnemyWeights(level, zone);

        // Level name
        String name = ZONE_NAMES[Math.min(zone, ZONE_COUNT - 1)] + " - " + LEVEL_SUBTITLES[posInZone];

        return new GameConfig(level, cols, rows, speed, shotChance,
                shelters, hasBoss, bossHP, bossSpd, speedMult,
                maxBullets, name, weights, zone, bossType);
    }

    /**
     * Generate a config for endless mode (levels beyond 100).
     * Same formula but uncapped speed scaling.
     */
    public static GameConfig generateEndless(int level) {
        if (level <= MAX_LEVEL) return generate(level);

        int overflow = level - MAX_LEVEL;

        int cols = 8;
        int rows = 7;
        float speed = Math.min(400f, 310f + overflow * 3f);
        int shotChance = Math.max(50, 80 - overflow * 2);
        int shelters = 2;

        boolean isBoss = (level % 5 == 0);
        int bossHP = isBoss ? 95 + overflow * 5 : 0;
        float bossSpd = isBoss ? 200f + overflow * 5f : 0;
        int bossType = isBoss ? (level / 10) % ZONE_COUNT : BOSS_NONE;

        float speedMult = 1.20f + overflow * 0.002f;
        int maxBullets = 20;

        // Very diverse enemy weights for endless
        int[] weights = new int[]{
                10, 10, 10, 0, 15, 15, 15,
                8, 7, 5, 5
        };

        String name = "Endless - Wave " + overflow;
        int zone = 9;

        return new GameConfig(level, cols, rows, speed, shotChance,
                shelters, isBoss, bossHP, bossSpd, speedMult,
                maxBullets, name, weights, zone, bossType);
    }

    /**
     * Compute enemy type weights based on level progression.
     * Returns an 11-element array for types 0-10.
     */
    private static int[] computeEnemyWeights(int level, int zone) {
        float progress = (level - 1) / 99f; // 0.0 at L1, 1.0 at L100

        // Base types (always present, shift from normal-heavy to balanced)
        int normal   = (int) (70 * (1f - progress * 0.7f));   // 70 -> 21
        int scout    = (int) (10 + 10 * progress);            // 10 -> 20
        int tank     = (int) (5 + 15 * progress);             // 5  -> 20
        int boss     = 0; // Never spawned as regular enemy
        int shielded = level >= 10 ? (int) (15 * progress) : 0;  // 0 -> 15
        int splitter = level >= 15 ? (int) (12 * progress) : 0;  // 0 -> 12
        int kamikaze = level >= 20 ? (int) (15 * progress) : 0;  // 0 -> 15

        // New types introduced in later zones
        int healer   = level >= 21 ? (int) (8 * progress) : 0;   // Zone 3+
        int cloaker  = level >= 41 ? (int) (10 * progress) : 0;  // Zone 5+
        int bomber   = level >= 51 ? (int) (8 * progress) : 0;   // Zone 6+
        int elite    = level >= 61 ? (int) (10 * progress) : 0;  // Zone 7+

        return new int[]{normal, scout, tank, boss, shielded, splitter,
                kamikaze, healer, cloaker, bomber, elite};
    }

    /**
     * Returns the zone index (0-9) for a given level.
     */
    public static int getZone(int level) {
        return Math.min(ZONE_COUNT - 1, (level - 1) / LEVELS_PER_ZONE);
    }

    /**
     * Returns the zone display name.
     */
    public static String getZoneName(int zone) {
        if (zone < 0 || zone >= ZONE_COUNT) return "Unknown";
        return ZONE_NAMES[zone];
    }
}
