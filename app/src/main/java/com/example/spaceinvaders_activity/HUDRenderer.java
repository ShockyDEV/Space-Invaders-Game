package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.List;
import java.util.Locale;

/**
 * Renders all HUD/UI overlay elements: score, lives, level info, XP bar,
 * combo counter, active skills, level transition screens, game over screen.
 */
public class HUDRenderer {

    private int screenX, screenY;
    private float hudTextSize;
    private float smallTextSize;
    private float bigTextSize;

    public HUDRenderer(int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.hudTextSize = screenY / 25f;
        this.smallTextSize = screenY / 35f;
        this.bigTextSize = screenY / 10f;
    }

    public void drawGameHUD(Canvas canvas, Paint paint, GameState state, PlayerShip player) {
        // Top bar background
        paint.setColor(Color.argb(140, 0, 0, 0));
        canvas.drawRect(0, 0, screenX, hudTextSize * 2.8f, paint);

        // Score
        paint.setColor(Color.argb(255, 249, 200, 0));
        paint.setTextSize(hudTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Score: " + state.score, 20, hudTextSize * 1.2f, paint);

        // Lives (draw hearts) - not shown in time attack
        if (state.gameMode != GameState.MODE_TIME_ATTACK) {
            float heartX = screenX * 0.4f;
            paint.setColor(Color.RED);
            paint.setTextSize(hudTextSize * 0.9f);
            for (int i = 0; i < state.lives; i++) {
                canvas.drawText("\u2665", heartX + i * hudTextSize * 1.1f, hudTextSize * 1.2f, paint);
            }
        }

        // Level/mode indicator
        paint.setColor(Color.argb(255, 100, 200, 255));
        paint.setTextSize(hudTextSize * 0.85f);
        paint.setTextAlign(Paint.Align.RIGHT);
        String levelText;
        if (state.gameMode == GameState.MODE_SURVIVAL) {
            levelText = "Wave " + state.survivalWave;
        } else if (state.gameMode == GameState.MODE_TIME_ATTACK) {
            long remaining = state.getRemainingTimeAttackMs();
            long secs = Math.max(0, remaining / 1000);
            levelText = "TIME: " + secs / 60 + ":" + String.format(Locale.getDefault(), "%02d", secs % 60);
            if (remaining < 15000) {
                // Flash red when low on time
                float flash = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.01);
                paint.setColor(Color.argb((int) (255 * flash), 255, 50, 50));
            }
        } else {
            levelText = "Lvl " + state.currentLevel;
            if (state.levelConfig != null) {
                levelText += " - " + state.levelConfig.levelName;
            }
        }
        canvas.drawText(levelText, screenX - 20, hudTextSize * 1.2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Second row: Combo + XP + Kill count
        float row2Y = hudTextSize * 2.3f;
        paint.setTextSize(smallTextSize);

        // Combo
        if (state.comboCount > 1) {
            int comboAlpha = Math.min(255, 150 + state.comboCount * 20);
            int r = Math.min(255, 150 + state.comboCount * 15);
            paint.setColor(Color.argb(comboAlpha, r, 255, 50));
            canvas.drawText("COMBO x" + state.comboCount + " (" + state.getScoreMultiplier() + "x)", 20, row2Y, paint);
        }

        // Kill count
        paint.setColor(Color.argb(200, 200, 200, 200));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Kills: " + state.enemiesKilled, screenX / 2f, row2Y, paint);

        // Game time / mode info
        paint.setTextAlign(Paint.Align.RIGHT);
        if (state.gameMode == GameState.MODE_SURVIVAL) {
            canvas.drawText("Next wave: " + (GameState.SURVIVAL_KILLS_PER_WAVE - state.survivalKillsThisWave) + " kills",
                    screenX - 20, row2Y, paint);
        } else {
            long elapsed = state.getGameDuration() / 1000;
            String time = String.format(Locale.getDefault(), "%d:%02d", elapsed / 60, elapsed % 60);
            canvas.drawText(time, screenX - 20, row2Y, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);

        // Active skills icons at bottom-left
        drawActiveSkills(canvas, paint, state.activeSkills);

        // Bottom-right indicators
        float bottomY = screenY - 20;

        // Ultimate laser cooldown
        if (player.hasUltimateLaserSkill()) {
            paint.setTextSize(smallTextSize);
            paint.setTextAlign(Paint.Align.RIGHT);
            long cooldown = player.getUltimateCooldownRemaining();
            if (cooldown > 0) {
                paint.setColor(Color.argb(150, 150, 100, 200));
                canvas.drawText("ULTIMATE: " + (cooldown / 1000) + "s", screenX - 20, bottomY, paint);
            } else if (player.isCurrentlyCharging()) {
                float flash = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.01);
                paint.setColor(Color.argb((int) (255 * flash), 255, 100, 255));
                canvas.drawText("CHARGING...", screenX - 20, bottomY, paint);
            } else {
                float flash = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.005);
                paint.setColor(Color.argb((int) (200 * flash), 255, 100, 255));
                canvas.drawText("[ULTIMATE READY - hold to charge]", screenX - 20, bottomY, paint);
            }
            paint.setTextAlign(Paint.Align.LEFT);
            bottomY -= smallTextSize * 1.5f;
        }

        // Bomb indicator if available
        if (player.hasBombSkill() && player.isBombAvailable()) {
            paint.setColor(Color.argb(200, 255, 80, 80));
            paint.setTextSize(smallTextSize);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("[BOMB READY - 2 finger tap]", screenX - 20, bottomY, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            bottomY -= smallTextSize * 1.5f;
        }

        // Cooldown indicators for ultimate skills
        drawUltimateCooldowns(canvas, paint, player, bottomY);

        // Zone progress bar (bottom center)
        if (state.gameMode == GameState.MODE_CAMPAIGN && state.levelConfig != null) {
            drawZoneProgress(canvas, paint, state);
        }
    }

    private void drawUltimateCooldowns(Canvas canvas, Paint paint, PlayerShip player, float startY) {
        paint.setTextSize(smallTextSize * 0.85f);
        paint.setTextAlign(Paint.Align.RIGHT);
        float y = startY;

        // Black Hole
        long cd = player.getBlackHoleCooldownRemaining();
        if (cd > 0) {
            paint.setColor(Color.argb(150, 120, 50, 255));
            canvas.drawText("BLACK HOLE: " + (cd / 1000) + "s", screenX - 20, y, paint);
            y -= smallTextSize * 1.3f;
        }

        // Time Stop
        cd = player.getTimeStopCooldownRemaining();
        if (cd > 0) {
            paint.setColor(Color.argb(150, 180, 100, 255));
            canvas.drawText("TIME STOP: " + (cd / 1000) + "s", screenX - 20, y, paint);
            y -= smallTextSize * 1.3f;
        }

        // Barrage
        cd = player.getBarrageCooldownRemaining();
        if (cd > 0) {
            paint.setColor(Color.argb(150, 255, 200, 50));
            canvas.drawText("BARRAGE: " + (cd / 1000) + "s", screenX - 20, y, paint);
            y -= smallTextSize * 1.3f;
        }

        // Supernova
        if (player.isSupernovaAvailable()) {
            float flash = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.003);
            paint.setColor(Color.argb((int) (200 * flash), 255, 255, 100));
            canvas.drawText("[SUPERNOVA READY]", screenX - 20, y, paint);
        }

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawZoneProgress(Canvas canvas, Paint paint, GameState state) {
        float barWidth = screenX * 0.3f;
        float barHeight = 8;
        float barX = (screenX - barWidth) / 2f;
        float barY = screenY - 15;

        // Background bar
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint);

        // Progress fill
        float progress = (state.currentLevel - 1) / 100f;
        paint.setColor(Color.argb(180, 100, 200, 255));
        canvas.drawRect(barX, barY, barX + barWidth * Math.min(1f, progress), barY + barHeight, paint);

        // Zone markers (every 10 levels)
        paint.setColor(Color.argb(120, 255, 255, 255));
        for (int i = 1; i <= 9; i++) {
            float mx = barX + barWidth * (i / 10f);
            canvas.drawRect(mx - 1, barY - 2, mx + 1, barY + barHeight + 2, paint);
        }

        // Zone label
        paint.setTextSize(smallTextSize * 0.65f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.argb(180, 200, 200, 200));
        int zone = state.levelConfig.zone + 1;
        canvas.drawText("Zone " + zone + "/10", screenX / 2f, barY - 3, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawActiveSkills(Canvas canvas, Paint paint, List<Integer> activeSkills) {
        if (activeSkills == null || activeSkills.isEmpty()) return;

        float startX = 15;
        float startY = screenY - 60;
        paint.setTextSize(smallTextSize * 0.7f);

        for (int skillId : activeSkills) {
            Skill skill = Skill.getSkillById(skillId);
            if (skill != null) {
                // Skill icon circle
                paint.setColor(skill.iconColor);
                paint.setAlpha(180);
                canvas.drawCircle(startX + 12, startY, 12, paint);

                // Skill name
                paint.setColor(Color.WHITE);
                paint.setAlpha(200);
                canvas.drawText(skill.name, startX + 30, startY + 5, paint);

                startY -= 35;
            }
        }
    }

    public void drawLevelTransition(Canvas canvas, Paint paint, GameState state) {
        // Darken screen
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenX, screenY, paint);

        long elapsed = System.currentTimeMillis() - state.levelTransitionStart;
        float progress = Math.min(1f, elapsed / (float) GameState.LEVEL_TRANSITION_DURATION);

        // Star rating from previous level
        if (state.starRating > 0 && progress < 0.4f) {
            float starAlpha = Math.min(1f, progress * 5f);
            paint.setColor(Color.argb((int) (255 * starAlpha), 255, 215, 0));
            paint.setTextSize(bigTextSize * 0.6f);
            paint.setTextAlign(Paint.Align.CENTER);

            String stars = "";
            for (int i = 0; i < state.starRating; i++) stars += "\u2605 ";
            for (int i = state.starRating; i < 3; i++) stars += "\u2606 ";
            canvas.drawText(stars, screenX / 2f, screenY * 0.35f, paint);
        }

        // Level number
        float scale = progress < 0.3f ? progress / 0.3f : 1f;
        paint.setColor(Color.argb(255, 100, 200, 255));
        paint.setTextSize(bigTextSize * scale);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LEVEL " + state.currentLevel, screenX / 2f, screenY * 0.45f, paint);

        // Level name
        if (state.levelConfig != null && progress > 0.2f) {
            float nameAlpha = Math.min(1f, (progress - 0.2f) * 3f);
            paint.setColor(Color.argb((int) (255 * nameAlpha), 255, 255, 255));
            paint.setTextSize(hudTextSize * 1.3f);
            canvas.drawText(state.levelConfig.levelName, screenX / 2f, screenY * 0.55f, paint);

            // Zone name (below level name)
            if (progress > 0.3f) {
                float zoneAlpha = Math.min(1f, (progress - 0.3f) * 3f);
                paint.setColor(Color.argb((int) (180 * zoneAlpha), 150, 200, 255));
                paint.setTextSize(hudTextSize * 0.9f);
                int zone = state.levelConfig.zone + 1;
                canvas.drawText("Zone " + zone + "/10", screenX / 2f, screenY * 0.60f, paint);
            }

            // Boss warning
            if (state.levelConfig.hasBoss && progress > 0.5f) {
                float warnAlpha = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.008);
                paint.setColor(Color.argb((int) (255 * warnAlpha), 255, 50, 50));
                paint.setTextSize(hudTextSize);
                canvas.drawText("!! WARNING: BOSS BATTLE !!", screenX / 2f, screenY * 0.68f, paint);
            }
        }

        // "Get Ready" text
        if (progress > 0.6f) {
            float readyAlpha = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.01);
            paint.setColor(Color.argb((int) (200 * readyAlpha), 255, 255, 255));
            paint.setTextSize(hudTextSize);
            canvas.drawText("Get Ready...", screenX / 2f, screenY * 0.75f, paint);
        }

