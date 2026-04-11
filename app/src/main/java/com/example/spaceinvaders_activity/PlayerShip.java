package com.example.spaceinvaders_activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class PlayerShip {

    private RectF rect;
    private Bitmap bitmap;
    private float length_EGG;
    private float height_EGG;
    private float x;
    private float y;
    private long lastShotTime_EGG = System.currentTimeMillis();
    private long shotCooldown_EGG = 500; // Cooldown between shots

    // Skill-related
    private boolean hasRapidFire = false;
    private boolean hasSpeedBoost = false;
    private boolean hasShield = false;
    private boolean hasMultiShot = false;
    private boolean hasBigLaser = false;
    private boolean hasPiercingShot = false;
    private boolean hasBomb = false;
    private boolean bombAvailable = true;
    private boolean hasScoreMultiplier = false;
    private boolean hasRegeneration = false;
    private boolean hasUltimateLaser = false;

    // Temporary power-up buffs
    private boolean tempRapidFire = false;
    private long tempRapidFireEnd = 0;
    private boolean tempShield = false;
    private long tempShieldEnd = 0;
    private boolean tempScoreBoost = false;
    private long tempScoreBoostEnd = 0;
    private boolean frozen = false; // For freeze power-up (freezes enemies, not player)

    // Regeneration timer
    private long lastRegenTime = 0;
    private static final long REGEN_INTERVAL = 30000; // 30 seconds

    // Shield visual
    private float shieldPulse = 0;

    private int screenX, screenY;

    public PlayerShip(Context context, int screenX, int screenY) {
        rect = new RectF();
        this.screenX = screenX;
        this.screenY = screenY;

        length_EGG = screenX / 10f;
        height_EGG = screenY / 10f;

        x = screenX / 2f;
        y = screenY - height_EGG;

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.playership);
        bitmap = Bitmap.createScaledBitmap(bitmap, (int) length_EGG, (int) height_EGG, false);
    }

    // --- Skill activation ---

    public void applySkills(java.util.List<Integer> activeSkills) {
        if (activeSkills == null) return;
        for (int skillId : activeSkills) {
            switch (skillId) {
                case Skill.RAPID_FIRE:
                    hasRapidFire = true;
                    shotCooldown_EGG = 250; // Half cooldown
                    break;
                case Skill.BIG_LASER:
                    hasBigLaser = true;
                    break;
                case Skill.SHIELD:
                    hasShield = true;
                    break;
                case Skill.MULTI_SHOT:
                    hasMultiShot = true;
                    break;
                case Skill.PIERCING_SHOT:
                    hasPiercingShot = true;
                    break;
                case Skill.SPEED_BOOST:
                    hasSpeedBoost = true;
                    break;
                case Skill.BOMB:
                    hasBomb = true;
                    bombAvailable = true;
                    break;
                case Skill.SCORE_MULTIPLIER:
                    hasScoreMultiplier = true;
                    break;
                case Skill.REGENERATION:
                    hasRegeneration = true;
                    lastRegenTime = System.currentTimeMillis();
                    break;
                case Skill.ULTIMATE_LASER:
                    hasUltimateLaser = true;
                    break;
            }
        }
    }

    // --- Power-up application ---

    public void applyPowerUp(int powerUpType) {
        long now = System.currentTimeMillis();
        switch (powerUpType) {
            case PowerUp.RAPID_FIRE:
                tempRapidFire = true;
                tempRapidFireEnd = now + PowerUp.BUFF_DURATION;
                break;
            case PowerUp.SHIELD_BUBBLE:
                tempShield = true;
                tempShieldEnd = now + PowerUp.BUFF_DURATION;
                break;
            case PowerUp.SCORE_BOOST:
                tempScoreBoost = true;
                tempScoreBoostEnd = now + PowerUp.BUFF_DURATION;
                break;
        }
    }

    // --- Getters ---

    public RectF getRect() { return rect; }
    public Bitmap getBitmap() { return bitmap; }
    public float getX() { return x; }
    public float getHeight_EGG() { return height_EGG; }
    public float getLength_EGG() { return length_EGG; }

    public boolean isMultiShot() { return hasMultiShot; }
    public boolean isBigLaser() { return hasBigLaser; }
    public boolean isPiercingShot() { return hasPiercingShot; }
    public boolean hasShieldSkill() { return hasShield; }
    public boolean hasBombSkill() { return hasBomb; }
    public boolean isBombAvailable() { return bombAvailable; }
    public boolean hasUltimateLaserSkill() { return hasUltimateLaser; }
    public boolean hasScoreMultiplierSkill() { return hasScoreMultiplier; }
    public boolean hasRegenerationSkill() { return hasRegeneration; }

    public void useBomb() { bombAvailable = false; }

    public int getExtraLives() {
        return hasShield ? 2 : 0;
    }

    public int getScoreMultiplier() {
        int mult = 1;
        if (hasScoreMultiplier) mult *= 2;
        if (tempScoreBoost) mult *= 2;
        return mult;
    }

    public boolean isShielded() {
        return tempShield && System.currentTimeMillis() < tempShieldEnd;
    }

    // Check if regeneration should heal
    public boolean checkRegeneration() {
        if (!hasRegeneration) return false;
        long now = System.currentTimeMillis();
        if (now - lastRegenTime >= REGEN_INTERVAL) {
            lastRegenTime = now;
            return true;
        }
        return false;
    }

    // --- Position & shooting ---

    public void updatePosition(float touchX) {
        x = touchX - length_EGG / 2;
        // Clamp to screen bounds
        if (x < 0) x = 0;
        if (x > screenX - length_EGG) x = screenX - length_EGG;
        rect.left = x;
        rect.right = x + length_EGG;
        rect.top = y;
        rect.bottom = y + height_EGG;
    }

    public boolean tryShoot() {
        long currentTime = System.currentTimeMillis();
        long effectiveCooldown = shotCooldown_EGG;
        if (tempRapidFire && currentTime < tempRapidFireEnd) {
            effectiveCooldown = shotCooldown_EGG / 2;
        }
        if (currentTime - lastShotTime_EGG >= effectiveCooldown) {
            lastShotTime_EGG = currentTime;
            return true;
        }
        return false;
    }

    public void update(long fps) {
        // Update power-up timers
        long now = System.currentTimeMillis();
        if (tempRapidFire && now > tempRapidFireEnd) tempRapidFire = false;
        if (tempShield && now > tempShieldEnd) tempShield = false;
        if (tempScoreBoost && now > tempScoreBoostEnd) tempScoreBoost = false;

        // Update shield pulse animation
        if (isShielded()) {
            shieldPulse += 3f / Math.max(1, fps);
            if (shieldPulse > 2 * Math.PI) shieldPulse -= 2 * (float) Math.PI;
        }

        rect.left = x;
        rect.right = x + length_EGG;
        rect.top = y;
        rect.bottom = y + height_EGG;
    }

    // Draw shield effect
    public void drawEffects(Canvas canvas, Paint paint) {
        if (isShielded()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(shieldPulse);
            int alpha = (int) (100 + 80 * pulse);
            paint.setColor(Color.argb(alpha, 0, 150, 255));
            float cx = x + length_EGG / 2;
            float cy = y + height_EGG / 2;
            float radius = Math.max(length_EGG, height_EGG) * 0.7f;
            canvas.drawCircle(cx, cy, radius, paint);
        }

        // Draw active buff indicators
        float indicatorY = y - 15;
        float indicatorX = x;
        if (tempRapidFire && System.currentTimeMillis() < tempRapidFireEnd) {
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(indicatorX, indicatorY, 6, paint);
            indicatorX += 18;
        }
        if (tempScoreBoost && System.currentTimeMillis() < tempScoreBoostEnd) {
            paint.setColor(Color.rgb(255, 215, 0));
            canvas.drawCircle(indicatorX, indicatorY, 6, paint);
        }
    }
}
