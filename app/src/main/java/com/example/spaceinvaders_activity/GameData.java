package com.example.spaceinvaders_activity;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages persistent game data: XP, player level, high scores, and game history.
 * Uses SharedPreferences for storage.
 */
public class GameData {

    private static final String PREFS_NAME = "SpaceInvadersData";
    private static final String KEY_TOTAL_XP = "totalXP";
    private static final String KEY_PLAYER_LEVEL = "playerLevel";
    private static final String KEY_HIGH_SCORE = "highScore";
    private static final String KEY_HIGHEST_LEVEL = "highestLevel";
    private static final String KEY_GAMES_PLAYED = "gamesPlayed";
    private static final String KEY_TOTAL_KILLS = "totalKills";
    private static final String KEY_GAME_HISTORY = "gameHistory";
    private static final String KEY_UNLOCKED_SKILLS = "unlockedSkills";

    private static final int MAX_HISTORY_RECORDS = 50;

    // XP required per player level: level * 500
    private static final int XP_PER_LEVEL_MULTIPLIER = 500;

    // XP rewards
    public static final int XP_PER_KILL = 10;
    public static final int XP_PER_LEVEL_COMPLETE = 100; // multiplied by game level
    public static final int XP_PER_BOSS_KILL = 500;
    public static final int XP_BONUS_NO_DEATHS = 200;

    private SharedPreferences prefs;

    public GameData(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- XP and Level ---

    public int getTotalXP() {
        return prefs.getInt(KEY_TOTAL_XP, 0);
    }

    public int getPlayerLevel() {
        return prefs.getInt(KEY_PLAYER_LEVEL, 1);
    }

    public int getXPForNextLevel() {
        return getPlayerLevel() * XP_PER_LEVEL_MULTIPLIER;
    }

    public int getCurrentLevelXP() {
        int totalXP = getTotalXP();
        int level = getPlayerLevel();
        int xpUsed = 0;
        for (int i = 1; i < level; i++) {
            xpUsed += i * XP_PER_LEVEL_MULTIPLIER;
        }
        return totalXP - xpUsed;
    }

    public boolean addXP(int xp) {
        int totalXP = getTotalXP() + xp;
        int level = getPlayerLevel();
        boolean leveledUp = false;

        while (getCurrentLevelXPWith(totalXP, level) >= level * XP_PER_LEVEL_MULTIPLIER) {
            level++;
            leveledUp = true;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_XP, totalXP);
        editor.putInt(KEY_PLAYER_LEVEL, level);
        editor.apply();

        return leveledUp;
    }

    private int getCurrentLevelXPWith(int totalXP, int level) {
        int xpUsed = 0;
        for (int i = 1; i < level; i++) {
            xpUsed += i * XP_PER_LEVEL_MULTIPLIER;
        }
        return totalXP - xpUsed;
    }

    // --- High Score ---

    public int getHighScore() {
        return prefs.getInt(KEY_HIGH_SCORE, 0);
    }

    public boolean updateHighScore(int score) {
        if (score > getHighScore()) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
            return true;
        }
        return false;
    }

    public int getHighestLevel() {
        return prefs.getInt(KEY_HIGHEST_LEVEL, 0);
    }

    public void updateHighestLevel(int level) {
        if (level > getHighestLevel()) {
            prefs.edit().putInt(KEY_HIGHEST_LEVEL, level).apply();
        }
    }

    // --- Statistics ---

    public int getGamesPlayed() {
        return prefs.getInt(KEY_GAMES_PLAYED, 0);
    }

    public int getTotalKills() {
        return prefs.getInt(KEY_TOTAL_KILLS, 0);
    }

    public void addKills(int kills) {
        int total = getTotalKills() + kills;
        prefs.edit().putInt(KEY_TOTAL_KILLS, total).apply();
    }

    // --- Game History ---

    public void saveGameRecord(int score, int levelReached, int enemiesKilled,
                                int livesRemaining, long timePlayed, int xpEarned) {
        int gamesPlayed = getGamesPlayed() + 1;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_GAMES_PLAYED, gamesPlayed);

