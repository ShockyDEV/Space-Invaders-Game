package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

/**
 * Controls unique boss mechanics, attacks, and phases.
 * Each zone boss (0-9) has distinct AI patterns.
 * Mini-bosses (-1) use a simplified attack set.
 */
public class BossController {

    private int bossType;
    private Invader boss;
    private int screenX, screenY;
    private Random rng = new Random();

    // Boss AI state
    private long lastAttackTime = 0;
    private long attackCooldown = 2000; // ms between special attacks
    private int phase = 0; // Multi-phase boss tracking (Overlord has 3)
    private boolean isInvulnerable = false;
    private long invulnerableEnd = 0;

    // Boss-specific state
    private boolean teleporting = false;
    private long teleportEndTime = 0;
    private float beamX = -1; // Solar Drake beam position
    private long beamWarningEnd = 0;
    private boolean beamActive = false;
    private float frozenZoneY = -1; // Cryo Titan freeze zone
    private boolean[] lightningZones = new boolean[3]; // Storm Lord zones
    private long lightningEndTime = 0;
    private boolean invisible = false; // Void Reaper
    private long invisibleToggleTime = 0;
    private long minionSpawnTime = 0; // Hive Queen
    private boolean timeWarpActive = false; // Chrono Lord
    private long timeWarpEnd = 0;

    // Attack tracking for mini-bosses
    private int attackPattern = 0;

    public BossController(int bossType, Invader boss, int screenX, int screenY) {
        this.bossType = bossType;
        this.boss = boss;
        this.screenX = screenX;
        this.screenY = screenY;
        this.lastAttackTime = System.currentTimeMillis();
        this.invisibleToggleTime = System.currentTimeMillis() + 5000; // start visible

        // Set attack cooldowns per boss type
        switch (bossType) {
            case LevelGenerator.BOSS_SENTINEL: attackCooldown = 2500; break;
            case LevelGenerator.BOSS_ROCK_GOLEM: attackCooldown = 8000; break;
            case LevelGenerator.BOSS_NEBULA_WRAITH: attackCooldown = 4000; break;
            case LevelGenerator.BOSS_SOLAR_DRAKE: attackCooldown = 6000; break;
            case LevelGenerator.BOSS_CRYO_TITAN: attackCooldown = 5000; break;
            case LevelGenerator.BOSS_STORM_LORD: attackCooldown = 5500; break;
            case LevelGenerator.BOSS_VOID_REAPER: attackCooldown = 3000; break;
            case LevelGenerator.BOSS_HIVE_QUEEN: attackCooldown = 8000; break;
            case LevelGenerator.BOSS_CHRONO_LORD: attackCooldown = 7000; break;
            case LevelGenerator.BOSS_OVERLORD: attackCooldown = 3000; break;
            case LevelGenerator.BOSS_MINI: attackCooldown = 3000; break;
        }
    }

    /**
     * Update boss AI each frame. Returns a BossAction describing what happened.
     */
    public BossAction update(long fps, float playerX, GameState state) {
        if (boss == null || !boss.getVisibility()) return BossAction.NONE;
        long now = System.currentTimeMillis();

        // Update invulnerability
        if (isInvulnerable && now > invulnerableEnd) {
            isInvulnerable = false;
        }

        // Update Overlord phases based on HP
        if (bossType == LevelGenerator.BOSS_OVERLORD) {
            float hpPercent = (float) boss.getHealth() / boss.getMaxHealth();
            if (hpPercent <= 0.33f) phase = 2;
            else if (hpPercent <= 0.66f) phase = 1;
            else phase = 0;
        }

        // Check if it's time for a special attack
        if (now - lastAttackTime < attackCooldown) {
            return updatePassiveMechanics(now, fps, playerX);
        }

        lastAttackTime = now;
        return executeAttack(now, playerX, state);
    }

