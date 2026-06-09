package com.example.spaceinvaders_activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Random;

public class Invader {

    RectF rect;

    Random generator = new Random();

    private Bitmap bitmap1;
    private Bitmap bitmap2;

    private float length;
    private float height;
    private float x;
    private float y;
    private float shipSpeed;

    public final int LEFT = 1;
    public final int RIGHT = 2;

    private int shipMoving = RIGHT;

    boolean isVisible;

    private float baseSpeed;
    private int shotChance;

    // Boss properties
    private boolean isBoss = false;
    private int health = 1;
    private int maxHealth = 1;
    private boolean isHit = false;
    private long hitFlashTime = 0;
    private static final long HIT_FLASH_DURATION = 150;

    // Enemy type (affects appearance and behavior)
    private int enemyType = 0; // 0=normal, 1=fast, 2=tank
    private int pointValue = 10;

    // Freeze state
    private boolean frozen = false;
    private long freezeEndTime = 0;

    // Animation
    private boolean useAltBitmap = false;
    private long lastAnimSwitch = 0;
    private static final long ANIM_INTERVAL = 500;

    public Invader(Context context, int row, int column, int screenX, int screenY,
                   float baseSpeed, int shotChance) {
        rect = new RectF();

        length = screenX / 20f;
        height = screenY / 20f;

        isVisible = true;

        int padding = screenX / 25;

        x = column * (length + padding);
        y = row * (length + padding / 4f) + screenY * 0.08f; // Offset from top for HUD

        bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader1);
        bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader2);

        bitmap1 = Bitmap.createScaledBitmap(bitmap1, (int) length, (int) height, false);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, (int) length, (int) height, false);

        this.baseSpeed = baseSpeed;
        this.shotChance = shotChance;
        shipSpeed = baseSpeed;

        // Assign enemy type based on row
        if (row == 0) {
            enemyType = 1; // Front row = fast scouts
            pointValue = 15;
        } else if (row >= 4) {
            enemyType = 2; // Back rows = tanks
            pointValue = 20;
        } else {
            enemyType = 0;
            pointValue = 10;
        }
    }

    // Boss constructor
    public Invader(Context context, int screenX, int screenY, float bossSpeed, int bossHealth) {
        rect = new RectF();

        length = screenX / 6f;  // Boss is much bigger
        height = screenY / 10f;

        isVisible = true;
        isBoss = true;
        health = bossHealth;
        maxHealth = bossHealth;
        pointValue = 100;

        x = screenX / 2f - length / 2f;
        y = screenY * 0.05f;

        bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader1);
        bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader2);

        bitmap1 = Bitmap.createScaledBitmap(bitmap1, (int) length, (int) height, false);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, (int) length, (int) height, false);

        this.baseSpeed = bossSpeed;
        this.shotChance = 80; // Boss shoots frequently
        shipSpeed = bossSpeed;
        enemyType = 3; // Boss type
    }

    // --- Boss methods ---

    public boolean isBoss() { return isBoss; }

    public int getHealth() { return health; }

    public int getMaxHealth() { return maxHealth; }

    public boolean takeDamage(int damage) {
        health -= damage;
        isHit = true;
        hitFlashTime = System.currentTimeMillis();
        if (health <= 0) {
            health = 0;
            isVisible = false;
            return true; // Boss defeated
        }
        return false;
    }

    public int getPointValue() { return pointValue; }

    public int getEnemyType() { return enemyType; }

    // --- Freeze ---

    public void freeze(long duration) {
        frozen = true;
        freezeEndTime = System.currentTimeMillis() + duration;
    }

    public boolean isFrozen() {
        if (frozen && System.currentTimeMillis() > freezeEndTime) {
            frozen = false;
        }
        return frozen;
    }

    // --- Standard methods ---

    public void setInvisible() { isVisible = false; }

    public boolean getVisibility() { return isVisible; }

    public RectF getRect() { return rect; }

    public Bitmap getBitmap() {
        return useAltBitmap ? bitmap2 : bitmap1;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getLength() { return length; }
    public float getHeight() { return height; }

    public void update(long fps) {
        if (fps <= 0) return; // Avoid divide-by-zero before the first frame is timed
        if (isFrozen()) return; // Don't move when frozen

        if (shipMoving == LEFT) {
            x = x - shipSpeed / fps;
        }
        if (shipMoving == RIGHT) {
            x = x + shipSpeed / fps;
        }

        // Animate sprite switching
        long now = System.currentTimeMillis();
        if (now - lastAnimSwitch > ANIM_INTERVAL) {
            useAltBitmap = !useAltBitmap;
            lastAnimSwitch = now;
        }

        // Update hit flash
        if (isHit && now - hitFlashTime > HIT_FLASH_DURATION) {
            isHit = false;
        }

        rect.top = y;
        rect.bottom = y + height;
        rect.left = x;
        rect.right = x + length;
    }

    public void dropDownAndReverse() {
        if (isFrozen()) return;

        if (shipMoving == LEFT) {
            shipMoving = RIGHT;
        } else {
            shipMoving = LEFT;
        }

        y = y + height;
        shipSpeed = shipSpeed * 1.12f;
    }

    public boolean takeAim(float playerShipX, float playerShipLength) {
        if (isFrozen()) return false;

        int randomNumber;

        if ((playerShipX + playerShipLength > x && playerShipX + playerShipLength < x + length)
                || (playerShipX > x && playerShipX < x + length)) {
            randomNumber = generator.nextInt(Math.max(1, shotChance));
            if (randomNumber == 0) {
                return true;
            }
        }

        randomNumber = generator.nextInt(Math.max(1, shotChance * 2));
        return randomNumber == 0;
    }

    // Draw with effects (hit flash, boss health bar, freeze tint)
    public void drawEffects(Canvas canvas, Paint paint) {
        if (!isVisible) return;

        // Hit flash overlay
        if (isHit) {
            paint.setColor(Color.argb(150, 255, 255, 255));
            canvas.drawRect(rect, paint);
        }

        // Freeze tint
        if (isFrozen()) {
            paint.setColor(Color.argb(100, 150, 220, 255));
            canvas.drawRect(rect, paint);
        }

        // Boss health bar
        if (isBoss && isVisible) {
            float barWidth = length * 1.2f;
            float barHeight = 8;
            float barX = x + length / 2 - barWidth / 2;
            float barY = y + height + 5;

            // Background
            paint.setColor(Color.argb(180, 50, 50, 50));
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint);

            // Health fill
            float healthPercent = (float) health / maxHealth;
            int r = (int) (255 * (1 - healthPercent));
            int g = (int) (255 * healthPercent);
            paint.setColor(Color.rgb(r, g, 0));
            canvas.drawRect(barX, barY, barX + barWidth * healthPercent, barY + barHeight, paint);

            // Boss label
            paint.setColor(Color.RED);
            paint.setTextSize(30);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("BOSS", x + length / 2, y - 10, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        // Enemy type indicator (colored dot)
        if (!isBoss) {
            switch (enemyType) {
                case 1: // Fast
                    paint.setColor(Color.argb(150, 255, 255, 0));
                    canvas.drawCircle(x + length / 2, y - 3, 3, paint);
                    break;
                case 2: // Tank
                    paint.setColor(Color.argb(150, 255, 100, 0));
                    canvas.drawCircle(x + length / 2, y - 3, 3, paint);
                    break;
            }
        }
    }

    // Legacy compatibility constructor
    public Invader(Context context, int row, int column, int screenX, int screenY, String difficulty) {
        this(context, row, column, screenX, screenY,
                difficulty.equals("Dificil") ? 200f : 100f,
                difficulty.equals("Dificil") ? 250 : 1000);
    }

    public void setDifficulty(String difficulty) {
        if (difficulty.equals("Dificil")) {
            baseSpeed = 200;
            shotChance = 250;
        } else {
            baseSpeed = 100;
            shotChance = 1000;
        }
        shipSpeed = baseSpeed;
    }
}
