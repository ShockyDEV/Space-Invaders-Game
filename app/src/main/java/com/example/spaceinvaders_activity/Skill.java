package com.example.spaceinvaders_activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill tree system. Players unlock skills as they level up
 * and can select active skills before each game.
 */
public class Skill {

    // Skill IDs
    public static final int RAPID_FIRE = 0;
    public static final int BIG_LASER = 1;
    public static final int SHIELD = 2;
    public static final int MULTI_SHOT = 3;
    public static final int PIERCING_SHOT = 4;
    public static final int SPEED_BOOST = 5;
    public static final int BOMB = 6;
    public static final int SCORE_MULTIPLIER = 7;
    public static final int REGENERATION = 8;
    public static final int ULTIMATE_LASER = 9;

    public static final int MAX_ACTIVE_SKILLS = 2;

    public final int id;
    public final String name;
    public final String description;
    public final int unlockLevel;
    public final int iconColor; // ARGB color for UI representation

    private Skill(int id, String name, String description, int unlockLevel, int iconColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unlockLevel = unlockLevel;
        this.iconColor = iconColor;
    }

    public static List<Skill> getAllSkills() {
        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill(RAPID_FIRE, "Rapid Fire",
                "Shoot 2x faster with reduced cooldown", 1,
                0xFFFFFF00)); // Yellow

        skills.add(new Skill(BIG_LASER, "Big Laser",
                "Wider, more powerful laser beam", 2,
                0xFFFF4400)); // Orange-red

        skills.add(new Skill(SHIELD, "Energy Shield",
                "Start with +2 extra lives", 3,
                0xFF00AAFF)); // Blue

        skills.add(new Skill(MULTI_SHOT, "Multi-Shot",
                "Fire 3 bullets in a spread pattern", 4,
                0xFFAA00FF)); // Purple

        skills.add(new Skill(PIERCING_SHOT, "Piercing Shot",
                "Bullets pass through enemies", 5,
                0xFF00FFAA)); // Teal

        skills.add(new Skill(SPEED_BOOST, "Speed Boost",
                "Ship moves 50% faster", 6,
                0xFFFFAA00)); // Gold

        skills.add(new Skill(BOMB, "Orbital Bomb",
                "Tap with 2 fingers to clear all visible enemies (1 use)", 7,
                0xFFFF0000)); // Red

        skills.add(new Skill(SCORE_MULTIPLIER, "Score Multiplier",
                "All score gains are doubled", 8,
                0xFFFFD700)); // Gold

        skills.add(new Skill(REGENERATION, "Regeneration",
                "Recover 1 life every 30 seconds", 9,
                0xFF00FF00)); // Green

        skills.add(new Skill(ULTIMATE_LASER, "Ultimate Laser",
                "Hold to charge a devastating beam that clears a column", 10,
                0xFFFF00FF)); // Magenta
        return skills;
    }

    public static Skill getSkillById(int id) {
        for (Skill s : getAllSkills()) {
            if (s.id == id) return s;
        }
        return null;
    }

    public static List<Skill> getAvailableSkills(int playerLevel) {
        List<Skill> available = new ArrayList<>();
        for (Skill s : getAllSkills()) {
            if (s.unlockLevel <= playerLevel) {
                available.add(s);
            }
        }
        return available;
    }
}