    private BossAction updatePassiveMechanics(long now, long fps, float playerX) {
        // Teleport completion
        if (teleporting && now > teleportEndTime) {
            teleporting = false;
            // Appear at random position
            boss.setX(rng.nextFloat() * (screenX - boss.getLength()));
            return BossAction.createVFX(BossAction.VFX_TELEPORT_ARRIVE,
                    boss.getX() + boss.getLength() / 2,
                    boss.getY() + boss.getHeight() / 2);
        }

        // Void Reaper: toggle visibility
        if (bossType == LevelGenerator.BOSS_VOID_REAPER && now > invisibleToggleTime) {
            invisible = !invisible;
            invisibleToggleTime = now + (invisible ? 3000 : 5000);
            return invisible ? BossAction.createVFX(BossAction.VFX_VANISH,
                    boss.getX() + boss.getLength() / 2,
                    boss.getY() + boss.getHeight() / 2) : BossAction.NONE;
        }

        // Solar Drake: beam warning -> active
        if (bossType == LevelGenerator.BOSS_SOLAR_DRAKE && beamWarningEnd > 0) {
            if (now > beamWarningEnd && !beamActive) {
                beamActive = true;
                return BossAction.createBeam(beamX, boss.getY() + boss.getHeight());
            }
            if (beamActive && now > beamWarningEnd + 1500) {
                beamActive = false;
                beamWarningEnd = 0;
                beamX = -1;
            }
        }

        // Storm Lord: lightning expires
        if (bossType == LevelGenerator.BOSS_STORM_LORD && now > lightningEndTime) {
            lightningZones[0] = lightningZones[1] = lightningZones[2] = false;
        }

        // Chrono Lord: time warp expires
        if (timeWarpActive && now > timeWarpEnd) {
            timeWarpActive = false;
            return BossAction.createVFX(BossAction.VFX_TIME_WARP_END, screenX / 2f, screenY / 2f);
        }

        return BossAction.NONE;
    }

    private BossAction executeAttack(long now, float playerX, GameState state) {
        float bx = boss.getX() + boss.getLength() / 2;
        float by = boss.getY() + boss.getHeight();

        switch (bossType) {
            case LevelGenerator.BOSS_MINI:
                // Mini-boss: alternates double-shot and speed burst
                attackPattern = (attackPattern + 1) % 2;
                if (attackPattern == 0) {
                    return BossAction.createMultiShot(bx, by, 2);
                } else {
                    return BossAction.createSpeedBurst(1.5f, 2000);
                }

            case LevelGenerator.BOSS_SENTINEL:
                // Sentinel: fires 2 bullets at once, simple side-to-side
                return BossAction.createMultiShot(bx, by, 2);

            case LevelGenerator.BOSS_ROCK_GOLEM:
                // Rock Golem: alternates invulnerable shield phase (3s) / vulnerable (5s)
                isInvulnerable = true;
                invulnerableEnd = now + 3000;
                return BossAction.createShieldPhase(bx, by - 20);

            case LevelGenerator.BOSS_NEBULA_WRAITH:
                // Teleport to new position + homing bullet
                teleporting = true;
                teleportEndTime = now + 800;
                return BossAction.createTeleportAndShoot(bx, by);

            case LevelGenerator.BOSS_SOLAR_DRAKE:
                // Beam sweep: warning line for 1s, then damage
                beamX = playerX; // Target player's current X
                beamWarningEnd = now + 1000;
                beamActive = false;
                return BossAction.createBeamWarning(beamX, by);

            case LevelGenerator.BOSS_CRYO_TITAN:
                // Frost zone slows player + spread shots
                frozenZoneY = screenY * 0.7f;
                return BossAction.createFrostZone(screenX / 2f, frozenZoneY, 3);

            case LevelGenerator.BOSS_STORM_LORD:
                // Chain lightning in random zones
                int zone1 = rng.nextInt(3);
                int zone2 = (zone1 + 1 + rng.nextInt(2)) % 3;
                lightningZones[zone1] = true;
                lightningZones[zone2] = true;
                lightningEndTime = now + 2000;
                return BossAction.createLightning(zone1, zone2, screenX, by);

            case LevelGenerator.BOSS_VOID_REAPER:
                // Fast burst of bullets
                return BossAction.createMultiShot(bx, by, 3);

            case LevelGenerator.BOSS_HIVE_QUEEN:
                // Spawn 4 minions
                minionSpawnTime = now;
                return BossAction.createSpawnMinions(bx, by, 4);

            case LevelGenerator.BOSS_CHRONO_LORD:
                // Time warp: speed enemies, slow player bullets
                timeWarpActive = true;
                timeWarpEnd = now + 4000;
                return BossAction.createTimeWarp(4000);

            case LevelGenerator.BOSS_OVERLORD:
                return overlordAttack(now, bx, by);

            default:
                return BossAction.createMultiShot(bx, by, 1);
        }
    }

