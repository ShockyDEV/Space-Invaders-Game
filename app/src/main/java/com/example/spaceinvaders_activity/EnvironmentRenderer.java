package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

/**
 * Renders zone-specific environment effects (background overlays, particles, weather).
 * Each of the 10 zones has a unique visual theme drawn behind the game objects.
 */
public class EnvironmentRenderer {

    private int screenX, screenY;
    private int zone = 0;
    private Random rng = new Random();

    // Asteroid Belt particles (Zone 2)
    private float[] asteroidX, asteroidY, asteroidSize, asteroidSpeed;
    private static final int MAX_ASTEROIDS = 12;

    // Nebula fog (Zone 3)
    private float fogPhase = 0;

    // Solar Flare (Zone 4)
    private long nextFlareTime = 0;
    private long flareEndTime = 0;
    private float flareAlpha = 0;

    // Ice crystals (Zone 5)
    private float[] crystalX, crystalY, crystalSpeed;
    private static final int MAX_CRYSTALS = 8;

    // Lightning (Zone 6)
    private long nextLightningTime = 0;
    private long lightningFlashEnd = 0;
    private float lightningX1, lightningY1, lightningX2, lightningY2;

    // Dark Matter (Zone 7) visibility circle
    private float darkRadius;

    // Hive blobs (Zone 8)
    private float[] blobX, blobY, blobSize, blobSpeed;
    private static final int MAX_BLOBS = 10;

    // Warp distortion (Zone 9)
    private float warpPhase = 0;

    public EnvironmentRenderer(int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;
        darkRadius = screenX * 0.25f;

        // Pre-allocate asteroid particles
        asteroidX = new float[MAX_ASTEROIDS];
        asteroidY = new float[MAX_ASTEROIDS];
        asteroidSize = new float[MAX_ASTEROIDS];
        asteroidSpeed = new float[MAX_ASTEROIDS];
        for (int i = 0; i < MAX_ASTEROIDS; i++) {
            resetAsteroid(i, true);
        }

        // Ice crystals
        crystalX = new float[MAX_CRYSTALS];
        crystalY = new float[MAX_CRYSTALS];
        crystalSpeed = new float[MAX_CRYSTALS];
        for (int i = 0; i < MAX_CRYSTALS; i++) {
            resetCrystal(i, true);
        }

        // Hive blobs
        blobX = new float[MAX_BLOBS];
        blobY = new float[MAX_BLOBS];
        blobSize = new float[MAX_BLOBS];
        blobSpeed = new float[MAX_BLOBS];
        for (int i = 0; i < MAX_BLOBS; i++) {
            resetBlob(i, true);
        }
    }

    public void setZone(int zone) {
        this.zone = zone;
        nextFlareTime = System.currentTimeMillis() + 5000 + rng.nextInt(5000);
        nextLightningTime = System.currentTimeMillis() + 3000 + rng.nextInt(3000);
    }

    public void update(long fps) {
        if (fps <= 0) return;
        long now = System.currentTimeMillis();

        switch (zone) {
            case 1: // Asteroid Belt
                for (int i = 0; i < MAX_ASTEROIDS; i++) {
                    asteroidY[i] += asteroidSpeed[i] / fps;
                    if (asteroidY[i] > screenY + 20) resetAsteroid(i, false);
                }
                break;

            case 2: // Nebula
                fogPhase += 1.0f / fps;
                break;

            case 3: // Solar Flare
                if (now > nextFlareTime && flareAlpha <= 0) {
                    flareAlpha = 1.0f;
                    flareEndTime = now + 600;
                    nextFlareTime = now + 8000 + rng.nextInt(4000);
                }
                if (flareAlpha > 0) {
                    float elapsed = (now - flareEndTime + 600) / 600f;
                    flareAlpha = Math.max(0, 1.0f - elapsed);
                }
                break;

            case 4: // Ice Field
                for (int i = 0; i < MAX_CRYSTALS; i++) {
                    crystalY[i] += crystalSpeed[i] / fps;
                    crystalX[i] += (float) Math.sin(crystalY[i] / 50.0) * 0.5f;
                    if (crystalY[i] > screenY + 10) resetCrystal(i, false);
                }
                break;

            case 5: // Electric Storm
                if (now > nextLightningTime) {
                    lightningX1 = rng.nextFloat() * screenX;
                    lightningY1 = 0;
                    lightningX2 = lightningX1 + (rng.nextFloat() - 0.5f) * screenX * 0.3f;
                    lightningY2 = screenY;
                    lightningFlashEnd = now + 150;
                    nextLightningTime = now + 5000 + rng.nextInt(3000);
                }
                break;

            case 6: // Dark Matter
                // Visibility circle pulses
                darkRadius = screenX * 0.22f + (float) Math.sin(now / 1000.0) * screenX * 0.03f;
                break;

            case 7: // Alien Hive
                for (int i = 0; i < MAX_BLOBS; i++) {
                    blobY[i] += blobSpeed[i] / fps;
                    blobX[i] += (float) Math.sin(blobY[i] / 80.0 + i) * 1.0f;
                    if (blobY[i] > screenY + 20) resetBlob(i, false);
                }
                break;

            case 8: // Warp Zone
                warpPhase += 2.0f / fps;
                break;

            case 9: // Final Frontier (combines effects at 50%)
                // Update asteroids and lightning at reduced rate
                for (int i = 0; i < MAX_ASTEROIDS / 2; i++) {
                    asteroidY[i] += asteroidSpeed[i] * 0.5f / fps;
                    if (asteroidY[i] > screenY + 20) resetAsteroid(i, false);
                }
                if (now > nextLightningTime) {
                    lightningX1 = rng.nextFloat() * screenX;
                    lightningY1 = 0;
                    lightningX2 = lightningX1 + (rng.nextFloat() - 0.5f) * screenX * 0.2f;
                    lightningY2 = screenY;
                    lightningFlashEnd = now + 100;
                    nextLightningTime = now + 8000 + rng.nextInt(5000);
                }
                break;
        }
    }

