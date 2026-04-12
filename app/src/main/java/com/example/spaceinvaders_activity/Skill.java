package com.example.spaceinvaders_activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill definitions for the expanded skill tree (35 skills in 5 categories).
 * Players unlock skills by reaching the required player level AND purchasing with XP.
 * Skills are equipped using a point-budget system.
 */
public class Skill {

    // ==================== OFFENSE ====================
    public static final int RAPID_FIRE = 0;
    public static final int BIG_LASER = 1;
    public static final int MULTI_SHOT = 3;
    public static final int PIERCING_SHOT = 4;
    public static final int HOMING_MISSILES = 10;
    public static final int CHAIN_LIGHTNING = 11;
    public static final int BARRAGE_MODE = 12;
    public static final int PLASMA_CANNON = 13;

    // ==================== DEFENSE ====================
    public static final int SHIELD = 2;
    public static final int REGENERATION = 8;
    public static final int REFLECT_BARRIER = 14;
    public static final int AUTO_REPAIR = 15;
    public static final int FORTRESS_MODE = 16;
    public static final int EMERGENCY_WARP = 17;
    public static final int NANO_SHIELD = 18;

    // ==================== UTILITY ====================
    public static final int SPEED_BOOST = 5;
    public static final int MAGNET_PULL = 19;
    public static final int LUCKY_DROPS = 20;
    public static final int FREEZE_WAVE = 21;
    public static final int ALLY_DRONE = 22;
    public static final int TEMPORAL_SHIFT = 23;
    public static final int SALVAGE_BOT = 24;

    // ==================== PASSIVE ====================
    public static final int SCORE_MULTIPLIER = 7;
    public static final int COMBO_MASTER = 25;
    public static final int CRITICAL_HIT = 26;
    public static final int MOMENTUM = 27;
    public static final int SCAVENGER = 28;
    public static final int VETERANS_INSTINCT = 29;
    public static final int XP_SURGE = 30;

    // ==================== ULTIMATE ====================
    public static final int BOMB = 6;
    public static final int ULTIMATE_LASER = 9;
    public static final int BLACK_HOLE = 31;
    public static final int ALLY_SQUADRON = 32;
    public static final int TIME_STOP = 33;
    public static final int SUPERNOVA = 34;

    public static final int TOTAL_SKILLS = 35;

    // Skill data
    public final int id;
    public final String name;
    public final String description;
    public final int unlockLevel;
    public final int iconColor;
    public final int category;
    public final int pointCost;
    public final int iconResId; // drawable resource id (0 if none)

    public Skill(int id, String name, String description, int unlockLevel,
                 int iconColor, int category, int pointCost) {
        this(id, name, description, unlockLevel, iconColor, category, pointCost, 0);
    }

    public Skill(int id, String name, String description, int unlockLevel,
                 int iconColor, int category, int pointCost, int iconResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unlockLevel = unlockLevel;
        this.iconColor = iconColor;
        this.category = category;
        this.pointCost = pointCost;
        this.iconResId = iconResId;
    }

