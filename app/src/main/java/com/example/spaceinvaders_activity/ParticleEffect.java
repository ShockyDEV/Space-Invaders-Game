package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Particle effects system for explosions, score popups, and visual feedback.
 */
public class ParticleEffect {

    private static final Random random = new Random();

    // --- Explosion Particles ---
    private static class Particle {
        float x, y, vx, vy;
        float life; // 0 to 1
        int color;
        float size;
        int type; // 0=circle, 1=spark(line), 2=debris(square), 3=sparkle(diamond)
        float rotation;
        float rotSpeed;
        float gravity;

        Particle(float x, float y, int color, float size) {
            this(x, y, color, size, 0);
        }

        Particle(float x, float y, int color, float size, int type) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            this.type = type;
            this.life = 1.0f;
            this.rotation = random.nextFloat() * 360f;
            this.rotSpeed = (random.nextFloat() - 0.5f) * 400f;
            this.gravity = 0;
            float angle = random.nextFloat() * 360f;
            float speed;
            switch (type) {
                case 1: // Spark: fast, narrow spread
                    speed = 200f + random.nextFloat() * 500f;
                    this.gravity = 150f;
                    break;
                case 2: // Debris: medium speed, heavy gravity
                    speed = 80f + random.nextFloat() * 200f;
                    this.gravity = 300f;
                    break;
                case 3: // Sparkle: slow, floaty
                    speed = 30f + random.nextFloat() * 100f;
                    this.gravity = -20f; // Float up
                    break;
                default: // Circle: standard
                    speed = 100f + random.nextFloat() * 300f;
                    break;
            }
            this.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            this.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
        }

        void update(long fps) {
            if (fps <= 0) return;
            x += vx / fps;
            y += vy / fps;
            vy += gravity / fps;
            vx *= 0.96f;
            vy *= 0.96f;
            rotation += rotSpeed / fps;
            float decayRate = (type == 3) ? 1.0f : (type == 1 ? 3.0f : 2.0f);
            life -= decayRate / fps;
        }

        void draw(Canvas canvas, Paint paint) {
            int alpha = (int) (255 * Math.max(0, life));
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            paint.setColor(Color.argb(alpha, r, g, b));

            switch (type) {
                case 1: // Spark: draw as a line/streak
                    float len = size * 3 * life;
                    float dx = vx * 0.01f;
                    float dy = vy * 0.01f;
                    paint.setStrokeWidth(Math.max(1, size * 0.5f * life));
                    canvas.drawLine(x, y, x - dx * len, y - dy * len, paint);
                    paint.setStrokeWidth(1);
                    // Bright head
                    paint.setColor(Color.argb(alpha, 255, 255, Math.min(255, b + 100)));
                    canvas.drawCircle(x, y, size * 0.4f * life, paint);
                    break;

                case 2: // Debris: rotating square
                    canvas.save();
                    canvas.rotate(rotation, x, y);
                    float halfSize = size * life;
                    canvas.drawRect(x - halfSize, y - halfSize,
                            x + halfSize, y + halfSize, paint);
                    canvas.restore();
                    break;

                case 3: // Sparkle: diamond shape with glow
                    float s = size * (0.5f + 0.5f * (float) Math.sin(life * 8));
                    canvas.save();
                    canvas.rotate(rotation, x, y);
                    // Glow
                    paint.setColor(Color.argb(alpha / 3, r, g, b));
                    canvas.drawCircle(x, y, s * 2, paint);
                    // Diamond
                    paint.setColor(Color.argb(alpha, r, g, b));
                    float[] pts = {x, y - s, x + s, y, x, y + s, x - s, y, x, y - s};
                    for (int i = 0; i < pts.length - 2; i += 2) {
                        canvas.drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], paint);
                    }
                    canvas.restore();
                    break;

