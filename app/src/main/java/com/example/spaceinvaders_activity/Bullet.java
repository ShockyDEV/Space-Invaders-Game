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
        if (isUltimate) return 5;
        if (isBigLaser) return 2;
        return 1;
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
        if (heading == UP) {
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

    // Enhanced drawing with glow effects
    public void draw(Canvas canvas, Paint paint, boolean isPlayerBullet) {
        if (isUltimate) {
            // Ultimate beam with pulsing glow
            float pulse = 0.7f + 0.3f * (float) Math.sin(glowPhase);
            paint.setColor(Color.argb((int) (60 * pulse), 255, 100, 255));
            canvas.drawRect(x - 15, y, x + width + 15, y + height, paint);
            paint.setColor(Color.argb((int) (120 * pulse), 255, 50, 255));
            canvas.drawRect(x - 5, y, x + width + 5, y + height, paint);
            paint.setColor(Color.argb(255, 255, 200, 255));
            canvas.drawRect(rect, paint);
        } else if (isBigLaser) {
            // Big laser with orange glow
            float pulse = 0.8f + 0.2f * (float) Math.sin(glowPhase);
            paint.setColor(Color.argb((int) (80 * pulse), 255, 150, 0));
            canvas.drawRect(x - 8, y, x + width + 8, y + height, paint);
            paint.setColor(color);
            canvas.drawRect(rect, paint);
        } else if (isPiercing) {
            // Piercing bullet with teal trail
            paint.setColor(Color.argb(60, 0, 255, 200));
            canvas.drawRect(x - 3, y, x + width + 3, y + height * 1.3f, paint);
            paint.setColor(color);
            canvas.drawRect(rect, paint);
        } else {
            // Standard bullet
            if (isPlayerBullet) {
                paint.setColor(Color.GREEN);
            } else {
                paint.setColor(Color.RED);
            }
            canvas.drawRect(rect, paint);
        }
    }
}
