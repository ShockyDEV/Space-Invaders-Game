package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * AI-controlled ally drone that orbits the player and fires at enemies.
 * Used by Skill 22 (permanent Ally Drone) and Skill 32 (temporary Ally Squadron).
 */
public class AllyDrone {

    private float x, y;
    private float orbitAngle;
    private float orbitRadius;
    private float orbitSpeed; // radians per second
    private long lastFireTime = 0;
    private static final long FIRE_INTERVAL = 2000; // 2 seconds

    // Trail positions for visual trail effect
    private float[] trailX = new float[6];
    private float[] trailY = new float[6];
    private int trailIndex = 0;
    private long lastTrailUpdate = 0;

    private boolean active = true;
    private long expireTime = Long.MAX_VALUE; // permanent by default
    private float size;

    // Muzzle flash
    private long muzzleFlashEnd = 0;

    public AllyDrone(float orbitRadius, float startAngle, float size) {
        this.orbitRadius = orbitRadius;
        this.orbitAngle = startAngle;
        this.orbitSpeed = 1.8f; // ~103 degrees per second
        this.size = size;
        for (int i = 0; i < trailX.length; i++) {
            trailX[i] = -1;
            trailY[i] = -1;
        }
    }

    /** Create a temporary drone that expires after durationMs */
    public static AllyDrone createTemporary(float orbitRadius, float startAngle,
                                            float size, long durationMs) {
        AllyDrone drone = new AllyDrone(orbitRadius, startAngle, size);
        drone.expireTime = System.currentTimeMillis() + durationMs;
        return drone;
    }

    public void update(long fps, float playerCX, float playerCY) {
        if (!active) return;

        // Check expiry
        if (System.currentTimeMillis() > expireTime) {
            active = false;
            return;
        }

        // Orbit around player
        if (fps > 0) {
            orbitAngle += orbitSpeed / fps;
        }
        x = playerCX + (float) Math.cos(orbitAngle) * orbitRadius;
        y = playerCY + (float) Math.sin(orbitAngle) * orbitRadius;

        // Update trail
        long now = System.currentTimeMillis();
        if (now - lastTrailUpdate > 50) { // every 50ms
            trailX[trailIndex] = x;
            trailY[trailIndex] = y;
            trailIndex = (trailIndex + 1) % trailX.length;
            lastTrailUpdate = now;
        }
    }

    /**
     * Try to fire at the nearest enemy. Returns a Bullet if firing, null otherwise.
     * Caller is responsible for adding the bullet to the game.
     */
    public Bullet tryFire(int screenY) {
        if (!active) return null;
        long now = System.currentTimeMillis();
        if (now - lastFireTime < FIRE_INTERVAL) return null;
        lastFireTime = now;
        muzzleFlashEnd = now + 80;

        Bullet b = Bullet.createHoming(screenY);
        b.shoot(x, y, Bullet.UP);
        return b;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        long now = System.currentTimeMillis();

        // Draw trail
        for (int i = 0; i < trailX.length; i++) {
            if (trailX[i] < 0) continue;
            int idx = (trailIndex - 1 - i + trailX.length) % trailX.length;
            float alpha = 1.0f - (float) i / trailX.length;
            paint.setColor(Color.argb((int) (60 * alpha), 0, 200, 255));
            canvas.drawCircle(trailX[idx], trailY[idx], size * 0.3f * alpha, paint);
        }

        // Draw drone body (triangle pointing up)
        Path path = new Path();
        path.moveTo(x, y - size);          // top
        path.lineTo(x - size * 0.7f, y + size * 0.5f); // bottom-left
        path.lineTo(x + size * 0.7f, y + size * 0.5f); // bottom-right
        path.close();

        // Body fill
        paint.setColor(Color.rgb(0, 180, 255));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);

        // Body outline
        paint.setColor(Color.rgb(100, 220, 255));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);

        // Thruster glow (orange-yellow at bottom)
        float thrusterPulse = 0.7f + 0.3f * (float) Math.sin(now / 80.0);
        paint.setColor(Color.argb((int) (180 * thrusterPulse), 255, 150, 0));
        canvas.drawCircle(x, y + size * 0.5f, size * 0.35f * thrusterPulse, paint);
        paint.setColor(Color.argb((int) (120 * thrusterPulse), 255, 255, 100));
        canvas.drawCircle(x, y + size * 0.5f, size * 0.2f * thrusterPulse, paint);

        // Muzzle flash when firing
        if (now < muzzleFlashEnd) {
            float flashAlpha = (muzzleFlashEnd - now) / 80f;
            paint.setColor(Color.argb((int) (200 * flashAlpha), 200, 255, 255));
            canvas.drawCircle(x, y - size * 1.2f, size * 0.5f, paint);
        }

        // Core light
        paint.setColor(Color.argb(150, 150, 230, 255));
        canvas.drawCircle(x, y, size * 0.25f, paint);
    }

    public boolean isActive() { return active; }
    public float getX() { return x; }
    public float getY() { return y; }

    public void deactivate() { active = false; }
}
