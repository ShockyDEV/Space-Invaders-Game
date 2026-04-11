package com.example.spaceinvaders_activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpaceInvadersActivity extends Activity {

    SpaceInvadersEngine spaceInvadersEngine;
    private boolean isGameInitialized = false;
    private int screenX, screenY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenX = size.x;
        screenY = size.y;

        initEngine();
        showMainMenu();
    }

    private void initEngine() {
        if (!isGameInitialized) {
            spaceInvadersEngine = new SpaceInvadersEngine(this, screenX, screenY);
            setContentView(spaceInvadersEngine);
            isGameInitialized = true;
        }
    }

    // ==================== MAIN MENU ====================

    public void showMainMenu() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Build a custom layout
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 40, 60, 40);
            layout.setGravity(Gravity.CENTER_HORIZONTAL);

            // Title
            TextView title = new TextView(this);
            title.setText("SPACE INVADERS");
            title.setTextSize(28);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 10);
            layout.addView(title);

            // Player level & XP
            TextView levelText = new TextView(this);
            levelText.setText(String.format(Locale.getDefault(),
                    "Commander Level: %d\nXP: %d / %d\nHigh Score: %d\nGames Played: %d",
                    data.getPlayerLevel(),
                    data.getCurrentLevelXP(),
                    data.getXPForNextLevel(),
                    data.getHighScore(),
                    data.getGamesPlayed()));
            levelText.setTextSize(14);
            levelText.setGravity(Gravity.CENTER);
            levelText.setPadding(0, 10, 0, 30);
            layout.addView(levelText);

            // Play button
            Button playBtn = createMenuButton("PLAY");
            layout.addView(playBtn);

            // Skills button
            Button skillsBtn = createMenuButton("SKILLS");
            layout.addView(skillsBtn);

            // History button
            Button historyBtn = createMenuButton("RECORDS");
            layout.addView(historyBtn);

            // Stats button
            Button statsBtn = createMenuButton("STATS");
            layout.addView(statsBtn);

            builder.setView(layout);
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);

            playBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showSkillSelection();
            });

            skillsBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showSkillTree();
            });

            historyBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showHistory();
            });

            statsBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showStats();
            });

            dialog.show();
        });
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(16);
        btn.setAllCaps(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(params);

        return btn;
    }

    // ==================== SKILL SELECTION ====================

    private void showSkillSelection() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            int playerLevel = data.getPlayerLevel();
            List<Skill> available = Skill.getAvailableSkills(playerLevel);

            if (available.isEmpty()) {
                // No skills available, start game directly
                startGameWithSkills(new ArrayList<>());
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView title = new TextView(this);
            title.setText("Select Skills (max 2)");
            title.setTextSize(20);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 20);
            layout.addView(title);

            TextView hint = new TextView(this);
            hint.setText(String.format(Locale.getDefault(),
                    "Commander Level %d - %d skills available", playerLevel, available.size()));
            hint.setTextSize(13);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, 0, 0, 15);
            layout.addView(hint);

            // Scrollable skill list
            ScrollView scroll = new ScrollView(this);
            LinearLayout skillList = new LinearLayout(this);
            skillList.setOrientation(LinearLayout.VERTICAL);

            List<CheckBox> checkBoxes = new ArrayList<>();

            for (Skill skill : available) {
                CheckBox cb = new CheckBox(this);
                cb.setText(String.format("%s (Lvl %d)\n%s", skill.name, skill.unlockLevel, skill.description));
                cb.setTextSize(13);
                cb.setPadding(10, 10, 10, 10);
                cb.setTag(skill.id);

                // Enforce max 2 selection
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        int checkedCount = 0;
                        for (CheckBox box : checkBoxes) {
                            if (box.isChecked()) checkedCount++;
                        }
                        if (checkedCount > Skill.MAX_ACTIVE_SKILLS) {
                            cb.setChecked(false);
                        }
                    }
                });

                checkBoxes.add(cb);
                skillList.addView(cb);
            }

            scroll.addView(skillList);
            layout.addView(scroll);

            builder.setView(layout);
            builder.setPositiveButton("START GAME", (dialog, which) -> {
                List<Integer> selectedSkills = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isChecked()) {
                        selectedSkills.add((Integer) cb.getTag());
                    }
                }
                startGameWithSkills(selectedSkills);
            });

            builder.setNegativeButton("BACK", (dialog, which) -> showMainMenu());

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    private void startGameWithSkills(List<Integer> skills) {
        spaceInvadersEngine.startNewGame(skills);
    }

    // ==================== SKILL TREE ====================

    private void showSkillTree() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            int playerLevel = data.getPlayerLevel();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            ScrollView scroll = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView title = new TextView(this);
            title.setText("SKILL TREE");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 10);
            layout.addView(title);

            TextView levelInfo = new TextView(this);
            levelInfo.setText(String.format(Locale.getDefault(),
                    "Your Level: %d | XP: %d / %d",
                    playerLevel, data.getCurrentLevelXP(), data.getXPForNextLevel()));
            levelInfo.setTextSize(14);
            levelInfo.setGravity(Gravity.CENTER);
            levelInfo.setPadding(0, 0, 0, 20);
            layout.addView(levelInfo);

            for (Skill skill : Skill.getAllSkills()) {
                LinearLayout skillRow = new LinearLayout(this);
                skillRow.setOrientation(LinearLayout.VERTICAL);
                skillRow.setPadding(15, 15, 15, 15);

                boolean unlocked = skill.unlockLevel <= playerLevel;

                TextView skillName = new TextView(this);
                String status = unlocked ? " [UNLOCKED]" : " [Level " + skill.unlockLevel + "]";
                skillName.setText(skill.name + status);
                skillName.setTextSize(16);
                skillName.setTypeface(Typeface.DEFAULT_BOLD);

                TextView skillDesc = new TextView(this);
                skillDesc.setText(skill.description);
                skillDesc.setTextSize(13);
                skillDesc.setPadding(0, 5, 0, 10);

                if (!unlocked) {
                    skillName.setAlpha(0.4f);
                    skillDesc.setAlpha(0.4f);
                }

                skillRow.addView(skillName);
                skillRow.addView(skillDesc);

                // Divider
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(Color.GRAY);

                layout.addView(skillRow);
                layout.addView(divider);
            }

            scroll.addView(layout);
            builder.setView(scroll);
            builder.setPositiveButton("BACK", (dialog, which) -> showMainMenu());

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    // ==================== HISTORY ====================

    private void showHistory() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            List<GameData.GameRecord> history = data.getGameHistory();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            ScrollView scroll = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView title = new TextView(this);
            title.setText("GAME RECORDS");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 20);
            layout.addView(title);

            if (history.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No games played yet!\nStart your first mission.");
                empty.setTextSize(16);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, 40, 0, 40);
                layout.addView(empty);
            } else {
                // Column headers
                TextView header = new TextView(this);
                header.setText(String.format("%-12s %6s %4s %5s %6s",
                        "Date", "Score", "Lvl", "Kills", "XP"));
                header.setTextSize(12);
                header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                header.setPadding(0, 0, 0, 10);
                layout.addView(header);

                for (int i = 0; i < history.size(); i++) {
                    GameData.GameRecord rec = history.get(i);

                    TextView row = new TextView(this);
                    String dateShort = rec.date.length() > 10 ? rec.date.substring(5) : rec.date;
                    row.setText(String.format(Locale.getDefault(),
                            "%-12s %6d %4d %5d %6d",
                            dateShort, rec.score, rec.levelReached,
                            rec.enemiesKilled, rec.xpEarned));
                    row.setTextSize(12);
                    row.setTypeface(Typeface.MONOSPACE);
                    row.setPadding(0, 5, 0, 5);

                    // Highlight high scores
                    if (rec.score == data.getHighScore() && rec.score > 0) {
                        row.setTextColor(Color.rgb(255, 215, 0));
                    }

                    layout.addView(row);

                    if (i < history.size() - 1) {
                        View divider = new View(this);
                        divider.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        divider.setBackgroundColor(Color.DKGRAY);
                        layout.addView(divider);
                    }
                }
            }

            scroll.addView(layout);
            builder.setView(scroll);
            builder.setPositiveButton("BACK", (dialog, which) -> showMainMenu());

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    // ==================== STATS ====================

    private void showStats() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 40, 60, 40);
            layout.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView title = new TextView(this);
            title.setText("COMMANDER STATS");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 30);
            layout.addView(title);

            String[] labels = {
                    "Commander Level",
                    "Total XP",
                    "XP to Next Level",
                    "High Score",
                    "Highest Level Reached",
                    "Games Played",
                    "Total Enemies Defeated",
                    "Skills Unlocked"
            };

            String[] values = {
                    String.valueOf(data.getPlayerLevel()),
                    String.valueOf(data.getTotalXP()),
                    data.getCurrentLevelXP() + " / " + data.getXPForNextLevel(),
                    String.valueOf(data.getHighScore()),
                    String.valueOf(data.getHighestLevel()),
                    String.valueOf(data.getGamesPlayed()),
                    String.valueOf(data.getTotalKills()),
                    Skill.getAvailableSkills(data.getPlayerLevel()).size() + " / "
                            + Skill.getAllSkills().size()
            };

            for (int i = 0; i < labels.length; i++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 10, 0, 10);

                TextView label = new TextView(this);
                label.setText(labels[i]);
                label.setTextSize(15);
                label.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView value = new TextView(this);
                value.setText(values[i]);
                value.setTextSize(15);
                value.setTypeface(Typeface.DEFAULT_BOLD);
                value.setGravity(Gravity.END);

                row.addView(label);
                row.addView(value);
                layout.addView(row);

                if (i < labels.length - 1) {
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(Color.GRAY);
                    layout.addView(divider);
                }
            }

            builder.setView(layout);
            builder.setPositiveButton("BACK", (dialog, which) -> showMainMenu());

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onResume() {
        super.onResume();
        if (isGameInitialized) {
            spaceInvadersEngine.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGameInitialized) {
            spaceInvadersEngine.pause();
        }
    }
}
