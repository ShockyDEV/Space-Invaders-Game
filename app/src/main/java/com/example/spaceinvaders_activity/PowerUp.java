package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Random;

/**
 * Power-ups that drop from destroyed enemies.
 * Player collects them by touching with their ship.
 */
public class PowerUp {

    // Power-up types
    public static final int HEALTH = 0;
    public static final int RAPID_FIRE = 1;
    public static final int SHIELD_BUBBLE = 2;
    public static final int SCORE_BOOST = 3;
    public static final int FREEZE = 4;

    public static final float DROP_CHANCE = 0.12f; // 12% chance per kill
    public static final long BUFF_DURATION = 8000; // 8 seconds for temp buffs
    public static final float FALL_SPEED = 150f; // pixels per second

    private static final Random random = new Random();

    private int type;
    private float x, y;
    private float size;
    private RectF rect;
    private boolean active;
    private float pulsePhase = 0;

    public PowerUp(int type, float x, float y, float screenSize) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.size = screenSize / 30f;
        this.rect = new RectF();
        this.active = true;
        updateRect();
    }

    public static PowerUp createRandom(float x, float y, float screenSize) {
        int type = random.nextInt(5);
        return new PowerUp(type, x, y, screenSize);
    }

    public static boolean shouldDrop() {
        return random.nextFloat() < DROP_CHANCE;
    }

    /** Lucky Drops skill doubles the drop chance */
    public static boolean shouldDrop(boolean luckyDrops) {
        float chance = luckyDrops ? DROP_CHANCE * 2 : DROP_CHANCE;
        return random.nextFloat() < chance;
    }

    public void update(long fps) {
        if (!active || fps <= 0) return;
        y += FALL_SPEED / fps;
        pulsePhase += 5f / fps;
        if (pulsePhase > 2 * Math.PI) pulsePhase -= 2 * (float) Math.PI;
        updateRect();
    }

    /** Magnet Pull: drift toward the given position */
    public void attractToward(float targetX, float targetY, long fps) {
        if (fps <= 0) return;
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 5) {
            float pullSpeed = 120f; // pixels/second
            x += (dx / dist) * pullSpeed / fps;
            y += (dy / dist) * pullSpeed / fps;
            updateRect();
        }
    }

    private void updateRect() {
        float halfSize = size / 2;
        rect.set(x - halfSize, y - halfSize, x + halfSize, y + halfSize);
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;

        float pulse = 0.7f + 0.3f * (float) Math.sin(pulsePhase);
        int alpha = (int) (255 * pulse);

        // Draw outer glow
        paint.setColor(getColor());
        paint.setAlpha(80);
        float glowSize = size * 0.8f;
        canvas.drawCircle(x, y, glowSize, paint);

        // Draw main body
        paint.setColor(getColor());
        paint.setAlpha(alpha);
        canvas.drawCircle(x, y, size / 2, paint);

        // Draw inner icon (simple shapes to indicate type)
        paint.setColor(Color.WHITE);
        paint.setAlpha(alpha);
        paint.setTextSize(size * 0.6f);
        paint.setTextAlign(Paint.Align.CENTER);
        String icon = getIcon();
        canvas.drawText(icon, x, y + size * 0.2f, paint);
        paint.setTextAlign(Paint.Align.LEFT); // Reset alignment
    }

    public int getColor() {
        switch (type) {
            case HEALTH:       return Color.argb(255, 0, 255, 100);   // Green
            case RAPID_FIRE:   return Color.argb(255, 255, 255, 0);   // Yellow
            case SHIELD_BUBBLE: return Color.argb(255, 0, 150, 255);  // Blue
            case SCORE_BOOST:  return Color.argb(255, 255, 215, 0);   // Gold
            case FREEZE:       return Color.argb(255, 150, 220, 255); // Ice blue
            default:           return Color.WHITE;
        }
    }

    private String getIcon() {
        switch (type) {
            case HEALTH:        return "+";
            case RAPID_FIRE:    return "R";
            case SHIELD_BUBBLE: return "S";
            case SCORE_BOOST:   return "$";
            case FREEZE:        return "*";
            default:            return "?";
        }
    }

    public String getName() {
        switch (type) {
            case HEALTH:        return "EXTRA LIFE!";
            case RAPID_FIRE:    return "RAPID FIRE!";
            case SHIELD_BUBBLE: return "SHIELD!";
            case SCORE_BOOST:   return "2x SCORE!";
            case FREEZE:        return "FREEZE!";
            default:            return "POWER UP!";
        }
    }

    public int getType() { return type; }
    public float getX() { return x; }
    public float getY() { return y; }
    public RectF getRect() { return rect; }
    public boolean isActive() { return active; }

    public void collect() { active = false; }

    public boolean isOffScreen(int screenY) {
        return y > screenY + size;
    }
}