        // Build record JSON
        JSONObject record = new JSONObject();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            record.put("date", sdf.format(new Date()));
            record.put("score", score);
            record.put("level", levelReached);
            record.put("kills", enemiesKilled);
            record.put("lives", livesRemaining);
            record.put("time", timePlayed / 1000); // seconds
            record.put("xp", xpEarned);
            record.put("gameNumber", gamesPlayed);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Load existing history
        JSONArray history = getHistoryJSON();
        // Add new record at beginning
        JSONArray newHistory = new JSONArray();
        newHistory.put(record);
        for (int i = 0; i < history.length() && i < MAX_HISTORY_RECORDS - 1; i++) {
            try {
                newHistory.put(history.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString(KEY_GAME_HISTORY, newHistory.toString());
        editor.apply();
    }

    private JSONArray getHistoryJSON() {
        String historyStr = prefs.getString(KEY_GAME_HISTORY, "[]");
        try {
            return new JSONArray(historyStr);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public List<GameRecord> getGameHistory() {
        List<GameRecord> records = new ArrayList<>();
        JSONArray history = getHistoryJSON();
        for (int i = 0; i < history.length(); i++) {
            try {
                JSONObject obj = history.getJSONObject(i);
                records.add(new GameRecord(
                        obj.getString("date"),
                        obj.getInt("score"),
                        obj.getInt("level"),
                        obj.getInt("kills"),
                        obj.optInt("lives", 0),
                        obj.optLong("time", 0),
                        obj.optInt("xp", 0)
                ));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return records;
    }

    // --- Unlocked Skills ---

    public void unlockSkill(int skillId) {
        String unlocked = prefs.getString(KEY_UNLOCKED_SKILLS, "");
        if (!isSkillUnlocked(skillId)) {
            if (!unlocked.isEmpty()) unlocked += ",";
            unlocked += skillId;
            prefs.edit().putString(KEY_UNLOCKED_SKILLS, unlocked).apply();
        }
    }

    public boolean isSkillUnlocked(int skillId) {
        String unlocked = prefs.getString(KEY_UNLOCKED_SKILLS, "");
        if (unlocked.isEmpty()) return false;
        for (String s : unlocked.split(",")) {
            if (Integer.parseInt(s.trim()) == skillId) return true;
        }
        return false;
    }

    public List<Integer> getUnlockedSkillIds() {
        List<Integer> ids = new ArrayList<>();
        String unlocked = prefs.getString(KEY_UNLOCKED_SKILLS, "");
        if (unlocked.isEmpty()) return ids;
        for (String s : unlocked.split(",")) {
            ids.add(Integer.parseInt(s.trim()));
        }
        return ids;
    }

    // --- Settings ---

    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    private static final String KEY_SFX_VOLUME = "sfxVolume";
    private static final String KEY_VIBRATION = "vibrationEnabled";

    public int getMusicVolume() { return prefs.getInt(KEY_MUSIC_VOLUME, 80); }
    public void setMusicVolume(int vol) { prefs.edit().putInt(KEY_MUSIC_VOLUME, vol).apply(); }
    public int getSfxVolume() { return prefs.getInt(KEY_SFX_VOLUME, 80); }
    public void setSfxVolume(int vol) { prefs.edit().putInt(KEY_SFX_VOLUME, vol).apply(); }
    public boolean isVibrationEnabled() { return prefs.getBoolean(KEY_VIBRATION, true); }
    public void setVibrationEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply(); }

    // --- Achievements ---

    private static final String KEY_ACHIEVEMENTS = "achievements";
    private static final String KEY_TOTAL_POWERUPS = "totalPowerups";

    public void unlockAchievement(int id) {
        String unlocked = prefs.getString(KEY_ACHIEVEMENTS, "");
        if (!isAchievementUnlocked(id)) {
            if (!unlocked.isEmpty()) unlocked += ",";
            unlocked += id;
            prefs.edit().putString(KEY_ACHIEVEMENTS, unlocked).apply();
        }
    }

    public boolean isAchievementUnlocked(int id) {
        String unlocked = prefs.getString(KEY_ACHIEVEMENTS, "");
        if (unlocked.isEmpty()) return false;
        for (String s : unlocked.split(",")) {
            try {
                if (Integer.parseInt(s.trim()) == id) return true;
            } catch (NumberFormatException e) { /* skip */ }
        }
        return false;
    }

    public int getUnlockedAchievementCount() {
        String unlocked = prefs.getString(KEY_ACHIEVEMENTS, "");
        if (unlocked.isEmpty()) return 0;
        return unlocked.split(",").length;
    }

    public void addPowerupCollected() {
        int total = prefs.getInt(KEY_TOTAL_POWERUPS, 0) + 1;
        prefs.edit().putInt(KEY_TOTAL_POWERUPS, total).apply();
    }

    public int getTotalPowerups() { return prefs.getInt(KEY_TOTAL_POWERUPS, 0); }

    // --- Reset ---

    public void resetAll() {
        prefs.edit().clear().apply();
    }

    // --- Inner class for game records ---

    public static class GameRecord {
        public final String date;
        public final int score;
        public final int levelReached;
        public final int enemiesKilled;
        public final int livesRemaining;
        public final long timePlayed;
        public final int xpEarned;

        public GameRecord(String date, int score, int levelReached, int enemiesKilled,
                          int livesRemaining, long timePlayed, int xpEarned) {
            this.date = date;
            this.score = score;
            this.levelReached = levelReached;
            this.enemiesKilled = enemiesKilled;
            this.livesRemaining = livesRemaining;
            this.timePlayed = timePlayed;
            this.xpEarned = xpEarned;
        }

        public String getFormattedTime() {
            long seconds = timePlayed;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }
}
