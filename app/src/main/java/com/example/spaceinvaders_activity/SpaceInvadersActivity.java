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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
                    "Commander Level: %d | Skill Points: %d\nXP: %d / %d | Spendable: %d\nHigh Score: %d | Games: %d",
                    data.getPlayerLevel(),
                    SkillTree.getSkillPointBudget(data.getPlayerLevel()),
                    data.getCurrentLevelXP(),
                    data.getXPForNextLevel(),
                    data.getSpendableXP(),
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

            // Achievements button
            Button achieveBtn = createMenuButton("ACHIEVEMENTS");
            layout.addView(achieveBtn);

            // Shop button
            Button shopBtn = createMenuButton("SHOP");
            layout.addView(shopBtn);

            // Settings button
            Button settingsBtn = createMenuButton("SETTINGS");
            layout.addView(settingsBtn);

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

            achieveBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showAchievements();
            });

            shopBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showShop();
            });

            settingsBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showSettings();
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

    // ==================== SKILL SELECTION (LOADOUT BUILDER) ====================

    private void showSkillSelection() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            int playerLevel = data.getPlayerLevel();
            int budget = SkillTree.getSkillPointBudget(playerLevel);
            List<Skill> purchased = new ArrayList<>();
            for (Skill sk : Skill.getAllSkills()) {
                if (data.isSkillPurchased(sk.id) && sk.unlockLevel <= playerLevel) {
                    purchased.add(sk);
                }
            }

            if (purchased.isEmpty()) {
                // No skills purchased, start game directly
                startGameWithSkills(new ArrayList<>());
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 25, 40, 25);

            TextView title = new TextView(this);
            title.setText("BUILD LOADOUT");
            title.setTextSize(20);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 8);
            layout.addView(title);

            // Budget display (updated dynamically)
            TextView budgetText = new TextView(this);
            budgetText.setText(String.format(Locale.getDefault(),
                    "Skill Points: %d / %d", budget, budget));
            budgetText.setTextSize(15);
            budgetText.setTypeface(Typeface.DEFAULT_BOLD);
            budgetText.setGravity(Gravity.CENTER);
            budgetText.setTextColor(Color.rgb(100, 255, 100));
            budgetText.setPadding(0, 0, 0, 15);
            layout.addView(budgetText);

            // Scrollable skill list by category
            ScrollView scroll = new ScrollView(this);
            LinearLayout skillList = new LinearLayout(this);
            skillList.setOrientation(LinearLayout.VERTICAL);

            List<CheckBox> checkBoxes = new ArrayList<>();
            List<Integer> selectedIds = new ArrayList<>();

            // Group purchased skills by category
            for (int cat = 0; cat < SkillTree.CATEGORY_COUNT; cat++) {
                List<Skill> catSkills = new ArrayList<>();
                for (Skill sk : purchased) {
                    if (sk.category == cat) catSkills.add(sk);
                }
                if (catSkills.isEmpty()) continue;

                // Category header
                TextView catHeader = new TextView(this);
                catHeader.setText(SkillTree.getCategoryName(cat));
                catHeader.setTextSize(14);
                catHeader.setTypeface(Typeface.DEFAULT_BOLD);
                catHeader.setTextColor(SkillTree.getCategoryColor(cat));
                catHeader.setPadding(0, 12, 0, 4);
                skillList.addView(catHeader);

                for (Skill skill : catSkills) {
                    CheckBox cb = new CheckBox(this);
                    String costDots = "";
                    for (int p = 0; p < skill.pointCost; p++) costDots += "\u2b24";
                    cb.setText(String.format("%s %s\n%s", skill.name, costDots, skill.description));
                    cb.setTextSize(12);
                    cb.setPadding(8, 6, 8, 6);
                    cb.setTag(skill.id);

                    // Point budget enforcement
                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        int skillId = (Integer) buttonView.getTag();
                        Skill sk = Skill.getSkillById(skillId);
                        if (sk == null) return;

                        if (isChecked) {
                            // Check if adding exceeds budget
                            int usedPoints = 0;
                            for (CheckBox box : checkBoxes) {
                                if (box.isChecked()) {
                                    Skill s = Skill.getSkillById((Integer) box.getTag());
                                    if (s != null) usedPoints += s.pointCost;
                                }
                            }
                            if (usedPoints > budget) {
                                cb.setChecked(false);
                                return;
                            }
                        }

                        // Update budget display
                        int usedPoints = 0;
                        for (CheckBox box : checkBoxes) {
                            if (box.isChecked()) {
                                Skill s = Skill.getSkillById((Integer) box.getTag());
                                if (s != null) usedPoints += s.pointCost;
                            }
                        }
                        int remaining = budget - usedPoints;
                        budgetText.setText(String.format(Locale.getDefault(),
                                "Skill Points: %d / %d", remaining, budget));
                        budgetText.setTextColor(remaining > 0 ?
                                Color.rgb(100, 255, 100) : Color.rgb(255, 200, 50));

                        // Disable checkboxes that can't fit in remaining budget
                        for (CheckBox box : checkBoxes) {
                            if (!box.isChecked()) {
                                Skill s = Skill.getSkillById((Integer) box.getTag());
                                box.setEnabled(s != null && s.pointCost <= remaining);
                            }
                        }
                    });

                    checkBoxes.add(cb);
                    skillList.addView(cb);
                }
            }

            scroll.addView(skillList);
            layout.addView(scroll);

            // Difficulty selector
            TextView diffLabel = new TextView(this);
            diffLabel.setText("Difficulty:");
            diffLabel.setTextSize(14);
            diffLabel.setTypeface(Typeface.DEFAULT_BOLD);
            diffLabel.setPadding(0, 15, 0, 3);
            layout.addView(diffLabel);

            RadioGroup diffGroup = new RadioGroup(this);
            diffGroup.setOrientation(RadioGroup.HORIZONTAL);

            RadioButton easyRb = new RadioButton(this);
            easyRb.setText("Easy");
            easyRb.setTextSize(12);
            easyRb.setId(View.generateViewId());
            diffGroup.addView(easyRb);

            RadioButton normalRb = new RadioButton(this);
            normalRb.setText("Normal");
            normalRb.setTextSize(12);
            normalRb.setChecked(true);
            normalRb.setId(View.generateViewId());
            diffGroup.addView(normalRb);

            RadioButton hardRb = new RadioButton(this);
            hardRb.setText("Hard");
            hardRb.setTextSize(12);
            hardRb.setId(View.generateViewId());
            diffGroup.addView(hardRb);

            layout.addView(diffGroup);

            // Game mode selector
            TextView modeLabel = new TextView(this);
            modeLabel.setText("Game Mode:");
            modeLabel.setTextSize(14);
            modeLabel.setTypeface(Typeface.DEFAULT_BOLD);
            modeLabel.setPadding(0, 10, 0, 3);
            layout.addView(modeLabel);

            RadioGroup modeGroup = new RadioGroup(this);
            modeGroup.setOrientation(RadioGroup.HORIZONTAL);

            RadioButton campaignRb = new RadioButton(this);
            campaignRb.setText("Campaign");
            campaignRb.setTextSize(11);
            campaignRb.setChecked(true);
            campaignRb.setId(View.generateViewId());
            modeGroup.addView(campaignRb);

            RadioButton survivalRb = new RadioButton(this);
            survivalRb.setText("Survival");
            survivalRb.setTextSize(11);
            survivalRb.setId(View.generateViewId());
            modeGroup.addView(survivalRb);

            RadioButton timeAttackRb = new RadioButton(this);
            timeAttackRb.setText("Time Attack");
            timeAttackRb.setTextSize(11);
            timeAttackRb.setId(View.generateViewId());
            modeGroup.addView(timeAttackRb);

            layout.addView(modeGroup);

            builder.setView(layout);
            builder.setPositiveButton("START GAME", (dialog, which) -> {
                List<Integer> selectedSkills = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isChecked()) {
                        selectedSkills.add((Integer) cb.getTag());
                    }
                }
                int difficulty = GameConfig.DIFF_NORMAL;
                if (easyRb.isChecked()) difficulty = GameConfig.DIFF_EASY;
                if (hardRb.isChecked()) difficulty = GameConfig.DIFF_HARD;
                int mode = GameState.MODE_CAMPAIGN;
                if (survivalRb.isChecked()) mode = GameState.MODE_SURVIVAL;
                if (timeAttackRb.isChecked()) mode = GameState.MODE_TIME_ATTACK;
                startGameWithSkills(selectedSkills, difficulty, mode);
            });

            builder.setNegativeButton("BACK", (dialog, which) -> showMainMenu());

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    private void startGameWithSkills(List<Integer> skills) {
        spaceInvadersEngine.startNewGame(skills, GameConfig.DIFF_NORMAL, GameState.MODE_CAMPAIGN);
    }

    private void startGameWithSkills(List<Integer> skills, int difficulty) {
        spaceInvadersEngine.startNewGame(skills, difficulty, GameState.MODE_CAMPAIGN);
    }

    private void startGameWithSkills(List<Integer> skills, int difficulty, int mode) {
        spaceInvadersEngine.startNewGame(skills, difficulty, mode);
    }

    // ==================== SKILL TREE (PURCHASE + VIEW) ====================

    private void showSkillTree() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            int playerLevel = data.getPlayerLevel();
            int budget = SkillTree.getSkillPointBudget(playerLevel);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            ScrollView scroll = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 25, 40, 25);

            TextView title = new TextView(this);
            title.setText("SKILL TREE");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 8);
            layout.addView(title);

            // Player info
            TextView levelInfo = new TextView(this);
            levelInfo.setText(String.format(Locale.getDefault(),
                    "Level: %d | Spendable XP: %d | Skill Points: %d",
                    playerLevel, data.getSpendableXP(), budget));
            levelInfo.setTextSize(13);
            levelInfo.setGravity(Gravity.CENTER);
            levelInfo.setPadding(0, 0, 0, 5);
            layout.addView(levelInfo);

            // Purchased count
            int purchasedCount = 0;
            for (Skill sk : Skill.getAllSkills()) {
                if (data.isSkillPurchased(sk.id)) purchasedCount++;
            }
            TextView purchasedInfo = new TextView(this);
            purchasedInfo.setText(String.format(Locale.getDefault(),
                    "Skills Owned: %d / %d", purchasedCount, Skill.getAllSkills().size()));
            purchasedInfo.setTextSize(12);
            purchasedInfo.setGravity(Gravity.CENTER);
            purchasedInfo.setPadding(0, 0, 0, 15);
            layout.addView(purchasedInfo);

            // Show skills grouped by category
            for (int cat = 0; cat < SkillTree.CATEGORY_COUNT; cat++) {
                List<Skill> catSkills = Skill.getSkillsByCategory(cat);
                if (catSkills.isEmpty()) continue;

                // Category header with colored bar
                LinearLayout catRow = new LinearLayout(this);
                catRow.setOrientation(LinearLayout.HORIZONTAL);
                catRow.setPadding(0, 15, 0, 5);
                catRow.setGravity(Gravity.CENTER_VERTICAL);

                View colorBar = new View(this);
                LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(8, 30);
                barParams.setMargins(0, 0, 10, 0);
                colorBar.setLayoutParams(barParams);
                colorBar.setBackgroundColor(SkillTree.getCategoryColor(cat));
                catRow.addView(colorBar);

                TextView catHeader = new TextView(this);
                catHeader.setText(SkillTree.getCategoryName(cat).toUpperCase());
                catHeader.setTextSize(15);
                catHeader.setTypeface(Typeface.DEFAULT_BOLD);
                catHeader.setTextColor(SkillTree.getCategoryColor(cat));
                catRow.addView(catHeader);

                layout.addView(catRow);

                for (Skill skill : catSkills) {
                    boolean levelReached = skill.unlockLevel <= playerLevel;
                    boolean purchased = data.isSkillPurchased(skill.id);
                    boolean canBuy = SkillTree.canPurchaseSkill(skill.id, playerLevel, data);
                    int xpCost = SkillTree.getXPCost(skill);

                    LinearLayout skillRow = new LinearLayout(this);
                    skillRow.setOrientation(LinearLayout.HORIZONTAL);
                    skillRow.setPadding(12, 8, 8, 8);
                    skillRow.setGravity(Gravity.CENTER_VERTICAL);

                    // Skill info (left side)
                    LinearLayout infoCol = new LinearLayout(this);
                    infoCol.setOrientation(LinearLayout.VERTICAL);
                    infoCol.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    // Name with point cost dots
                    TextView skillName = new TextView(this);
                    String costDots = "";
                    for (int p = 0; p < skill.pointCost; p++) costDots += "\u2b24";
                    String statusTag;
                    if (purchased) {
                        statusTag = " [OWNED]";
                    } else if (!levelReached) {
                        statusTag = " [Lv." + skill.unlockLevel + "]";
                    } else {
                        statusTag = " [" + xpCost + " XP]";
                    }
                    skillName.setText(skill.name + " " + costDots + statusTag);
                    skillName.setTextSize(13);
                    skillName.setTypeface(Typeface.DEFAULT_BOLD);

                    TextView skillDesc = new TextView(this);
                    skillDesc.setText(skill.description);
                    skillDesc.setTextSize(11);
                    skillDesc.setPadding(0, 2, 0, 0);

                    if (!levelReached) {
                        skillName.setAlpha(0.35f);
                        skillDesc.setAlpha(0.35f);
                    } else if (purchased) {
                        skillName.setTextColor(Color.rgb(100, 255, 100));
                    }

                    infoCol.addView(skillName);
                    infoCol.addView(skillDesc);
                    skillRow.addView(infoCol);

                    // Buy button (right side)
                    if (levelReached && !purchased) {
                        Button buyBtn = new Button(this);
                        buyBtn.setText("BUY\n" + xpCost + " XP");
                        buyBtn.setTextSize(10);
                        buyBtn.setEnabled(canBuy);
                        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        btnParams.setMargins(8, 0, 0, 0);
                        buyBtn.setLayoutParams(btnParams);
                        final int skillId = skill.id;
                        buyBtn.setOnClickListener(v -> {
                            if (data.purchaseSkill(skillId)) {
                                showSkillTree(); // Refresh
                            }
                        });
                        skillRow.addView(buyBtn);
                    }

                    layout.addView(skillRow);

                    // Thin divider
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(Color.DKGRAY);
                    layout.addView(divider);
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

            int pLevel = data.getPlayerLevel();
            int purchasedCount = 0;
            for (Skill sk : Skill.getAllSkills()) {
                if (data.isSkillPurchased(sk.id)) purchasedCount++;
            }

            String[] labels = {
                    "Commander Level",
                    "Total XP",
                    "Spendable XP",
                    "XP to Next Level",
                    "Skill Point Budget",
                    "Skills Purchased",
                    "High Score",
                    "Highest Level Reached",
                    "Games Played",
                    "Total Enemies Defeated"
            };

            String[] values = {
                    String.valueOf(pLevel),
                    String.valueOf(data.getTotalXP()),
                    String.valueOf(data.getSpendableXP()),
                    data.getCurrentLevelXP() + " / " + data.getXPForNextLevel(),
                    String.valueOf(SkillTree.getSkillPointBudget(pLevel)),
                    purchasedCount + " / " + Skill.getAllSkills().size(),
                    String.valueOf(data.getHighScore()),
                    String.valueOf(data.getHighestLevel()),
                    String.valueOf(data.getGamesPlayed()),
                    String.valueOf(data.getTotalKills())
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

    // ==================== SHOP ====================

    private void showShop() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            List<CosmeticsShop.ShipSkin> allSkins = CosmeticsShop.getAllSkins();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            ScrollView scroll = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView title = new TextView(this);
            title.setText("SHIP SKINS");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 10);
            layout.addView(title);

            TextView balanceText = new TextView(this);
            balanceText.setText("Spendable XP: " + data.getSpendableXP());
            balanceText.setTextSize(15);
            balanceText.setGravity(Gravity.CENTER);
            balanceText.setPadding(0, 0, 0, 20);
            layout.addView(balanceText);

            int selectedSkin = data.getSelectedSkin();

            for (CosmeticsShop.ShipSkin skin : allSkins) {
                boolean owned = data.isSkinOwned(skin.id);
                boolean selected = skin.id == selectedSkin;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(10, 15, 10, 15);
                row.setGravity(Gravity.CENTER_VERTICAL);

                // Color swatch
                View swatch = new View(this);
                LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(40, 40);
                swatchParams.setMargins(0, 0, 15, 0);
                swatch.setLayoutParams(swatchParams);
                if (skin.isRainbow) {
                    swatch.setBackgroundColor(Color.rgb(255, 100, 200));
                } else if (skin.tintColor != 0) {
                    swatch.setBackgroundColor(skin.tintColor);
                } else {
                    swatch.setBackgroundColor(Color.LTGRAY);
                }
                row.addView(swatch);

                // Name and status
                TextView nameText = new TextView(this);
                String label = skin.name;
                if (selected) label += " [EQUIPPED]";
                else if (owned) label += " [OWNED]";
                else label += " - " + skin.cost + " XP";
                nameText.setText(label);
                nameText.setTextSize(15);
                nameText.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                if (selected) nameText.setTextColor(Color.rgb(100, 255, 100));
                else if (owned) nameText.setTextColor(Color.rgb(200, 200, 200));
                row.addView(nameText);

                // Action button
                if (!owned && skin.cost > 0) {
                    Button buyBtn = new Button(this);
                    buyBtn.setText("BUY");
                    buyBtn.setTextSize(12);
                    final int skinId = skin.id;
                    final int cost = skin.cost;
                    buyBtn.setOnClickListener(v -> {
                        if (data.spendXP(cost)) {
                            data.ownSkin(skinId);
                            data.setSelectedSkin(skinId);
                            showShop(); // Refresh
                        }
                    });
                    buyBtn.setEnabled(data.getSpendableXP() >= skin.cost);
                    row.addView(buyBtn);
                } else if (owned && !selected) {
                    Button equipBtn = new Button(this);
                    equipBtn.setText("EQUIP");
                    equipBtn.setTextSize(12);
                    final int skinId = skin.id;
                    equipBtn.setOnClickListener(v -> {
                        data.setSelectedSkin(skinId);
                        showShop(); // Refresh
                    });
                    row.addView(equipBtn);
                }

                layout.addView(row);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.GRAY);
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

    // ==================== ACHIEVEMENTS ====================

    private void showAchievements() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            List<AchievementManager.Achievement> allAchievements = AchievementManager.getAllAchievements();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            ScrollView scroll = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView title = new TextView(this);
            title.setText("ACHIEVEMENTS");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 10);
            layout.addView(title);

            int unlocked = data.getUnlockedAchievementCount();
            TextView progressText = new TextView(this);
            progressText.setText(String.format(Locale.getDefault(),
                    "Unlocked: %d / %d", unlocked, allAchievements.size()));
            progressText.setTextSize(14);
            progressText.setGravity(Gravity.CENTER);
            progressText.setPadding(0, 0, 0, 20);
            layout.addView(progressText);

            for (AchievementManager.Achievement a : allAchievements) {
                boolean isUnlocked = data.isAchievementUnlocked(a.id);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(15, 15, 15, 15);

                TextView nameText = new TextView(this);
                String status = isUnlocked ? " \u2705" : " \uD83D\uDD12";
                nameText.setText(a.name + status);
                nameText.setTextSize(16);
                nameText.setTypeface(Typeface.DEFAULT_BOLD);

                TextView descText = new TextView(this);
                descText.setText(a.description + " (+" + a.xpReward + " XP)");
                descText.setTextSize(13);
                descText.setPadding(0, 5, 0, 10);

                if (!isUnlocked) {
                    nameText.setAlpha(0.5f);
                    descText.setAlpha(0.5f);
                } else {
                    nameText.setTextColor(Color.rgb(255, 215, 0));
                }

                row.addView(nameText);
                row.addView(descText);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(Color.GRAY);

                layout.addView(row);
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

    // ==================== SETTINGS ====================

    private void showSettings() {
        runOnUiThread(() -> {
            GameData data = spaceInvadersEngine.getGameData();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 40, 60, 40);

            TextView title = new TextView(this);
            title.setText("SETTINGS");
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 30);
            layout.addView(title);

            // Music Volume
            TextView musicLabel = new TextView(this);
            musicLabel.setText("Music Volume: " + data.getMusicVolume() + "%");
            musicLabel.setTextSize(15);
            musicLabel.setPadding(0, 10, 0, 5);
            layout.addView(musicLabel);

            SeekBar musicBar = new SeekBar(this);
            musicBar.setMax(100);
            musicBar.setProgress(data.getMusicVolume());
            musicBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    musicLabel.setText("Music Volume: " + progress + "%");
                }
                public void onStartTrackingTouch(SeekBar seekBar) {}
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            layout.addView(musicBar);

            // SFX Volume
            TextView sfxLabel = new TextView(this);
            sfxLabel.setText("SFX Volume: " + data.getSfxVolume() + "%");
            sfxLabel.setTextSize(15);
            sfxLabel.setPadding(0, 20, 0, 5);
            layout.addView(sfxLabel);

            SeekBar sfxBar = new SeekBar(this);
            sfxBar.setMax(100);
            sfxBar.setProgress(data.getSfxVolume());
            sfxBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    sfxLabel.setText("SFX Volume: " + progress + "%");
                }
                public void onStartTrackingTouch(SeekBar seekBar) {}
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            layout.addView(sfxBar);

            // Vibration toggle
            CheckBox vibrationCb = new CheckBox(this);
            vibrationCb.setText("Vibration");
            vibrationCb.setTextSize(15);
            vibrationCb.setChecked(data.isVibrationEnabled());
            vibrationCb.setPadding(0, 20, 0, 10);
            layout.addView(vibrationCb);

            // Reset tutorial button
            Button tutorialBtn = new Button(this);
            tutorialBtn.setText("RESET TUTORIAL");
            tutorialBtn.setTextSize(13);
            tutorialBtn.setOnClickListener(v -> {
                data.setTutorialDone(false);
                tutorialBtn.setText("TUTORIAL RESET!");
                tutorialBtn.setEnabled(false);
            });
            LinearLayout.LayoutParams tutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tutParams.setMargins(0, 20, 0, 0);
            tutorialBtn.setLayoutParams(tutParams);
            layout.addView(tutorialBtn);

            builder.setView(layout);
            builder.setPositiveButton("SAVE", (dialog, which) -> {
                data.setMusicVolume(musicBar.getProgress());
                data.setSfxVolume(sfxBar.getProgress());
                data.setVibrationEnabled(vibrationCb.isChecked());
                spaceInvadersEngine.reloadSettings();
                showMainMenu();
            });
            builder.setNegativeButton("CANCEL", (dialog, which) -> showMainMenu());

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
