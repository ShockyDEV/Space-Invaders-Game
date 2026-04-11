package com.example.spaceinvaders_activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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

    // Speed Boost: lerp factor for ship tracking (0.15 = sluggish, 1.0 = instant snap)
    private float speedFactor = 0.15f;
    private float targetX;

    // Ultimate Laser: hold-to-charge mechanic
    private boolean isCharging = false;
    private long chargeStartTime = 0;
    private float chargeProgress = 0f;
    private long ultimateCooldownEnd = 0;
    private static final long CHARGE_DURATION = 1500; // 1.5 seconds to fully charge
    private static final long ULTIMATE_COOLDOWN = 15000; // 15 second cooldown

    // Temporary power-up buffs
    private boolean tempRapidFire = false;
    private long tempRapidFireEnd = 0;
    private boolean tempShield = false;
    private long tempShieldEnd = 0;
    private boolean tempScoreBoost = false;
    private long tempScoreBoostEnd = 0;

    // Regeneration timer
    private long lastRegenTime = 0;
    private static final long REGEN_INTERVAL = 30000; // 30 seconds

    // Shield visual
    private float shieldPulse = 0;

    // Skin
    private int skinId = 0;
    private int skinTintColor = 0;
    private boolean skinIsRainbow = false;
    private Paint skinPaint;

    private int screenX, screenY;

    public PlayerShip(Context context, int screenX, int screenY) {
        rect = new RectF();
        this.screenX = screenX;
        this.screenY = screenY;

        length_EGG = screenX / 10f;
        height_EGG = screenY / 10f;

        x = screenX / 2f;
        targetX = x;
        y = screenY - height_EGG;

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.playership);
        bitmap = Bitmap.createScaledBitmap(bitmap, (int) length_EGG, (int) height_EGG, false);
    }

    // --- Skin ---

    public void applySkin(int skinId) {
        this.skinId = skinId;
        CosmeticsShop.ShipSkin skin = CosmeticsShop.getSkinById(skinId);
        skinTintColor = skin.tintColor;
        skinIsRainbow = skin.isRainbow;
        skinPaint = new Paint();
        if (skinTintColor != 0) {
            skinPaint.setColorFilter(new PorterDuffColorFilter(skinTintColor, PorterDuff.Mode.MULTIPLY));
        }
    }

    /** Returns a Paint with the current skin tint applied (may be null for default) */
    public Paint getSkinPaint() {
        if (skinIsRainbow) {
            // Cycle through hues over time
            float hue = (System.currentTimeMillis() % 5000) / 5000f * 360f;
            int color = Color.HSVToColor(new float[]{hue, 0.7f, 1.0f});
            if (skinPaint == null) skinPaint = new Paint();
            skinPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        }
        return (skinId != 0 && skinPaint != null) ? skinPaint : null;
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
                    speedFactor = 1.0f; // Instant snap tracking
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

    // --- Ultimate Laser charge mechanic ---

    public void startCharging() {
        if (!hasUltimateLaser || !isUltimateReady()) return;
        isCharging = true;
        chargeStartTime = System.currentTimeMillis();
        chargeProgress = 0f;
    }

    public void updateCharge() {
        if (!isCharging) return;
        long elapsed = System.currentTimeMillis() - chargeStartTime;
        chargeProgress = Math.min(1.0f, elapsed / (float) CHARGE_DURATION);
    }

    public boolean releaseCharge() {
        if (!isCharging || chargeProgress < 1.0f) {
            cancelCharge();
            return false;
        }
        isCharging = false;
        chargeProgress = 0f;
        ultimateCooldownEnd = System.currentTimeMillis() + ULTIMATE_COOLDOWN;
        return true;
    }

    public void cancelCharge() {
        isCharging = false;
        chargeProgress = 0f;
    }

    public boolean isUltimateReady() {
        return hasUltimateLaser && System.currentTimeMillis() > ultimateCooldownEnd;
    }

    public boolean isCurrentlyCharging() { return isCharging; }
    public float getChargeProgress() { return chargeProgress; }

    public long getUltimateCooldownRemaining() {
        long remaining = ultimateCooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

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
        targetX = touchX - length_EGG / 2;
        // Clamp target to screen bounds
        if (targetX < 0) targetX = 0;
        if (targetX > screenX - length_EGG) targetX = screenX - length_EGG;
        // Lerp toward target (speedFactor: 0.15 default, 1.0 with Speed Boost)
        x = x + (targetX - x) * speedFactor;
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

        // Update ultimate laser charge
        updateCharge();

        rect.left = x;
        rect.right = x + length_EGG;
        rect.top = y;
        rect.bottom = y + height_EGG;
    }

    // Draw shield effect, charge indicator, buff indicators
    public void drawEffects(Canvas canvas, Paint paint) {
        float cx = x + length_EGG / 2;
        float cy = y + height_EGG / 2;

        // Shield bubble
        if (isShielded()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(shieldPulse);
            int alpha = (int) (100 + 80 * pulse);
            paint.setColor(Color.argb(alpha, 0, 150, 255));
            float radius = Math.max(length_EGG, height_EGG) * 0.7f;
            canvas.drawCircle(cx, cy, radius, paint);
        }

        // Ultimate laser charge indicator
        if (isCharging) {
            // Growing aura ring around ship
            float maxRadius = Math.max(length_EGG, height_EGG) * 0.9f;
            float radius = maxRadius * chargeProgress;
            int r = (int) (150 + 105 * chargeProgress);
            int b = (int) (255 * chargeProgress);
            paint.setColor(Color.argb((int) (180 * chargeProgress), r, 50, b));
            canvas.drawCircle(cx, cy, radius, paint);

            // Progress arc text
            paint.setColor(Color.argb(220, 255, 200, 255));
            paint.setTextSize(height_EGG * 0.25f);
            paint.setTextAlign(Paint.Align.CENTER);
            int pct = (int) (chargeProgress * 100);
            canvas.drawText("CHARGING " + pct + "%", cx, y - 20, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            if (chargeProgress >= 1.0f) {
                // Ready flash
                float flash = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.015);
                paint.setColor(Color.argb((int) (200 * flash), 255, 100, 255));
                canvas.drawCircle(cx, cy, maxRadius, paint);
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("RELEASE!", cx, y - 20, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }

        // Active buff indicators
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
