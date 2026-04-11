package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Bullet {

    private float x;
    private float y;

    private RectF rect;

    public static final int UP = 0;
    public static final int DOWN = 1;

    int heading = -1;
    float speed = 400;

    private int width = 10;
    private int height;

    // Bullet variants
    private boolean isBigLaser = false;
    private boolean isPiercing = false;
    private boolean isUltimate = false;
    private boolean isHoming = false;
    private boolean isCritical = false;

    // Homing target
    private float targetX = -1;
    private float targetY = -1;

    // Visual
    private float glowPhase = 0;
    private int color = Color.GREEN;

    public Bullet(int screenY) {
        height = screenY / 20;
        rect = new RectF();
    }

    // Create a big laser bullet
    public static Bullet createBigLaser(int screenY) {
        Bullet b = new Bullet(screenY);
        b.isBigLaser = true;
        b.width = 30;
        b.height = screenY / 12;
        b.color = Color.rgb(255, 100, 0);
        return b;
    }

    // Create a piercing bullet
    public static Bullet createPiercing(int screenY) {
        Bullet b = new Bullet(screenY);
        b.isPiercing = true;
        b.color = Color.rgb(0, 255, 200);
        b.speed = 500;
        return b;
    }

    // Create a homing missile bullet
    public static Bullet createHoming(int screenY) {
        Bullet b = new Bullet(screenY);
        b.color = Color.rgb(255, 200, 0);
        b.width = 14;
        b.speed = 350;
        b.isHoming = true;
        return b;
    }

    // Create a critical hit bullet (3x damage)
    public static Bullet createCritical(int screenY, boolean isBig, boolean isPierc) {
        Bullet b;
        if (isBig) {
            b = createBigLaser(screenY);
        } else if (isPierc) {
            b = createPiercing(screenY);
        } else {
            b = new Bullet(screenY);
        }
        b.isCritical = true;
        b.color = Color.rgb(255, 50, 50);
        return b;
    }

    // Create an ultimate laser beam
    public static Bullet createUltimate(int screenY) {
        Bullet b = new Bullet(screenY);
        b.isUltimate = true;
        b.isPiercing = true;
        b.width = 50;
        b.height = screenY; // Full screen height beam
        b.color = Color.rgb(255, 0, 255);
        b.speed = 600;
        return b;
    }

    public RectF getRect() { return rect; }

    public boolean isBigLaser() { return isBigLaser; }
    public boolean isPiercing() { return isPiercing; }
    public boolean isUltimate() { return isUltimate; }

    public int getDamage() {
        int base;
        if (isUltimate) base = 5;
        else if (isBigLaser) base = 2;
        else base = 1;
        return isCritical ? base * 3 : base;
    }

    public boolean isHoming() { return isHoming; }
    public boolean isCritical() { return isCritical; }

    public void setHomingTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public void shoot(float startX, float startY, int direction) {
        x = startX - width / 2f; // Center the bullet
        y = startY;
        heading = direction;
    }

    public float getImpactPointY() {
        if (heading == DOWN) {
            return y + height;
        } else {
            return y;
        }
    }

    public void update(long fps) {
        if (fps <= 0) return;

        if (isHoming && targetX >= 0 && targetY >= 0) {
            // Steer toward target
            float dx = targetX - (x + width / 2f);
            float dy = targetY - (y + height / 2f);
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 1) {
                float steerStrength = 5f; // pixels per frame toward target
                x += (dx / dist) * steerStrength;
                y -= speed / fps; // still moves upward
            }
        } else if (heading == UP) {
            y -= speed / fps;
        } else {
            y += speed / fps;
        }

        glowPhase += 8f / fps;
        if (glowPhase > 2 * Math.PI) glowPhase -= 2 * (float) Math.PI;

        rect.left = x;
        rect.right = x + width;
        rect.top = y;
        rect.bottom = y + height;
    }

    public boolean isOffScreen(int screenY) {
        return (heading == UP && y + height < 0) || (heading == DOWN && y > screenY);
    }

    public int getColor() { return color; }
    public float getCenterX() { return x + width / 2f; }
    public float getCenterY() { return y + height / 2f; }
    public int getWidth() { return width; }

    // Enhanced drawing with glow effects
    public void draw(Canvas canvas, Paint paint, boolean isPlayerBullet) {
        float pulse = 0.7f + 0.3f * (float) Math.sin(glowPhase);
        float cx = x + width / 2f;

        if (isUltimate) {
            // Ultimate beam: multi-layer pulsing beam with side flares
            // Outermost glow
            paint.setColor(Color.argb((int) (30 * pulse), 200, 50, 255));
            canvas.drawRect(x - 25, y, x + width + 25, y + height, paint);
            // Mid glow
            paint.setColor(Color.argb((int) (60 * pulse), 255, 100, 255));
            canvas.drawRect(x - 15, y, x + width + 15, y + height, paint);
            // Inner glow
            paint.setColor(Color.argb((int) (120 * pulse), 255, 50, 255));
            canvas.drawRect(x - 5, y, x + width + 5, y + height, paint);
            // Core (white-pink)
            paint.setColor(Color.argb(255, 255, 200, 255));
            canvas.drawRect(rect, paint);
            // Energy flares along the beam
            for (int i = 0; i < 5; i++) {
                float flareY = y + (i * height / 5f) + (glowPhase * 30) % (height / 5f);
                float flareSize = 8 + 6 * (float) Math.sin(glowPhase + i * 1.5f);
                paint.setColor(Color.argb((int) (100 * pulse), 255, 180, 255));
                canvas.drawCircle(cx, flareY, flareSize, paint);
            }

        } else if (isBigLaser) {
            // Big laser with layered orange glow
            paint.setColor(Color.argb((int) (40 * pulse), 255, 200, 0));
            canvas.drawRect(x - 12, y - 5, x + width + 12, y + height + 5, paint);
            paint.setColor(Color.argb((int) (80 * pulse), 255, 150, 0));
            canvas.drawRect(x - 6, y, x + width + 6, y + height, paint);
            // Core
            paint.setColor(color);
            canvas.drawRect(rect, paint);
            // Hot center line
            paint.setColor(Color.argb(200, 255, 255, 150));
            canvas.drawRect(cx - 2, y, cx + 2, y + height, paint);

        } else if (isHoming) {
            // Homing missile: gold/orange glow with trail
            float trailLen = height * 0.8f;
            paint.setColor(Color.argb(25, 255, 200, 0));
            canvas.drawRect(x - 4, y, x + width + 4, y + height + trailLen, paint);
            paint.setColor(Color.argb(60, 255, 180, 0));
            canvas.drawRect(x - 2, y, x + width + 2, y + height + trailLen * 0.4f, paint);
            // Core
            paint.setColor(Color.rgb(255, 200, 50));
            canvas.drawRect(rect, paint);
            // Bright tip
            paint.setColor(Color.argb(230, 255, 255, 200));
            canvas.drawCircle(cx, y, width * 0.5f, paint);

        } else if (isPiercing) {
            // Piercing: teal energy bolt with trailing glow
            float trailLen = height * 0.6f;
            paint.setColor(Color.argb(30, 0, 255, 200));
            canvas.drawRect(x - 5, y, x + width + 5, y + height + trailLen, paint);
            paint.setColor(Color.argb(70, 0, 255, 200));
            canvas.drawRect(x - 2, y, x + width + 2, y + height + trailLen * 0.5f, paint);
            // Core
            paint.setColor(color);
            canvas.drawRect(rect, paint);
            // Bright tip
            paint.setColor(Color.argb(220, 200, 255, 255));
            canvas.drawCircle(cx, y, width * 0.6f, paint);

        } else {
            // Standard bullet with subtle glow
            if (isPlayerBullet) {
                // Green glow
                paint.setColor(Color.argb((int) (40 * pulse), 50, 255, 50));
                canvas.drawRect(x - 3, y - 3, x + width + 3, y + height + 3, paint);
                paint.setColor(Color.argb(220, 100, 255, 100));
                canvas.drawRect(rect, paint);
                // Bright core
                paint.setColor(Color.argb(180, 200, 255, 200));
                canvas.drawRect(cx - 1, y, cx + 1, y + height, paint);
            } else {
                // Red enemy bullet with glow
                paint.setColor(Color.argb((int) (50 * pulse), 255, 50, 50));
                canvas.drawRect(x - 2, y - 2, x + width + 2, y + height + 2, paint);
                paint.setColor(Color.argb(230, 255, 80, 60));
                canvas.drawRect(rect, paint);
            }
        }
    }
}