        paint.setTextAlign(Paint.Align.LEFT);
    }

    public void drawGameOver(Canvas canvas, Paint paint, GameState state) {
        // Dark overlay
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, screenX, screenY, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        float cx = screenX / 2f;

        // GAME OVER title
        paint.setColor(Color.argb(255, 255, 50, 50));
        paint.setTextSize(bigTextSize);
        canvas.drawText("GAME OVER", cx, screenY * 0.2f, paint);

        // Stats
        paint.setColor(Color.WHITE);
        paint.setTextSize(hudTextSize);

        float y = screenY * 0.35f;
        float spacing = hudTextSize * 1.8f;

        canvas.drawText("Final Score: " + state.score, cx, y, paint);
        y += spacing;

        if (state.gameMode == GameState.MODE_SURVIVAL) {
            canvas.drawText("Waves Survived: " + state.survivalWave, cx, y, paint);
        } else if (state.gameMode == GameState.MODE_TIME_ATTACK) {
            canvas.drawText("Rounds Cleared: " + (state.currentLevel - 1), cx, y, paint);
        } else {
            canvas.drawText("Level Reached: " + state.currentLevel, cx, y, paint);
        }
        y += spacing;

        canvas.drawText("Enemies Defeated: " + state.enemiesKilled, cx, y, paint);
        y += spacing;

        if (state.bossesKilled > 0) {
            canvas.drawText("Bosses Defeated: " + state.bossesKilled, cx, y, paint);
            y += spacing;
        }

        canvas.drawText("Max Combo: " + state.maxCombo, cx, y, paint);
        y += spacing;

        // XP earned
        paint.setColor(Color.argb(255, 100, 255, 100));
        canvas.drawText("XP Earned: +" + state.xpEarned, cx, y, paint);
        y += spacing;

        // Time played
        long elapsed = state.getGameDuration() / 1000;
        String time = String.format(Locale.getDefault(), "%d:%02d", elapsed / 60, elapsed % 60);
        paint.setColor(Color.argb(200, 200, 200, 200));
        canvas.drawText("Time: " + time, cx, y, paint);
        y += spacing * 1.5f;

        // Tap to continue
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.005);
        paint.setColor(Color.argb((int) (255 * pulse), 255, 255, 255));
        paint.setTextSize(hudTextSize * 0.9f);
        canvas.drawText("Tap to continue", cx, y, paint);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    public void drawPauseScreen(Canvas canvas, Paint paint) {
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(0, 0, screenX, screenY, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(bigTextSize * 0.7f);
        canvas.drawText("PAUSED", screenX / 2f, screenY * 0.4f, paint);

        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.004);
        paint.setColor(Color.argb((int) (200 * pulse), 255, 255, 255));
        paint.setTextSize(hudTextSize);
        canvas.drawText("Tap to resume", screenX / 2f, screenY * 0.55f, paint);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private static final String[] TUTORIAL_TIPS = {
            "Drag your finger to move your ship",
            "Release your finger to fire!",
            "Kill enemies quickly for COMBO multipliers",
            "Collect power-ups dropped by enemies",
            "Good luck, Commander!"
    };

    public void drawTutorial(Canvas canvas, Paint paint, GameState state) {
        if (!state.tutorialActive || state.tutorialStep >= GameState.TUTORIAL_STEPS) return;

        // Semi-transparent overlay
        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, screenY * 0.3f, screenX, screenY * 0.7f, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        float cx = screenX / 2f;

        // Step indicator
        paint.setColor(Color.argb(200, 150, 150, 150));
        paint.setTextSize(smallTextSize);
        canvas.drawText("TIP " + (state.tutorialStep + 1) + " / " + GameState.TUTORIAL_STEPS,
                cx, screenY * 0.38f, paint);

        // Tip text
        paint.setColor(Color.WHITE);
        paint.setTextSize(hudTextSize * 1.2f);
        canvas.drawText(TUTORIAL_TIPS[state.tutorialStep], cx, screenY * 0.48f, paint);

        // Tap to continue
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.005);
        paint.setColor(Color.argb((int) (200 * pulse), 200, 200, 200));
        paint.setTextSize(hudTextSize * 0.8f);
        canvas.drawText("Tap to continue", cx, screenY * 0.6f, paint);

        paint.setTextAlign(Paint.Align.LEFT);
    }
}