    /** Returns all 35 skills. */
    public static List<Skill> getAllSkills() {
        List<Skill> s = new ArrayList<>();

        // === OFFENSE ===
        s.add(new Skill(RAPID_FIRE, "Rapid Fire",
                "Shoot 2x faster with reduced cooldown", 1,
                0xFFFFFF00, SkillTree.CAT_OFFENSE, 1, R.drawable.skill_50000));
        s.add(new Skill(BIG_LASER, "Big Laser",
                "Wider, more powerful laser beam (2x damage)", 3,
                0xFFFF4400, SkillTree.CAT_OFFENSE, 2, R.drawable.skill_50003));
        s.add(new Skill(MULTI_SHOT, "Multi-Shot",
                "Fire 3 bullets in a spread pattern", 5,
                0xFFAA00FF, SkillTree.CAT_OFFENSE, 2, R.drawable.skill_50015));
        s.add(new Skill(PIERCING_SHOT, "Piercing Shot",
                "Bullets pass through enemies", 8,
                0xFF00FFAA, SkillTree.CAT_OFFENSE, 2, R.drawable.skill_50018));
        s.add(new Skill(HOMING_MISSILES, "Homing Missiles",
                "Every 5th shot auto-tracks nearest enemy", 15,
                0xFFFF8800, SkillTree.CAT_OFFENSE, 3, R.drawable.skill_50048));
        s.add(new Skill(CHAIN_LIGHTNING, "Chain Lightning",
                "Kills arc damage to 2 adjacent enemies", 22,
                0xFF44CCFF, SkillTree.CAT_OFFENSE, 3, R.drawable.skill_50051));
        s.add(new Skill(BARRAGE_MODE, "Barrage Mode",
                "Double-tap: 8 bullets in all directions (20s CD)", 28,
                0xFFFF2200, SkillTree.CAT_OFFENSE, 2, R.drawable.skill_50060));
        s.add(new Skill(PLASMA_CANNON, "Plasma Cannon",
                "Hold 0.5s for a 5x AoE damage shot", 38,
                0xFFDD00FF, SkillTree.CAT_OFFENSE, 3, R.drawable.skill_50063));

        // === DEFENSE ===
        s.add(new Skill(SHIELD, "Energy Shield",
                "Start with +2 extra lives", 2,
                0xFF00AAFF, SkillTree.CAT_DEFENSE, 1, R.drawable.skill_50006));
        s.add(new Skill(REGENERATION, "Regeneration",
                "Recover 1 life every 25 seconds", 10,
                0xFF00FF00, SkillTree.CAT_DEFENSE, 2, R.drawable.skill_50036));
        s.add(new Skill(REFLECT_BARRIER, "Reflect Barrier",
                "15% chance to reflect enemy bullets back", 12,
                0xFF00DDFF, SkillTree.CAT_DEFENSE, 2, R.drawable.skill_50066));
        s.add(new Skill(AUTO_REPAIR, "Auto-Repair",
                "30% chance to survive a lethal hit", 18,
                0xFF88FF88, SkillTree.CAT_DEFENSE, 1, R.drawable.skill_50075));
        s.add(new Skill(FORTRESS_MODE, "Fortress Mode",
                "Shelters take 3 hits instead of 1", 25,
                0xFF8888FF, SkillTree.CAT_DEFENSE, 3, R.drawable.skill_50078));
        s.add(new Skill(EMERGENCY_WARP, "Emergency Warp",
                "On hit, teleport to a safe position (10s CD)", 32,
                0xFFCC44FF, SkillTree.CAT_DEFENSE, 2, R.drawable.skill_50081));
        s.add(new Skill(NANO_SHIELD, "Nano Shield",
                "Auto-absorb 1 hit every 20 seconds", 42,
                0xFF44FFDD, SkillTree.CAT_DEFENSE, 2, R.drawable.skill_50090));

        // === UTILITY ===
        s.add(new Skill(SPEED_BOOST, "Speed Boost",
                "Ship moves instantly to touch position", 4,
                0xFFFFAA00, SkillTree.CAT_UTILITY, 1, R.drawable.skill_50021));
        s.add(new Skill(MAGNET_PULL, "Magnet Pull",
                "Power-ups drift toward your ship", 7,
                0xFFFFDD44, SkillTree.CAT_UTILITY, 1, R.drawable.skill_50093));
        s.add(new Skill(LUCKY_DROPS, "Lucky Drops",
                "Power-up drop rate doubled (12% to 24%)", 14,
                0xFF44FF44, SkillTree.CAT_UTILITY, 2, R.drawable.skill_50096));
        s.add(new Skill(FREEZE_WAVE, "Freeze Wave",
                "Freeze lasts 8s instead of 4s, also slows boss", 20,
                0xFF88DDFF, SkillTree.CAT_UTILITY, 2, R.drawable.skill_50105));
        s.add(new Skill(ALLY_DRONE, "Ally Drone",
                "AI ship orbits you, fires every 2s at nearest enemy", 26,
                0xFF44FF88, SkillTree.CAT_UTILITY, 3, R.drawable.skill_50108));
        s.add(new Skill(TEMPORAL_SHIFT, "Temporal Shift",
                "Enemies always move 20% slower", 35,
                0xFF8844FF, SkillTree.CAT_UTILITY, 2, R.drawable.skill_50111));
        s.add(new Skill(SALVAGE_BOT, "Salvage Bot",
                "Earn 50% more XP from kills", 16,
                0xFFCCCC00, SkillTree.CAT_UTILITY, 1, R.drawable.skill_50120));

        // === PASSIVE ===
        s.add(new Skill(SCORE_MULTIPLIER, "Score Multiplier",
                "All score gains are doubled", 6,
                0xFFFFD700, SkillTree.CAT_PASSIVE, 1, R.drawable.skill_50033));
        s.add(new Skill(COMBO_MASTER, "Combo Master",
                "Combo window extended to 3.5s (from 2s)", 9,
                0xFFFF8844, SkillTree.CAT_PASSIVE, 1, R.drawable.skill_50123));
        s.add(new Skill(CRITICAL_HIT, "Critical Hit",
                "15% chance each bullet deals 3x damage", 13,
                0xFFFF4444, SkillTree.CAT_PASSIVE, 2, R.drawable.skill_50126));
        s.add(new Skill(MOMENTUM, "Momentum",
                "Each combo kill increases bullet speed by 5%", 17,
                0xFFFFAA44, SkillTree.CAT_PASSIVE, 1, R.drawable.skill_50135));
        s.add(new Skill(SCAVENGER, "Scavenger",
                "25% chance kills drop a micro-heal", 11,
                0xFF88FF44, SkillTree.CAT_PASSIVE, 1, R.drawable.skill_50138));
        s.add(new Skill(VETERANS_INSTINCT, "Veteran's Instinct",
                "Boss health bars and weak points highlighted", 30,
                0xFFAAAAFF, SkillTree.CAT_PASSIVE, 2, R.drawable.skill_50141));
        s.add(new Skill(XP_SURGE, "XP Surge",
                "Level-complete XP reward tripled", 40,
                0xFFFFFF88, SkillTree.CAT_PASSIVE, 1, R.drawable.skill_50150));

        // === ULTIMATE ===
        s.add(new Skill(BOMB, "Orbital Bomb",
                "Tap with 2 fingers to clear all enemies (1 use/level)", 8,
                0xFFFF0000, SkillTree.CAT_ULTIMATE, 2, R.drawable.skill_50030));
        s.add(new Skill(ULTIMATE_LASER, "Ultimate Laser",
                "Hold to charge a devastating column-clearing beam (15s CD)", 10,
                0xFFFF00FF, SkillTree.CAT_ULTIMATE, 3, R.drawable.skill_50045));
        s.add(new Skill(BLACK_HOLE, "Black Hole",
                "Spawn a vortex that pulls and destroys enemies (30s CD)", 24,
                0xFF6600CC, SkillTree.CAT_ULTIMATE, 3, R.drawable.skill_50153));
        s.add(new Skill(ALLY_SQUADRON, "Ally Squadron",
                "Summon 3 ally ships for 10s that swarm enemies (45s CD)", 33,
                0xFF00CC66, SkillTree.CAT_ULTIMATE, 3, R.drawable.skill_50156));
        s.add(new Skill(TIME_STOP, "Time Stop",
                "Freeze everything for 5s, you can still shoot (60s CD)", 40,
                0xFF4488FF, SkillTree.CAT_ULTIMATE, 3, R.drawable.skill_50165));
        s.add(new Skill(SUPERNOVA, "Supernova",
                "Screen-wide explosion dealing 10 damage to all (1 use/game)", 48,
                0xFFFFAA00, SkillTree.CAT_ULTIMATE, 3, R.drawable.skill_50168));

        return s;
    }

    public static Skill getSkillById(int id) {
        for (Skill sk : getAllSkills()) {
            if (sk.id == id) return sk;
        }
        return null;
    }

    public static List<Skill> getAvailableSkills(int playerLevel) {
        List<Skill> available = new ArrayList<>();
        for (Skill sk : getAllSkills()) {
            if (sk.unlockLevel <= playerLevel) {
                available.add(sk);
            }
        }
        return available;
    }

    public static List<Skill> getSkillsByCategory(int category) {
        List<Skill> result = new ArrayList<>();
        for (Skill sk : getAllSkills()) {
            if (sk.category == category) {
                result.add(sk);
            }
        }
        return result;
    }
}