    /** Draw environment effects behind game objects */
    public void drawBackground(Canvas canvas, Paint paint) {
        long now = System.currentTimeMillis();

        switch (zone) {
            case 1: // Asteroid Belt - gray debris
                paint.setColor(Color.argb(180, 120, 110, 100));
                for (int i = 0; i < MAX_ASTEROIDS; i++) {
                    canvas.drawCircle(asteroidX[i], asteroidY[i], asteroidSize[i], paint);
                    // Rocky texture
                    paint.setColor(Color.argb(100, 90, 80, 70));
                    canvas.drawCircle(asteroidX[i] + 2, asteroidY[i] - 1,
                            asteroidSize[i] * 0.6f, paint);
                    paint.setColor(Color.argb(180, 120, 110, 100));
                }
                break;

            case 2: // Nebula - purple fog at edges
                float fogWave = (float) Math.sin(fogPhase) * 0.3f + 0.5f;
                int fogAlpha = (int) (50 * fogWave);
                // Left edge fog
                paint.setColor(Color.argb(fogAlpha, 100, 30, 150));
                canvas.drawRect(0, 0, screenX * 0.15f, screenY, paint);
                // Right edge fog
                canvas.drawRect(screenX * 0.85f, 0, screenX, screenY, paint);
                // Top/bottom fog
                paint.setColor(Color.argb(fogAlpha / 2, 80, 20, 120));
                canvas.drawRect(0, 0, screenX, screenY * 0.08f, paint);
                canvas.drawRect(0, screenY * 0.92f, screenX, screenY, paint);
                break;

            case 3: // Solar Flare - orange ambient + flashes
                // Constant warm tint
                paint.setColor(Color.argb(15, 255, 150, 50));
                canvas.drawRect(0, 0, screenX, screenY, paint);
                // Flare flash
                if (flareAlpha > 0) {
                    paint.setColor(Color.argb((int) (80 * flareAlpha), 255, 180, 50));
                    canvas.drawRect(0, 0, screenX, screenY, paint);
                }
                break;

            case 4: // Ice Field - cyan crystals + slight blue tint
                paint.setColor(Color.argb(10, 100, 200, 255));
                canvas.drawRect(0, 0, screenX, screenY, paint);
                // Crystals
                paint.setColor(Color.argb(150, 180, 240, 255));
                for (int i = 0; i < MAX_CRYSTALS; i++) {
                    // Diamond shape
                    float cs = crystalSpeed[i] * 0.3f;
                    canvas.drawLine(crystalX[i], crystalY[i] - cs,
                            crystalX[i] + cs * 0.5f, crystalY[i], paint);
                    canvas.drawLine(crystalX[i] + cs * 0.5f, crystalY[i],
                            crystalX[i], crystalY[i] + cs, paint);
                    canvas.drawLine(crystalX[i], crystalY[i] + cs,
                            crystalX[i] - cs * 0.5f, crystalY[i], paint);
                    canvas.drawLine(crystalX[i] - cs * 0.5f, crystalY[i],
                            crystalX[i], crystalY[i] - cs, paint);
                }
                break;

            case 5: // Electric Storm - lightning arcs
                if (now < lightningFlashEnd) {
                    // Flash overlay
                    paint.setColor(Color.argb(40, 200, 200, 255));
                    canvas.drawRect(0, 0, screenX, screenY, paint);
                    // Lightning bolt
                    paint.setColor(Color.argb(220, 220, 220, 255));
                    paint.setStrokeWidth(3);
                    float midX = (lightningX1 + lightningX2) / 2 +
                            (rng.nextFloat() - 0.5f) * 40;
                    float midY = screenY * 0.4f + rng.nextFloat() * screenY * 0.2f;
                    canvas.drawLine(lightningX1, lightningY1, midX, midY, paint);
                    canvas.drawLine(midX, midY, lightningX2, lightningY2, paint);
                    // Branch
                    paint.setColor(Color.argb(150, 180, 180, 255));
                    paint.setStrokeWidth(2);
                    canvas.drawLine(midX, midY,
                            midX + (rng.nextFloat() - 0.5f) * 60,
                            midY + 50 + rng.nextFloat() * 80, paint);
                    paint.setStrokeWidth(1);
                }
                break;

            case 6: // Dark Matter - dark overlay with visibility circle
                // First draw full dark overlay
                paint.setColor(Color.argb(100, 0, 0, 0));
                canvas.drawRect(0, 0, screenX, screenY, paint);
                break;

            case 7: // Alien Hive - green organic blobs
                paint.setColor(Color.argb(8, 0, 150, 50));
                canvas.drawRect(0, 0, screenX, screenY, paint);
                for (int i = 0; i < MAX_BLOBS; i++) {
                    float pulse = 0.6f + 0.4f * (float) Math.sin(now / 500.0 + i);
                    paint.setColor(Color.argb((int) (60 * pulse), 50, 200, 80));
                    canvas.drawCircle(blobX[i], blobY[i], blobSize[i], paint);
                    paint.setColor(Color.argb((int) (30 * pulse), 100, 255, 120));
                    canvas.drawCircle(blobX[i], blobY[i], blobSize[i] * 0.5f, paint);
                }
                break;

            case 8: // Warp Zone - wavy distortion (color shifts)
                float warpWave = (float) Math.sin(warpPhase);
                int warpR = (int) (20 + 15 * warpWave);
                int warpB = (int) (30 + 20 * Math.cos(warpPhase * 0.7));
                paint.setColor(Color.argb(15, warpR, 10, warpB));
                canvas.drawRect(0, 0, screenX, screenY, paint);
                // Horizontal distortion lines
                paint.setColor(Color.argb(20, 100, 50, 200));
                for (int i = 0; i < 6; i++) {
                    float ly = screenY * i / 6f +
                            (float) Math.sin(warpPhase + i) * 15;
                    canvas.drawRect(0, ly, screenX, ly + 2, paint);
                }
                break;

            case 9: // Final Frontier - combined effects at reduced intensity
                // Asteroids (half count)
                paint.setColor(Color.argb(120, 120, 110, 100));
                for (int i = 0; i < MAX_ASTEROIDS / 2; i++) {
                    canvas.drawCircle(asteroidX[i], asteroidY[i], asteroidSize[i] * 0.7f, paint);
                }
                // Slight dark overlay
                paint.setColor(Color.argb(50, 0, 0, 0));
                canvas.drawRect(0, 0, screenX, screenY, paint);
                // Lightning (reuse)
                if (now < lightningFlashEnd) {
                    paint.setColor(Color.argb(25, 200, 200, 255));
                    canvas.drawRect(0, 0, screenX, screenY, paint);
                }
                break;
        }
    }