                default: // Circle with glow
                    // Outer glow
                    paint.setColor(Color.argb(alpha / 3, r, g, b));
                    canvas.drawCircle(x, y, size * life * 2, paint);
                    // Core
                    paint.setColor(Color.argb(alpha, r, g, b));
                    canvas.drawCircle(x, y, size * life, paint);
                    break;
            }
        }

        boolean isDead() {
            return life <= 0;
        }
    }

    // --- Score Popup Text ---
    private static class ScorePopup {
        float x, y;
        String text;
        int color;
        float life;
        float textSize;

        ScorePopup(float x, float y, String text, int color, float textSize) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.life = 1.0f;
            this.textSize = textSize;
        }

        void update(long fps) {
            if (fps <= 0) return;
            y -= 80f / fps; // Float upward
            life -= 1.2f / fps; // Lasts ~0.8 seconds
        }

        void draw(Canvas canvas, Paint paint) {
            int alpha = (int) (255 * Math.max(0, life));
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            paint.setColor(Color.argb(alpha, r, g, b));
            paint.setTextSize(textSize * (1.0f + (1.0f - life) * 0.3f));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, x, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        boolean isDead() {
            return life <= 0;
        }
    }

    // --- Notification Banner ---
    private static class Banner {
        String text;
        int color;
        float life;
        float screenX, screenY;
        float textSize;

        Banner(String text, int color, float screenX, float screenY, float textSize) {
            this.text = text;
            this.color = color;
            this.screenX = screenX;
            this.screenY = screenY;
            this.life = 1.0f;
            this.textSize = textSize;
        }

        void update(long fps) {
            if (fps <= 0) return;
            life -= 0.4f / fps; // Lasts ~2.5 seconds
        }

        void draw(Canvas canvas, Paint paint) {
            float alpha = Math.max(0, life);
            float scale = Math.min(1.0f, life * 3); // Quick fade in

            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            // Background bar
            paint.setColor(Color.argb((int) (120 * alpha), 0, 0, 0));
            float barY = screenY * 0.35f;
            canvas.drawRect(0, barY - textSize, screenX, barY + textSize * 0.5f, paint);

            // Text
            paint.setColor(Color.argb((int) (255 * alpha), r, g, b));
            paint.setTextSize(textSize * scale);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, screenX / 2, barY, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        boolean isDead() {
            return life <= 0;
        }
    }

    // Collections
    private List<Particle> particles = new ArrayList<>();
    private List<ScorePopup> popups = new ArrayList<>();
    private List<Banner> banners = new ArrayList<>();

    public void addExplosion(float x, float y, int color, int count) {
        // Mix of circles and sparks for richer look
        for (int i = 0; i < count; i++) {
            float size = 3f + random.nextFloat() * 5f;
            int r = Math.min(255, Color.red(color) + random.nextInt(40) - 20);
            int g = Math.min(255, Color.green(color) + random.nextInt(40) - 20);
            int b = Math.min(255, Color.blue(color) + random.nextInt(40) - 20);
            int variedColor = Color.rgb(Math.max(0, r), Math.max(0, g), Math.max(0, b));
            int type = (i % 3 == 0) ? 1 : 0; // Every 3rd particle is a spark
            particles.add(new Particle(x, y, variedColor, size, type));
        }
    }

    public void addBigExplosion(float x, float y) {
        // Core flash particles
        addExplosion(x, y, Color.rgb(255, 255, 200), 8);
        // Fire particles
        addExplosion(x, y, Color.rgb(255, 200, 50), 20);
        addExplosion(x, y, Color.rgb(255, 100, 0), 15);
        // Smoke debris
        for (int i = 0; i < 8; i++) {
            float size = 4f + random.nextFloat() * 6f;
            particles.add(new Particle(x, y, Color.rgb(100, 80, 60), size, 2));
        }
        // Sparks
        for (int i = 0; i < 12; i++) {
            float size = 2f + random.nextFloat() * 3f;
            particles.add(new Particle(x, y, Color.rgb(255, 255, 100), size, 1));
        }
    }

    /** Sparkle burst for power-up collection */
    public void addSparkleBurst(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float size = 3f + random.nextFloat() * 5f;
            int r = Math.min(255, Color.red(color) + random.nextInt(60) - 30);
            int g = Math.min(255, Color.green(color) + random.nextInt(60) - 30);
            int b = Math.min(255, Color.blue(color) + random.nextInt(60) - 30);
            int variedColor = Color.rgb(Math.max(0, r), Math.max(0, g), Math.max(0, b));
            particles.add(new Particle(x, y, variedColor, size, 3));
        }
    }

    /** Debris explosion (for shielded enemy shield break, etc.) */
    public void addDebris(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float size = 2f + random.nextFloat() * 4f;
            particles.add(new Particle(x, y, color, size, 2));
        }
    }

    /** Directional spark shower (for bullet impacts) */
    public void addImpactSparks(float x, float y, int color, int count, float directionDeg) {
        for (int i = 0; i < count; i++) {
            float size = 1.5f + random.nextFloat() * 2.5f;
            Particle p = new Particle(x, y, color, size, 1);
            // Override velocity to spray in a cone around direction
            float spread = 40f; // degrees
            float angle = directionDeg + (random.nextFloat() - 0.5f) * spread;
            float speed = 150f + random.nextFloat() * 350f;
            p.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            p.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
            particles.add(p);
        }
    }

    public void addScorePopup(float x, float y, String text, int color, float textSize) {
        popups.add(new ScorePopup(x, y, text, color, textSize));
    }

    public void addBanner(String text, int color, float screenX, float screenY, float textSize) {
        banners.add(new Banner(text, color, screenX, screenY, textSize));
    }

    public void update(long fps) {
        Iterator<Particle> pi = particles.iterator();
        while (pi.hasNext()) {
            Particle p = pi.next();
            p.update(fps);
            if (p.isDead()) pi.remove();
        }

        Iterator<ScorePopup> si = popups.iterator();
        while (si.hasNext()) {
            ScorePopup s = si.next();
            s.update(fps);
            if (s.isDead()) si.remove();
        }

        Iterator<Banner> bi = banners.iterator();
        while (bi.hasNext()) {
            Banner b = bi.next();
            b.update(fps);
            if (b.isDead()) bi.remove();
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }
        for (ScorePopup s : popups) {
            s.draw(canvas, paint);
        }
        for (Banner b : banners) {
            b.draw(canvas, paint);
        }
    }

    public void clear() {
        particles.clear();
        popups.clear();
        banners.clear();
    }

    public boolean hasActiveBanners() {
        return !banners.isEmpty();
    }
}