    /** Overlord has 3 phases with escalating attacks */
    private BossAction overlordAttack(long now, float bx, float by) {
        switch (phase) {
            case 0: // Phase 1: shield + minions
                if (rng.nextBoolean()) {
                    isInvulnerable = true;
                    invulnerableEnd = now + 2000;
                    return BossAction.createShieldPhase(bx, by);
                } else {
                    return BossAction.createSpawnMinions(bx, by, 2);
                }
            case 1: // Phase 2: beam + teleport
                if (rng.nextBoolean()) {
                    beamX = bx;
                    beamWarningEnd = now + 800;
                    beamActive = false;
                    return BossAction.createBeamWarning(bx, by);
                } else {
                    teleporting = true;
                    teleportEndTime = now + 600;
                    return BossAction.createTeleportAndShoot(bx, by);
                }
            case 2: // Phase 3: berserk - everything
                int attack = rng.nextInt(3);
                if (attack == 0) {
                    return BossAction.createMultiShot(bx, by, 5);
                } else if (attack == 1) {
                    int z1 = rng.nextInt(3);
                    lightningZones[z1] = true;
                    lightningEndTime = now + 1500;
                    return BossAction.createLightning(z1, (z1 + 1) % 3, screenX, by);
                } else {
                    return BossAction.createSpawnMinions(bx, by, 3);
                }
            default:
                return BossAction.createMultiShot(bx, by, 2);
        }
    }

    // --- State queries ---

    public boolean isInvulnerable() { return isInvulnerable; }
    public boolean isInvisible() { return invisible; }
    public boolean isTeleporting() { return teleporting; }
    public boolean isBeamActive() { return beamActive; }
    public float getBeamX() { return beamX; }
    public boolean isTimeWarpActive() { return timeWarpActive; }
    public float getFrostZoneY() { return frozenZoneY; }
    public boolean[] getLightningZones() { return lightningZones; }
    public int getPhase() { return phase; }
    public int getBossType() { return bossType; }

