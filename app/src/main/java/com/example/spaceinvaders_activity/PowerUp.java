package com.example.spaceinvaders_activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
        return new PowerUp(randomType(), x, y, screenSize);
    }

    /** Pick a random power-up type id. Pure logic (no Android dependency), unit-testable. */
    static int randomType() {
        return random.nextInt(5);
    }

    public static boolean shouldDrop() {
        return random.nextFloat() < DROP_CHANCE;
    }

    public void update(long fps) {
        if (!active || fps <= 0) return;
        y += FALL_SPEED / fps;
        pulsePhase += 5f / fps;
        if (pulsePhase > 2 * Math.PI) pulsePhase -= 2 * (float) Math.PI;
        updateRect();
    }

    private void updateRect() {
        float halfSize = size / 2;
        rect.set(x - halfSize, y - halfSize, x + halfSize, y + halfSize);
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;

        paint.setAntiAlias(true);
        float pulse = 0.85f + 0.15f * (float) Math.sin(pulsePhase);
        float r = size / 2f;
        int col = getColor();

        // Halo exterior suave (dos capas)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(col);
        paint.setAlpha(45);
        canvas.drawCircle(x, y, r * 1.8f * pulse, paint);
        paint.setAlpha(95);
        canvas.drawCircle(x, y, r * 1.25f, paint);

        // Cuerpo principal
        paint.setAlpha(255);
        canvas.drawCircle(x, y, r, paint);

        // Brillo especular (aspecto de orbe de cristal)
        paint.setColor(Color.WHITE);
        paint.setAlpha(80);
        canvas.drawCircle(x - r * 0.28f, y - r * 0.3f, r * 0.45f, paint);

        // Borde
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, size * 0.07f));
        paint.setColor(Color.WHITE);
        paint.setAlpha(230);
        canvas.drawCircle(x, y, r, paint);

        // Icono según el tipo
        drawIcon(canvas, paint, r);

        // Restaura el estado del pincel para los siguientes dibujos
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
    }

    // Dibuja un icono geométrico nítido en blanco según el tipo de power-up.
    private void drawIcon(Canvas canvas, Paint paint, float r) {
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        float stroke = Math.max(2.5f, r * 0.16f);

        switch (type) {
            case HEALTH: { // Cruz
                paint.setStyle(Paint.Style.FILL);
                float t = r * 0.18f; // grosor (mitad)
                float a = r * 0.5f;  // longitud del brazo (mitad)
                canvas.drawRect(x - t, y - a, x + t, y + a, paint);
                canvas.drawRect(x - a, y - t, x + a, y + t, paint);
                break;
            }
            case RAPID_FIRE: { // Doble galón apuntando hacia arriba (velocidad)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                float w = r * 0.5f;
                for (int i = 0; i < 2; i++) {
                    float oy = i * r * 0.42f - r * 0.05f;
                    canvas.drawLine(x - w, y + oy, x, y + oy - r * 0.42f, paint);
                    canvas.drawLine(x, y + oy - r * 0.42f, x + w, y + oy, paint);
                }
                break;
            }
            case SHIELD_BUBBLE: { // Anillo
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                canvas.drawCircle(x, y, r * 0.48f, paint);
                break;
            }
            case SCORE_BOOST: { // Estrella de 5 puntas
                paint.setStyle(Paint.Style.FILL);
                Path star = new Path();
                float outer = r * 0.6f;
                float inner = r * 0.26f;
                for (int i = 0; i < 10; i++) {
                    float rad = (i % 2 == 0) ? outer : inner;
                    double ang = Math.PI / 2 + i * Math.PI / 5;
                    float px = x + (float) Math.cos(ang) * rad;
                    float py = y - (float) Math.sin(ang) * rad;
                    if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
                }
                star.close();
                canvas.drawPath(star, paint);
                break;
            }
            case FREEZE: { // Copo de nieve (tres ejes)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                float a = r * 0.55f;
                for (int i = 0; i < 3; i++) {
                    double ang = i * Math.PI / 3;
                    float dx = (float) Math.cos(ang) * a;
                    float dy = (float) Math.sin(ang) * a;
                    canvas.drawLine(x - dx, y - dy, x + dx, y + dy, paint);
                }
                break;
            }
            default: {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, y, r * 0.3f, paint);
                break;
            }
        }
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
