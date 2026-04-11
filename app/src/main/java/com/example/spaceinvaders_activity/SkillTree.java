package com.example.spaceinvaders_activity;

import java.util.List;

/**
 * Manages the skill point budget system.
 * Players earn skill points as they level up and spend them equipping skills.
 * Each skill has a point cost (1-3). The total equipped cost must fit the budget.
 * Skills must also be purchased with XP before they can be equipped.
 */
public class SkillTree {

    // Skill categories
    public static final int CAT_OFFENSE = 0;
    public static final int CAT_DEFENSE = 1;
    public static final int CAT_UTILITY = 2;
    public static final int CAT_PASSIVE = 3;
    public static final int CAT_ULTIMATE = 4;

    public static final int CATEGORY_COUNT = 5;

    private static final String[] CATEGORY_NAMES = {
            "Offense", "Defense", "Utility", "Passive", "Ultimate"
    };

    private static final int[] CATEGORY_COLORS = {
            0xFFFF4444, // Offense - Red
            0xFF4488FF, // Defense - Blue
            0xFF44FF88, // Utility - Green
            0xFFFFAA44, // Passive - Orange
            0xFFCC44FF  // Ultimate - Purple
    };

    /**
     * Calculate skill point budget based on player level.
     * Starts at 1 point, gains +1 every 3 levels.
     * Level 1: 1pt, Level 3: 2pt, Level 6: 3pt, Level 30: 11pt, Level 53: 18pt
     */
    public static int getSkillPointBudget(int playerLevel) {
        return 1 + playerLevel / 3;
    }

    /**
     * Calculate the XP cost to purchase (unlock) a skill permanently.
     * Cost = pointCost * 200 XP.
     */
    public static int getXPCost(Skill skill) {
        return skill.pointCost * 200;
    }

    /**
     * Check if a loadout of skills fits within the point budget.
     */
    public static boolean isValidLoadout(List<Integer> selectedSkillIds, int playerLevel) {
        int budget = getSkillPointBudget(playerLevel);
        int totalCost = 0;
        for (int skillId : selectedSkillIds) {
            Skill skill = Skill.getSkillById(skillId);
            if (skill == null) return false;
            if (skill.unlockLevel > playerLevel) return false;
            totalCost += skill.pointCost;
        }
        return totalCost <= budget;
    }

    /**
     * Calculate total point cost of a set of skills.
     */
    public static int getTotalCost(List<Integer> selectedSkillIds) {
        int total = 0;
        for (int skillId : selectedSkillIds) {
            Skill skill = Skill.getSkillById(skillId);
            if (skill != null) total += skill.pointCost;
        }
        return total;
    }

    /**
     * Get the remaining points available after equipping the given skills.
     */
    public static int getRemainingPoints(List<Integer> selectedSkillIds, int playerLevel) {
        return getSkillPointBudget(playerLevel) - getTotalCost(selectedSkillIds);
    }

    /**
     * Check if a specific skill can be added to the current loadout.
     */
    public static boolean canAddSkill(int skillId, List<Integer> currentLoadout, int playerLevel,
                                       GameData gameData) {
        Skill skill = Skill.getSkillById(skillId);
        if (skill == null) return false;
        if (skill.unlockLevel > playerLevel) return false;
        if (!gameData.isSkillPurchased(skillId)) return false;
        if (currentLoadout.contains(skillId)) return false;

        int remaining = getRemainingPoints(currentLoadout, playerLevel);
        return skill.pointCost <= remaining;
    }

    /**
     * Check if a skill can be purchased (player has enough level and XP).
     */
    public static boolean canPurchaseSkill(int skillId, int playerLevel, GameData gameData) {
        Skill skill = Skill.getSkillById(skillId);
        if (skill == null) return false;
        if (skill.unlockLevel > playerLevel) return false;
        if (gameData.isSkillPurchased(skillId)) return false;
        return gameData.getSpendableXP() >= getXPCost(skill);
    }

    public static String getCategoryName(int category) {
        if (category < 0 || category >= CATEGORY_COUNT) return "Unknown";
        return CATEGORY_NAMES[category];
    }

    public static int getCategoryColor(int category) {
        if (category < 0 || category >= CATEGORY_COUNT) return 0xFFFFFFFF;
        return CATEGORY_COLORS[category];
    }
}