    /** Draw effects that go on top of game objects (Dark Matter visibility) */
    public void drawForeground(Canvas canvas, Paint paint, float playerCX, float playerCY) {
        if (zone == 6) {
            // Dark Matter: clear a circle around the player
            // Draw 4 dark rects leaving a hole in the center
            // This is a simplified approach without clipping
            paint.setColor(Color.argb(120, 0, 0, 0));
            // We just re-darken everything except near the player
            // by drawing a radial gradient effect with concentric rings
            for (float r = darkRadius; r < screenX; r += 20) {
                float alpha = Math.min(140, 20 + (r - darkRadius) * 0.5f);
                paint.setColor(Color.argb((int) alpha, 0, 0, 0));
                // Draw ring
                canvas.drawCircle(playerCX, playerCY, r + 20, paint);
            }

            // Enemies glow in dark
            // (handled by Invader.drawEffects - we just provide the dark overlay)
        }
    }

    // --- Particle reset helpers ---

    private void resetAsteroid(int i, boolean randomY) {
        asteroidX[i] = rng.nextFloat() * screenX;
        asteroidY[i] = randomY ? rng.nextFloat() * screenY : -20;
        asteroidSize[i] = 4 + rng.nextFloat() * 10;
        asteroidSpeed[i] = 20 + rng.nextFloat() * 40;
    }

    private void resetCrystal(int i, boolean randomY) {
        crystalX[i] = rng.nextFloat() * screenX;
        crystalY[i] = randomY ? rng.nextFloat() * screenY : -10;
        crystalSpeed[i] = 15 + rng.nextFloat() * 25;
    }

    private void resetBlob(int i, boolean randomY) {
        blobX[i] = rng.nextFloat() * screenX;
        blobY[i] = randomY ? rng.nextFloat() * screenY : -20;
        blobSize[i] = 8 + rng.nextFloat() * 15;
        blobSpeed[i] = 10 + rng.nextFloat() * 30;
    }
}