    /** Draw boss-specific visual effects (shield glow, beam, frost zone, etc.) */
    public void drawEffects(Canvas canvas, Paint paint) {
        if (boss == null || !boss.getVisibility()) return;
        float bx = boss.getX() + boss.getLength() / 2;
        float by = boss.getY() + boss.getHeight() / 2;

        // Invulnerable shield glow
        if (isInvulnerable) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 200.0);
            paint.setColor(Color.argb((int) (80 * pulse), 100, 200, 255));
            canvas.drawCircle(bx, by, boss.getLength() * 0.7f, paint);
            paint.setColor(Color.argb((int) (40 * pulse), 200, 230, 255));
            canvas.drawCircle(bx, by, boss.getLength() * 0.9f, paint);
        }

        // Void Reaper invisibility (dim the boss)
        if (invisible) {
            paint.setColor(Color.argb(100, 0, 0, 0));
            canvas.drawRect(boss.getX(), boss.getY(),
                    boss.getX() + boss.getLength(),
                    boss.getY() + boss.getHeight(), paint);
        }

        // Solar Drake beam warning line
        if (beamX >= 0 && !beamActive) {
            float pulse = 0.3f + 0.7f * (float) Math.sin(System.currentTimeMillis() / 100.0);
            paint.setColor(Color.argb((int) (150 * pulse), 255, 100, 0));
            canvas.drawRect(beamX - 3, boss.getY() + boss.getHeight(),
                    beamX + 3, screenY, paint);
        }

        // Solar Drake active beam
        if (beamActive && beamX >= 0) {
            // Outer glow
            paint.setColor(Color.argb(60, 255, 150, 0));
            canvas.drawRect(beamX - 30, boss.getY() + boss.getHeight(),
                    beamX + 30, screenY, paint);
            // Inner beam
            paint.setColor(Color.argb(180, 255, 200, 50));
            canvas.drawRect(beamX - 10, boss.getY() + boss.getHeight(),
                    beamX + 10, screenY, paint);
            // Core
            paint.setColor(Color.argb(255, 255, 255, 200));
            canvas.drawRect(beamX - 3, boss.getY() + boss.getHeight(),
                    beamX + 3, screenY, paint);
        }

        // Cryo Titan frost zone
        if (frozenZoneY > 0) {
            long age = System.currentTimeMillis() - lastAttackTime;
            if (age < 5000) { // Frost lasts 5 seconds
                float alpha = 1.0f - age / 5000f;
                paint.setColor(Color.argb((int) (60 * alpha), 100, 220, 255));
                canvas.drawRect(0, frozenZoneY, screenX, screenY, paint);
                // Ice crystals
                paint.setColor(Color.argb((int) (120 * alpha), 180, 240, 255));
                for (int i = 0; i < 5; i++) {
                    float cx = screenX * (i + 0.5f) / 5f;
                    canvas.drawCircle(cx, frozenZoneY + 20, 8, paint);
                }
            } else {
                frozenZoneY = -1;
            }
        }

        // Storm Lord lightning zones
        float zoneWidth = screenX / 3f;
        for (int i = 0; i < 3; i++) {
            if (lightningZones[i]) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 80.0 + i);
                paint.setColor(Color.argb((int) (70 * pulse), 200, 200, 255));
                canvas.drawRect(i * zoneWidth, 0, (i + 1) * zoneWidth, screenY, paint);
                // Lightning bolt lines
                paint.setColor(Color.argb((int) (200 * pulse), 220, 220, 255));
                paint.setStrokeWidth(3);
                float lx = i * zoneWidth + zoneWidth / 2 + (float) Math.sin(System.currentTimeMillis() / 50.0) * 20;
                canvas.drawLine(lx, 0, lx + 15, screenY * 0.3f, paint);
                canvas.drawLine(lx + 15, screenY * 0.3f, lx - 10, screenY * 0.6f, paint);
                canvas.drawLine(lx - 10, screenY * 0.6f, lx + 5, screenY, paint);
                paint.setStrokeWidth(1);
            }
        }

        // Phase indicator for Overlord
        if (bossType == LevelGenerator.BOSS_OVERLORD && phase > 0) {
            paint.setColor(phase == 2 ? Color.RED : Color.YELLOW);
            paint.setTextSize(20);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("PHASE " + (phase + 1), bx, boss.getY() - 10, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    // --- Boss Action result class ---

    public static class BossAction {
        // Action types
        public static final int TYPE_NONE = 0;
        public static final int TYPE_MULTI_SHOT = 1;
        public static final int TYPE_SHIELD_PHASE = 2;
        public static final int TYPE_TELEPORT_SHOOT = 3;
        public static final int TYPE_BEAM_WARNING = 4;
        public static final int TYPE_BEAM = 5;
        public static final int TYPE_FROST_ZONE = 6;
        public static final int TYPE_LIGHTNING = 7;
        public static final int TYPE_SPAWN_MINIONS = 8;
        public static final int TYPE_TIME_WARP = 9;
        public static final int TYPE_SPEED_BURST = 10;
        public static final int TYPE_VFX = 11;

        public static final BossAction NONE = new BossAction(TYPE_NONE);

        // VFX subtypes
        public static final int VFX_TELEPORT_ARRIVE = 0;
        public static final int VFX_VANISH = 1;
        public static final int VFX_TIME_WARP_END = 2;

        public final int type;
        public float x, y;
        public int count; // bullets, minions, etc.
        public int zone1, zone2; // lightning zones
        public float speedMult;
        public long duration;
        public int vfxSubtype;

        private BossAction(int type) { this.type = type; }

        static BossAction createMultiShot(float x, float y, int count) {
            BossAction a = new BossAction(TYPE_MULTI_SHOT);
            a.x = x; a.y = y; a.count = count;
            return a;
        }

        static BossAction createShieldPhase(float x, float y) {
            BossAction a = new BossAction(TYPE_SHIELD_PHASE);
            a.x = x; a.y = y;
            return a;
        }

        static BossAction createTeleportAndShoot(float x, float y) {
            BossAction a = new BossAction(TYPE_TELEPORT_SHOOT);
            a.x = x; a.y = y;
            return a;
        }

        static BossAction createBeamWarning(float x, float y) {
            BossAction a = new BossAction(TYPE_BEAM_WARNING);
            a.x = x; a.y = y;
            return a;
        }

        static BossAction createBeam(float x, float y) {
            BossAction a = new BossAction(TYPE_BEAM);
            a.x = x; a.y = y;
            return a;
        }

        static BossAction createFrostZone(float x, float y, int spreadCount) {
            BossAction a = new BossAction(TYPE_FROST_ZONE);
            a.x = x; a.y = y; a.count = spreadCount;
            return a;
        }

        static BossAction createLightning(int z1, int z2, int screenX, float y) {
            BossAction a = new BossAction(TYPE_LIGHTNING);
            a.zone1 = z1; a.zone2 = z2; a.y = y;
            a.x = screenX / 2f;
            return a;
        }

        static BossAction createSpawnMinions(float x, float y, int count) {
            BossAction a = new BossAction(TYPE_SPAWN_MINIONS);
            a.x = x; a.y = y; a.count = count;
            return a;
        }

        static BossAction createTimeWarp(long duration) {
            BossAction a = new BossAction(TYPE_TIME_WARP);
            a.duration = duration;
            return a;
        }

        static BossAction createSpeedBurst(float mult, long duration) {
            BossAction a = new BossAction(TYPE_SPEED_BURST);
            a.speedMult = mult; a.duration = duration;
            return a;
        }

        static BossAction createVFX(int subtype, float x, float y) {
            BossAction a = new BossAction(TYPE_VFX);
            a.vfxSubtype = subtype; a.x = x; a.y = y;
            return a;
        }
    }
}
