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

    // --- New skill flags ---
    private boolean hasHomingMissiles = false;
    private boolean hasChainLightning = false;
    private boolean hasBarrageMode = false;
    private boolean hasPlasmaCannon = false;
    private boolean hasReflectBarrier = false;
    private boolean hasAutoRepair = false;
    private boolean hasFortressMode = false;
    private boolean hasEmergencyWarp = false;
    private boolean hasNanoShield = false;
    private boolean hasMagnetPull = false;
    private boolean hasLuckyDrops = false;
    private boolean hasFreezeWave = false;
    private boolean hasAllyDrone = false;
    private boolean hasTemporalShift = false;
    private boolean hasSalvageBot = false;
    private boolean hasComboMaster = false;
    private boolean hasCriticalHit = false;
    private boolean hasMomentum = false;
    private boolean hasScavenger = false;
    private boolean hasVeteransInstinct = false;
    private boolean hasXPSurge = false;
    private boolean hasBlackHole = false;
    private boolean hasAllySquadron = false;
    private boolean hasTimeStop = false;
    private boolean hasSupernova = false;

    // Cooldown timers for active skills
    private int homingShotCounter = 0;
    private long barrageCooldownEnd = 0;
    private static final long BARRAGE_COOLDOWN = 20000;
    private long emergencyWarpCooldownEnd = 0;
    private static final long EMERGENCY_WARP_COOLDOWN = 10000;
    private long nanoShieldCooldownEnd = 0;
    private static final long NANO_SHIELD_COOLDOWN = 20000;
    private boolean nanoShieldActive = true;
    private long blackHoleCooldownEnd = 0;
    private static final long BLACK_HOLE_COOLDOWN = 30000;
    private long allySquadronCooldownEnd = 0;
    private static final long ALLY_SQUADRON_COOLDOWN = 45000;
    private long allySquadronEnd = 0;
    private static final long ALLY_SQUADRON_DURATION = 10000;
    private long timeStopCooldownEnd = 0;
    private static final long TIME_STOP_COOLDOWN = 60000;
    private long timeStopEnd = 0;
    private static final long TIME_STOP_DURATION = 5000;
    private boolean supernovaUsed = false;
    private boolean autoRepairUsed = false;

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
                case Skill.HOMING_MISSILES:
                    hasHomingMissiles = true;
                    break;
                case Skill.CHAIN_LIGHTNING:
                    hasChainLightning = true;
                    break;
                case Skill.BARRAGE_MODE:
                    hasBarrageMode = true;
                    break;
                case Skill.PLASMA_CANNON:
                    hasPlasmaCannon = true;
                    break;
                case Skill.REFLECT_BARRIER:
                    hasReflectBarrier = true;
                    break;
                case Skill.AUTO_REPAIR:
                    hasAutoRepair = true;
                    break;
                case Skill.FORTRESS_MODE:
                    hasFortressMode = true;
                    break;
                case Skill.EMERGENCY_WARP:
                    hasEmergencyWarp = true;
                    break;
                case Skill.NANO_SHIELD:
                    hasNanoShield = true;
                    nanoShieldActive = true;
                    break;
                case Skill.MAGNET_PULL:
                    hasMagnetPull = true;
                    break;
                case Skill.LUCKY_DROPS:
                    hasLuckyDrops = true;
                    break;
                case Skill.FREEZE_WAVE:
                    hasFreezeWave = true;
                    break;
                case Skill.ALLY_DRONE:
                    hasAllyDrone = true;
                    break;
                case Skill.TEMPORAL_SHIFT:
                    hasTemporalShift = true;
                    break;
                case Skill.SALVAGE_BOT:
                    hasSalvageBot = true;
                    break;
                case Skill.COMBO_MASTER:
                    hasComboMaster = true;
                    break;
                case Skill.CRITICAL_HIT:
                    hasCriticalHit = true;
                    break;
                case Skill.MOMENTUM:
                    hasMomentum = true;
                    break;
                case Skill.SCAVENGER:
                    hasScavenger = true;
                    break;
                case Skill.VETERANS_INSTINCT:
                    hasVeteransInstinct = true;
                    break;
                case Skill.XP_SURGE:
                    hasXPSurge = true;
                    break;
                case Skill.BLACK_HOLE:
                    hasBlackHole = true;
                    break;
                case Skill.ALLY_SQUADRON:
                    hasAllySquadron = true;
                    break;
                case Skill.TIME_STOP:
                    hasTimeStop = true;
                    break;
                case Skill.SUPERNOVA:
                    hasSupernova = true;
                    supernovaUsed = false;
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

    // New skill getters
    public boolean hasHomingMissilesSkill() { return hasHomingMissiles; }
    public boolean hasChainLightningSkill() { return hasChainLightning; }
    public boolean hasBarrageModeSkill() { return hasBarrageMode; }
    public boolean hasPlasmaCannonSkill() { return hasPlasmaCannon; }
    public boolean hasReflectBarrierSkill() { return hasReflectBarrier; }
    public boolean hasAutoRepairSkill() { return hasAutoRepair; }
    public boolean hasFortressModeSkill() { return hasFortressMode; }
    public boolean hasEmergencyWarpSkill() { return hasEmergencyWarp; }
    public boolean hasNanoShieldSkill() { return hasNanoShield; }
    public boolean hasMagnetPullSkill() { return hasMagnetPull; }
    public boolean hasLuckyDropsSkill() { return hasLuckyDrops; }
    public boolean hasFreezeWaveSkill() { return hasFreezeWave; }
    public boolean hasAllyDroneSkill() { return hasAllyDrone; }
    public boolean hasTemporalShiftSkill() { return hasTemporalShift; }
    public boolean hasSalvageBotSkill() { return hasSalvageBot; }
    public boolean hasComboMasterSkill() { return hasComboMaster; }
    public boolean hasCriticalHitSkill() { return hasCriticalHit; }
    public boolean hasMomentumSkill() { return hasMomentum; }
    public boolean hasScavengerSkill() { return hasScavenger; }
    public boolean hasVeteransInstinctSkill() { return hasVeteransInstinct; }
    public boolean hasXPSurgeSkill() { return hasXPSurge; }
    public boolean hasBlackHoleSkill() { return hasBlackHole; }
    public boolean hasAllySquadronSkill() { return hasAllySquadron; }
    public boolean hasTimeStopSkill() { return hasTimeStop; }
    public boolean hasSupernovaSkill() { return hasSupernova; }

    // --- Homing missile shot counter ---
    public boolean checkHomingShot() {
        if (!hasHomingMissiles) return false;
        homingShotCounter++;
        if (homingShotCounter >= 5) {
            homingShotCounter = 0;
            return true;
        }
        return false;
    }

    // --- Barrage ---
    public boolean tryBarrage() {
        if (!hasBarrageMode) return false;
        if (System.currentTimeMillis() < barrageCooldownEnd) return false;
        barrageCooldownEnd = System.currentTimeMillis() + BARRAGE_COOLDOWN;
        return true;
    }
    public long getBarrageCooldownRemaining() {
        return Math.max(0, barrageCooldownEnd - System.currentTimeMillis());
    }

    // --- Emergency Warp ---
    public boolean tryEmergencyWarp() {
        if (!hasEmergencyWarp) return false;
        if (System.currentTimeMillis() < emergencyWarpCooldownEnd) return false;
        emergencyWarpCooldownEnd = System.currentTimeMillis() + EMERGENCY_WARP_COOLDOWN;
        // Warp to random safe X position
        x = (float) (Math.random() * (screenX - length_EGG));
        targetX = x;
        return true;
    }

    // --- Nano Shield ---
    public boolean tryNanoShieldAbsorb() {
        if (!hasNanoShield) return false;
        if (!nanoShieldActive) return false;
        nanoShieldActive = false;
        nanoShieldCooldownEnd = System.currentTimeMillis() + NANO_SHIELD_COOLDOWN;
        return true;
    }

    // --- Auto Repair ---
    public boolean tryAutoRepair() {
        if (!hasAutoRepair || autoRepairUsed) return false;
        if (Math.random() < 0.30) {
            autoRepairUsed = true;
            return true;
        }
        return false;
    }

    // --- Black Hole ---
    public boolean tryBlackHole() {
        if (!hasBlackHole) return false;
        if (System.currentTimeMillis() < blackHoleCooldownEnd) return false;
        blackHoleCooldownEnd = System.currentTimeMillis() + BLACK_HOLE_COOLDOWN;
        return true;
    }
    public long getBlackHoleCooldownRemaining() {
        return Math.max(0, blackHoleCooldownEnd - System.currentTimeMillis());
    }

    // --- Ally Squadron ---
    public boolean tryAllySquadron() {
        if (!hasAllySquadron) return false;
        if (System.currentTimeMillis() < allySquadronCooldownEnd) return false;
        allySquadronCooldownEnd = System.currentTimeMillis() + ALLY_SQUADRON_COOLDOWN;
        allySquadronEnd = System.currentTimeMillis() + ALLY_SQUADRON_DURATION;
        return true;
    }
    public boolean isAllySquadronActive() {
        return hasAllySquadron && System.currentTimeMillis() < allySquadronEnd;
    }

    // --- Time Stop ---
    public boolean tryTimeStop() {
        if (!hasTimeStop) return false;
        if (System.currentTimeMillis() < timeStopCooldownEnd) return false;
        timeStopCooldownEnd = System.currentTimeMillis() + TIME_STOP_COOLDOWN;
        timeStopEnd = System.currentTimeMillis() + TIME_STOP_DURATION;
        return true;
    }
    public boolean isTimeStopActive() {
        return hasTimeStop && System.currentTimeMillis() < timeStopEnd;
    }
    public long getTimeStopCooldownRemaining() {
        return Math.max(0, timeStopCooldownEnd - System.currentTimeMillis());
    }

    // --- Supernova ---
    public boolean trySupernova() {
        if (!hasSupernova || supernovaUsed) return false;
        supernovaUsed = true;
        return true;
    }
    public boolean isSupernovaAvailable() { return hasSupernova && !supernovaUsed; }

    // --- Critical Hit ---
    public boolean rollCriticalHit() {
        return hasCriticalHit && Math.random() < 0.15;
    }

    // --- Reflect Barrier ---
    public boolean rollReflect() {
        return hasReflectBarrier && Math.random() < 0.15;
    }

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

        // Nano shield recharge
        if (hasNanoShield && !nanoShieldActive && now > nanoShieldCooldownEnd) {
            nanoShieldActive = true;
        }

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
