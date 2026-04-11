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

        Particle(float x, float y, int color, float size) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            this.life = 1.0f;
            float angle = random.nextFloat() * 360f;
            float speed = 100f + random.nextFloat() * 300f;
            this.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            this.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
        }

        void update(long fps) {
            if (fps <= 0) return;
            x += vx / fps;
            y += vy / fps;
            vx *= 0.96f;
            vy *= 0.96f;
            life -= 2.0f / fps; // Lasts ~0.5 seconds
        }

        void draw(Canvas canvas, Paint paint) {
            int alpha = (int) (255 * Math.max(0, life));
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            paint.setColor(Color.argb(alpha, r, g, b));
            canvas.drawCircle(x, y, size * life, paint);
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
        for (int i = 0; i < count; i++) {
            float size = 3f + random.nextFloat() * 5f;
            // Vary color slightly
            int r = Math.min(255, Color.red(color) + random.nextInt(40) - 20);
            int g = Math.min(255, Color.green(color) + random.nextInt(40) - 20);
            int b = Math.min(255, Color.blue(color) + random.nextInt(40) - 20);
            int variedColor = Color.rgb(Math.max(0, r), Math.max(0, g), Math.max(0, b));
            particles.add(new Particle(x, y, variedColor, size));
        }
    }

    public void addBigExplosion(float x, float y) {
        addExplosion(x, y, Color.rgb(255, 200, 50), 20);
        addExplosion(x, y, Color.rgb(255, 100, 0), 15);
        addExplosion(x, y, Color.rgb(255, 50, 50), 10);
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
