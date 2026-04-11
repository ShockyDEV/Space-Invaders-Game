package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Advanced visual effects manager: animated starfield, screen flashes,
 * shockwave rings, bullet trails, ship thruster, combo aura, ambient dust.
 */
public class VFXManager {

    private final int screenX, screenY;
    private final Random rng = new Random();

    // === Starfield (3-layer parallax) ===
    private static final int STAR_COUNT_FAR = 60;
    private static final int STAR_COUNT_MID = 35;
    private static final int STAR_COUNT_NEAR = 15;

    private final float[] starX, starY, starSpeed, starSize;
    private final int[] starColor;
    private final int totalStars;

    // === Screen Flash ===
    private int flashColor = 0;
    private float flashAlpha = 0;
    private float flashDecay = 0;

    // === Shockwave Rings ===
    private final List<Shockwave> shockwaves = new ArrayList<>();

    // === Bullet Trails ===
    private final List<Trail> trails = new ArrayList<>();

    // === Ambient Dust ===
    private static final int DUST_COUNT = 25;
    private final float[] dustX, dustY, dustSpeed, dustSize, dustAlpha;

    // === Combo Aura (screen edge glow) ===
    private float comboIntensity = 0;
    private float targetComboIntensity = 0;

    // === Ship Thruster ===
    private final List<ThrusterParticle> thrusterParticles = new ArrayList<>();

    // --- Inner classes ---

    private static class Shockwave {
        float x, y;
        float radius, maxRadius;
        float life; // 1.0 -> 0.0
        int color;
        float thickness;

        Shockwave(float x, float y, float maxRadius, int color, float thickness) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.thickness = thickness;
            this.radius = 0;
            this.life = 1.0f;
        }
    }

    private static class Trail {
        float x, y;
        float alpha;
        int color;
        float width, height;

        Trail(float x, float y, int color, float width, float height) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.width = width;
            this.height = height;
            this.alpha = 0.6f;
        }
    }

    private static class ThrusterParticle {
        float x, y, vx, vy;
        float life;
        float size;

        ThrusterParticle(float x, float y) {
            this.x = x;
            this.y = y;
            Random r = new Random();
            this.vx = (r.nextFloat() - 0.5f) * 60f;
            this.vy = 80f + r.nextFloat() * 120f; // Downward
            this.life = 0.4f + r.nextFloat() * 0.3f;
            this.size = 2f + r.nextFloat() * 4f;
        }
    }

    // === Constructor ===

    public VFXManager(int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;

        // Init starfield
        totalStars = STAR_COUNT_FAR + STAR_COUNT_MID + STAR_COUNT_NEAR;
        starX = new float[totalStars];
        starY = new float[totalStars];
        starSpeed = new float[totalStars];
        starSize = new float[totalStars];
        starColor = new int[totalStars];

        int idx = 0;
        // Far stars: dim, slow, small
        for (int i = 0; i < STAR_COUNT_FAR; i++, idx++) {
            starX[idx] = rng.nextFloat() * screenX;
            starY[idx] = rng.nextFloat() * screenY;
            starSpeed[idx] = 8f + rng.nextFloat() * 12f;
            starSize[idx] = 0.8f + rng.nextFloat() * 1.2f;
            int bright = 80 + rng.nextInt(60);
            starColor[idx] = Color.argb(bright, 180, 180, 220);
        }
        // Mid stars: medium
        for (int i = 0; i < STAR_COUNT_MID; i++, idx++) {
            starX[idx] = rng.nextFloat() * screenX;
            starY[idx] = rng.nextFloat() * screenY;
            starSpeed[idx] = 20f + rng.nextFloat() * 25f;
            starSize[idx] = 1.5f + rng.nextFloat() * 1.5f;
            int bright = 140 + rng.nextInt(60);
            int tint = rng.nextInt(3);
            if (tint == 0) starColor[idx] = Color.argb(bright, 200, 200, 255);
            else if (tint == 1) starColor[idx] = Color.argb(bright, 255, 220, 180);
            else starColor[idx] = Color.argb(bright, 220, 255, 220);
        }
        // Near stars: bright, fast, larger
        for (int i = 0; i < STAR_COUNT_NEAR; i++, idx++) {
            starX[idx] = rng.nextFloat() * screenX;
            starY[idx] = rng.nextFloat() * screenY;
            starSpeed[idx] = 50f + rng.nextFloat() * 40f;
            starSize[idx] = 2.5f + rng.nextFloat() * 2f;
            starColor[idx] = Color.argb(200 + rng.nextInt(55), 255, 255, 255);
        }

        // Init ambient dust
        dustX = new float[DUST_COUNT];
        dustY = new float[DUST_COUNT];
        dustSpeed = new float[DUST_COUNT];
        dustSize = new float[DUST_COUNT];
        dustAlpha = new float[DUST_COUNT];
        for (int i = 0; i < DUST_COUNT; i++) {
            resetDust(i);
            dustY[i] = rng.nextFloat() * screenY; // Random starting position
        }
    }

    private void resetDust(int i) {
        dustX[i] = rng.nextFloat() * screenX;
        dustY[i] = -rng.nextFloat() * 50;
        dustSpeed[i] = 15f + rng.nextFloat() * 25f;
        dustSize[i] = 1f + rng.nextFloat() * 2f;
        dustAlpha[i] = 0.1f + rng.nextFloat() * 0.25f;
    }

    // === Trigger Methods ===

    public void triggerScreenFlash(int color, float intensity) {
        flashColor = color;
        flashAlpha = intensity;
        flashDecay = intensity * 3f; // Faster decay for brighter flashes
    }

    public void addShockwave(float x, float y, float maxRadius, int color, float thickness) {
        shockwaves.add(new Shockwave(x, y, maxRadius, color, thickness));
    }

    public void addBulletTrail(float x, float y, int color, float width, float height) {
        trails.add(new Trail(x, y, color, width, height));
    }

    public void setComboIntensity(int comboCount) {
        if (comboCount >= 10) targetComboIntensity = 0.4f;
        else if (comboCount >= 6) targetComboIntensity = 0.25f;
        else if (comboCount >= 3) targetComboIntensity = 0.12f;
        else targetComboIntensity = 0;
    }

    public void emitThruster(float shipX, float shipCenterX, float shipTopY) {
        // Emit 1-2 particles per frame from ship bottom
        int count = 1 + rng.nextInt(2);
        for (int i = 0; i < count; i++) {
            float px = shipCenterX + (rng.nextFloat() - 0.5f) * 20f;
            thrusterParticles.add(new ThrusterParticle(px, shipTopY));
        }
    }

    // === Update ===

    public void update(long fps) {
        if (fps <= 0) return;
        float dt = 1f / fps;

        // Starfield scroll
        for (int i = 0; i < totalStars; i++) {
            starY[i] += starSpeed[i] * dt;
            if (starY[i] > screenY) {
                starY[i] = -2;
                starX[i] = rng.nextFloat() * screenX;
            }
        }

        // Screen flash decay
        if (flashAlpha > 0) {
            flashAlpha -= flashDecay * dt;
            if (flashAlpha < 0) flashAlpha = 0;
        }

        // Shockwaves
        Iterator<Shockwave> swIt = shockwaves.iterator();
        while (swIt.hasNext()) {
            Shockwave sw = swIt.next();
            sw.life -= 1.5f * dt;
            sw.radius = sw.maxRadius * (1f - sw.life);
            if (sw.life <= 0) swIt.remove();
        }

        // Bullet trails fade
        Iterator<Trail> tIt = trails.iterator();
        while (tIt.hasNext()) {
            Trail t = tIt.next();
            t.alpha -= 4f * dt;
            if (t.alpha <= 0) tIt.remove();
        }

        // Ambient dust
        for (int i = 0; i < DUST_COUNT; i++) {
            dustY[i] += dustSpeed[i] * dt;
            if (dustY[i] > screenY + 10) {
                resetDust(i);
            }
        }

        // Combo aura lerp
        comboIntensity += (targetComboIntensity - comboIntensity) * 3f * dt;

        // Thruster particles
        Iterator<ThrusterParticle> thIt = thrusterParticles.iterator();
        while (thIt.hasNext()) {
            ThrusterParticle tp = thIt.next();
            tp.x += tp.vx * dt;
            tp.y += tp.vy * dt;
            tp.life -= dt;
            tp.size *= 0.97f;
            if (tp.life <= 0) thIt.remove();
        }
    }

    // === Draw Methods ===

    /** Draw starfield behind everything */
    public void drawStarfield(Canvas canvas, Paint paint) {
        for (int i = 0; i < totalStars; i++) {
            paint.setColor(starColor[i]);
            // Twinkle effect for near stars
            if (i >= STAR_COUNT_FAR + STAR_COUNT_MID) {
                float twinkle = 0.6f + 0.4f * (float) Math.sin(
                        System.currentTimeMillis() * 0.003 + i * 1.7);
                int a = (int) (Color.alpha(starColor[i]) * twinkle);
                paint.setAlpha(a);
            }
            canvas.drawCircle(starX[i], starY[i], starSize[i], paint);
        }
    }

    /** Draw ambient dust particles */
    public void drawDust(Canvas canvas, Paint paint) {
        for (int i = 0; i < DUST_COUNT; i++) {
            int alpha = (int) (255 * dustAlpha[i]);
            paint.setColor(Color.argb(alpha, 200, 180, 255));
            canvas.drawCircle(dustX[i], dustY[i], dustSize[i], paint);
        }
    }

    /** Draw thruster flame behind player ship */
    public void drawThruster(Canvas canvas, Paint paint) {
        for (ThrusterParticle tp : thrusterParticles) {
            float lifeRatio = tp.life / 0.6f;
            // Color shifts from white-yellow to orange-red as it ages
            int r = 255;
            int g = (int) (255 * Math.min(1f, lifeRatio * 1.5f));
            int b = (int) (100 * lifeRatio);
            int alpha = (int) (220 * lifeRatio);
            paint.setColor(Color.argb(Math.max(0, alpha), r, Math.max(0, g), Math.max(0, b)));
            canvas.drawCircle(tp.x, tp.y, tp.size, paint);

            // Inner bright core
            if (lifeRatio > 0.5f) {
                paint.setColor(Color.argb((int) (150 * lifeRatio), 255, 255, 200));
                canvas.drawCircle(tp.x, tp.y, tp.size * 0.5f, paint);
            }
        }
    }

    /** Draw bullet trails */
    public void drawTrails(Canvas canvas, Paint paint) {
        for (Trail t : trails) {
            int r = Color.red(t.color);
            int g = Color.green(t.color);
            int b = Color.blue(t.color);
            paint.setColor(Color.argb((int) (255 * t.alpha * 0.3f), r, g, b));
            canvas.drawRect(t.x - t.width * 0.8f, t.y,
                    t.x + t.width * 1.8f, t.y + t.height, paint);
        }
    }

    /** Draw shockwave rings */
    public void drawShockwaves(Canvas canvas, Paint paint) {
        for (Shockwave sw : shockwaves) {
            int r = Color.red(sw.color);
            int g = Color.green(sw.color);
            int b = Color.blue(sw.color);
            int alpha = (int) (200 * sw.life);

            // Outer ring
            paint.setColor(Color.argb(alpha / 2, r, g, b));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sw.thickness * sw.life);
            canvas.drawCircle(sw.x, sw.y, sw.radius, paint);

            // Inner glow ring
            paint.setColor(Color.argb(alpha, r, g, b));
            paint.setStrokeWidth(sw.thickness * sw.life * 0.4f);
            canvas.drawCircle(sw.x, sw.y, sw.radius * 0.85f, paint);

            paint.setStyle(Paint.Style.FILL);
        }
    }

    /** Draw screen flash overlay */
    public void drawScreenFlash(Canvas canvas, Paint paint) {
        if (flashAlpha <= 0.01f) return;
        int r = Color.red(flashColor);
        int g = Color.green(flashColor);
        int b = Color.blue(flashColor);
        int alpha = (int) (255 * Math.min(1f, flashAlpha));
        paint.setColor(Color.argb(alpha, r, g, b));
        canvas.drawRect(0, 0, screenX, screenY, paint);
    }

    /** Draw combo aura (glowing edges when combo is high) */
    public void drawComboAura(Canvas canvas, Paint paint) {
        if (comboIntensity < 0.01f) return;

        float pulse = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() * 0.006);
        int alpha = (int) (255 * comboIntensity * pulse);
        float edgeWidth = 40f + comboIntensity * 80f;

        // Top edge
        paint.setShader(new LinearGradient(0, 0, 0, edgeWidth,
                Color.argb(alpha, 255, 200, 50), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, screenX, edgeWidth, paint);

        // Bottom edge
        paint.setShader(new LinearGradient(0, screenY, 0, screenY - edgeWidth,
                Color.argb(alpha, 255, 200, 50), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, screenY - edgeWidth, screenX, screenY, paint);

        // Left edge
        paint.setShader(new LinearGradient(0, 0, edgeWidth, 0,
                Color.argb(alpha / 2, 255, 200, 50), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, edgeWidth, screenY, paint);

        // Right edge
        paint.setShader(new LinearGradient(screenX, 0, screenX - edgeWidth, 0,
                Color.argb(alpha / 2, 255, 200, 50), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(screenX - edgeWidth, 0, screenX, screenY, paint);

        paint.setShader(null);
    }
}
