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
    // 0=normal, 1=scout, 2=tank, 3=boss, 4=shielded, 5=splitter, 6=kamikaze
    // 7=healer, 8=cloaker, 9=bomber, 10=elite
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_SCOUT = 1;
    public static final int TYPE_TANK = 2;
    public static final int TYPE_BOSS = 3;
    public static final int TYPE_SHIELDED = 4;
    public static final int TYPE_SPLITTER = 5;
    public static final int TYPE_KAMIKAZE = 6;
    public static final int TYPE_HEALER = 7;
    public static final int TYPE_CLOAKER = 8;
    public static final int TYPE_BOMBER = 9;
    public static final int TYPE_ELITE = 10;

    private int enemyType = 0;
    private int pointValue = 10;

    // Shielded type: absorbs one hit
    private boolean hasShield = false;

    // Splitter type: spawns children on death
    private boolean isChild = false;

    // Kamikaze type: moves toward player
    private float targetPlayerX = -1;
    private float kamikazeSpeedY = 0;

    // Healer type: periodically heals adjacent invaders
    private long lastHealTime = 0;
    private static final long HEAL_INTERVAL = 3000;

    // Cloaker type: toggles semi-transparency
    private boolean cloaked = false;
    private long cloakToggleTime = 0;
    private static final long CLOAK_VISIBLE_DURATION = 3000;
    private static final long CLOAK_HIDDEN_DURATION = 2000;

    // Bomber type: drops timed bomb on death
    private boolean hasBomb = true;

    // Elite type: unpredictable movement
    private float eliteMoveSeed = 0;

    // Freeze state
    private boolean frozen = false;
    private long freezeEndTime = 0;

    // Speed multiplier (Temporal Shift sets to 0.8)
    private float speedMultiplier = 1.0f;

    // Animation
    private boolean useAltBitmap = false;
    private long lastAnimSwitch = 0;
    private static final long ANIM_INTERVAL = 500;

    public Invader(Context context, int row, int column, int screenX, int screenY,
                   float baseSpeed, int shotChance) {
        this(context, row, column, screenX, screenY, baseSpeed, shotChance, -1);
    }

    /**
     * Creates an invader with a specific enemy type.
     * @param explicitType the enemy type index (0-6), or -1 to assign by row
     */
    public Invader(Context context, int row, int column, int screenX, int screenY,
                   float baseSpeed, int shotChance, int explicitType) {
        rect = new RectF();

        length = screenX / 20f;
        height = screenY / 20f;

        isVisible = true;

        int padding = screenX / 25;

        x = column * (length + padding);
        y = row * (length + padding / 4f) + screenY * 0.08f;

        bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader1);
        bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader2);

        bitmap1 = Bitmap.createScaledBitmap(bitmap1, (int) length, (int) height, false);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, (int) length, (int) height, false);

        this.baseSpeed = baseSpeed;
        this.shotChance = shotChance;
        shipSpeed = baseSpeed;

        // Assign enemy type
        if (explicitType >= 0) {
            enemyType = explicitType;
        } else {
            // Legacy fallback: assign by row
            if (row == 0) {
                enemyType = TYPE_SCOUT;
            } else if (row >= 4) {
                enemyType = TYPE_TANK;
            } else {
                enemyType = TYPE_NORMAL;
            }
        }

        // Apply type-specific properties
        applyTypeProperties(screenX, screenY);
    }

    /** Configure properties based on enemyType after assignment */
    private void applyTypeProperties(int screenX, int screenY) {
        switch (enemyType) {
            case TYPE_NORMAL:
                pointValue = 10;
                break;
            case TYPE_SCOUT:
                pointValue = 15;
                shipSpeed = baseSpeed * 1.4f;
                break;
            case TYPE_TANK:
                pointValue = 20;
                health = 2;
                maxHealth = 2;
                shipSpeed = baseSpeed * 0.7f;
                break;
            case TYPE_SHIELDED:
                pointValue = 25;
                hasShield = true;
                break;
            case TYPE_SPLITTER:
                pointValue = 15;
                break;
            case TYPE_KAMIKAZE:
                pointValue = 20;
                shipSpeed = baseSpeed * 1.2f;
                kamikazeSpeedY = baseSpeed * 0.5f;
                break;
            case TYPE_HEALER:
                pointValue = 25;
                lastHealTime = System.currentTimeMillis();
                break;
            case TYPE_CLOAKER:
                pointValue = 30;
                cloakToggleTime = System.currentTimeMillis() + CLOAK_VISIBLE_DURATION;
                break;
            case TYPE_BOMBER:
                pointValue = 20;
                hasBomb = true;
                break;
            case TYPE_ELITE:
                pointValue = 40;
                health = 2;
                maxHealth = 2;
                shipSpeed = baseSpeed * 1.1f;
                this.shotChance = Math.max(1, this.shotChance / 2); // Fires 2x more
                eliteMoveSeed = (float) (Math.random() * 1000);
                break;
        }
    }

    /**
     * Creates a splitter child invader (half-size, spawned on parent death).
     */
    public static Invader createSplitterChild(Context context, float parentX, float parentY,
                                               int screenX, int screenY, float speed, boolean goRight) {
        Invader child = new Invader();
        child.rect = new RectF();
        child.length = screenX / 40f;  // Half normal size
        child.height = screenY / 40f;
        child.isVisible = true;
        child.isChild = true;
        child.enemyType = TYPE_NORMAL; // Children are basic
        child.pointValue = 8;
        child.health = 1;
        child.maxHealth = 1;

        child.x = parentX + (goRight ? 20 : -20);
        child.y = parentY;
        child.baseSpeed = speed * 1.3f;
        child.shipSpeed = child.baseSpeed;
        child.shotChance = 2000; // Rarely shoots
        child.shipMoving = goRight ? child.RIGHT : child.LEFT;

        child.bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader1);
        child.bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader2);
        child.bitmap1 = Bitmap.createScaledBitmap(child.bitmap1, (int) child.length, (int) child.height, false);
        child.bitmap2 = Bitmap.createScaledBitmap(child.bitmap2, (int) child.length, (int) child.height, false);

        return child;
    }

    /** Private no-arg constructor for factory methods */
    private Invader() {}

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

    // --- Shielded ---

    public boolean hasShield() { return hasShield; }

    /** Absorbs a hit. Returns true if shield broke (still alive), false if no shield. */
    public boolean hitShield() {
        if (hasShield) {
            hasShield = false;
            isHit = true;
            hitFlashTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    // --- Splitter ---

    public boolean isSplitter() { return enemyType == TYPE_SPLITTER && !isChild; }

    public boolean isChildInvader() { return isChild; }

    // --- Kamikaze ---

    public void setTargetPlayerX(float playerX) {
        this.targetPlayerX = playerX;
    }

    public boolean isKamikaze() { return enemyType == TYPE_KAMIKAZE; }

    // --- Healer ---

    /** Returns true if this healer should heal a neighbor this frame */
    public boolean shouldHeal() {
        if (enemyType != TYPE_HEALER || !isVisible) return false;
        long now = System.currentTimeMillis();
        if (now - lastHealTime >= HEAL_INTERVAL) {
            lastHealTime = now;
            return true;
        }
        return false;
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    // --- Cloaker ---

    public boolean isCloaked() { return enemyType == TYPE_CLOAKER && cloaked; }

    // --- Bomber ---

    public boolean isBomber() { return enemyType == TYPE_BOMBER; }
    public boolean hasBombOnDeath() { return enemyType == TYPE_BOMBER && hasBomb; }

    // --- Elite ---

    public boolean isElite() { return enemyType == TYPE_ELITE; }

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
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public float getLength() { return length; }
    public float getHeight() { return height; }

    public void setSpeedMultiplier(float mult) { this.speedMultiplier = mult; }

    public void update(long fps) {
        if (isFrozen()) return; // Don't move when frozen

        float effectiveSpeed = shipSpeed * speedMultiplier;

        // Kamikaze: move diagonally toward player
        if (enemyType == TYPE_KAMIKAZE && targetPlayerX >= 0) {
            float dx = targetPlayerX - (x + length / 2);
            float moveX = Math.signum(dx) * effectiveSpeed * 0.6f / fps;
            x += moveX;
            y += kamikazeSpeedY * speedMultiplier / fps;
        } else {
            if (shipMoving == LEFT) {
                x = x - effectiveSpeed / fps;
            }
            if (shipMoving == RIGHT) {
                x = x + effectiveSpeed / fps;
            }
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

        // Cloaker: toggle cloak
        if (enemyType == TYPE_CLOAKER && now > cloakToggleTime) {
            cloaked = !cloaked;
            cloakToggleTime = now + (cloaked ? CLOAK_HIDDEN_DURATION : CLOAK_VISIBLE_DURATION);
        }

        // Elite: slight sinusoidal offset for unpredictable movement
        if (enemyType == TYPE_ELITE) {
            float wobble = (float) Math.sin(now / 300.0 + eliteMoveSeed) * 2f;
            x += wobble;
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

        // Enemy type visual differentiation (prominent overlays for gameplay readability)
        if (!isBoss) {
            float cx = x + length / 2;
            float cy = y + height / 2;
            float hw = length / 2;
            long now = System.currentTimeMillis();

            switch (enemyType) {
                case TYPE_SCOUT:
                    // Yellow speed streaks behind the scout
                    paint.setColor(Color.argb(120, 255, 255, 0));
                    canvas.drawRect(x - 6, cy - 2, x, cy + 2, paint);
                    canvas.drawRect(x + length, cy - 2, x + length + 6, cy + 2, paint);
                    // Yellow tint
                    paint.setColor(Color.argb(35, 255, 255, 0));
                    canvas.drawRect(rect, paint);
                    break;

                case TYPE_TANK:
                    // Orange armor plating outline
                    paint.setColor(Color.argb(160, 255, 120, 0));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(3);
                    canvas.drawRect(x - 3, y - 3, x + length + 3, y + height + 3, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setStrokeWidth(1);
                    // Orange tint
                    paint.setColor(Color.argb(40, 255, 120, 0));
                    canvas.drawRect(rect, paint);
                    // Corner rivets
                    paint.setColor(Color.argb(200, 255, 150, 0));
                    canvas.drawCircle(x, y, 3, paint);
                    canvas.drawCircle(x + length, y, 3, paint);
                    canvas.drawCircle(x, y + height, 3, paint);
                    canvas.drawCircle(x + length, y + height, 3, paint);
                    break;

                case TYPE_SHIELDED:
                    if (hasShield) {
                        // Blue shield bubble around the invader
                        float shieldPulse = 0.7f + 0.3f * (float) Math.sin(now / 300.0);
                        paint.setColor(Color.argb((int) (80 * shieldPulse), 50, 150, 255));
                        canvas.drawCircle(cx, cy, hw * 1.3f, paint);
                        paint.setColor(Color.argb((int) (150 * shieldPulse), 100, 200, 255));
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2);
                        canvas.drawCircle(cx, cy, hw * 1.3f, paint);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(1);
                    } else {
                        // Broken shield remnants
                        paint.setColor(Color.argb(60, 50, 100, 200));
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(1);
                        canvas.drawArc(x - 3, y - 3, x + length + 3, y + height + 3,
                                -30, 60, false, paint);
                        canvas.drawArc(x - 3, y - 3, x + length + 3, y + height + 3,
                                150, 60, false, paint);
                        paint.setStyle(Paint.Style.FILL);
                    }
                    break;

                case TYPE_SPLITTER:
                    // Green mitosis marks (two halves) indicating it will split
                    paint.setColor(Color.argb(120, 50, 255, 50));
                    canvas.drawLine(cx, y - 2, cx, y + height + 2, paint);
                    // Green glow
                    paint.setColor(Color.argb(30, 50, 255, 50));
                    canvas.drawRect(rect, paint);
                    // Diamond markers on sides
                    paint.setColor(Color.argb(200, 80, 255, 80));
                    float d = 5;
                    canvas.drawLine(x - d, cy, x, cy - d, paint);
                    canvas.drawLine(x, cy - d, x + d, cy, paint);
                    canvas.drawLine(x + length - d, cy, x + length, cy - d, paint);
                    canvas.drawLine(x + length, cy - d, x + length + d, cy, paint);
                    break;

                case TYPE_KAMIKAZE:
                    // Red flame trail (prominent, visible from distance)
                    float flicker = 0.5f + 0.5f * (float) Math.sin(now * 0.02);
                    // Main engine flame
                    paint.setColor(Color.argb((int) (220 * flicker), 255, 50, 0));
                    canvas.drawCircle(cx, y + height + 6, 7 * flicker, paint);
                    paint.setColor(Color.argb((int) (180 * flicker), 255, 150, 0));
                    canvas.drawCircle(cx, y + height + 8, 5 * flicker, paint);
                    paint.setColor(Color.argb((int) (150 * flicker), 255, 255, 100));
                    canvas.drawCircle(cx, y + height + 4, 3 * flicker, paint);
                    // Side flames
                    paint.setColor(Color.argb((int) (120 * flicker), 255, 80, 0));
                    canvas.drawCircle(cx - hw * 0.5f, y + height + 4, 4 * flicker, paint);
                    canvas.drawCircle(cx + hw * 0.5f, y + height + 4, 4 * flicker, paint);
                    // Red tint
                    paint.setColor(Color.argb(30, 255, 0, 0));
                    canvas.drawRect(rect, paint);
                    break;

                case TYPE_HEALER:
                    // Green healing aura (large, pulsing circle)
                    float healPulse = 0.4f + 0.6f * (float) Math.sin(now / 400.0);
                    paint.setColor(Color.argb((int) (50 * healPulse), 50, 255, 50));
                    canvas.drawCircle(cx, cy, length * 0.8f, paint);
                    // Prominent green cross on top
                    paint.setColor(Color.argb(220, 50, 255, 50));
                    canvas.drawRect(cx - 3, y - 8, cx + 3, y - 1, paint);
                    canvas.drawRect(cx - 6, y - 6, cx + 6, y - 3, paint);
                    // Green tint
                    paint.setColor(Color.argb(30, 50, 255, 50));
                    canvas.drawRect(rect, paint);
                    break;

                case TYPE_CLOAKER:
                    if (cloaked) {
                        // Mostly invisible - just a shimmer outline
                        paint.setColor(Color.argb(180, 0, 0, 0));
                        canvas.drawRect(rect, paint);
                        // Shimmer edge
                        float shimmer = 0.3f + 0.7f * (float) Math.sin(now / 150.0);
                        paint.setColor(Color.argb((int) (40 * shimmer), 180, 50, 255));
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2);
                        canvas.drawRect(x - 1, y - 1, x + length + 1, y + height + 1, paint);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(1);
                    } else {
                        // Purple stealth marker + eye symbol
                        paint.setColor(Color.argb(40, 180, 50, 255));
                        canvas.drawRect(rect, paint);
                        paint.setColor(Color.argb(180, 200, 80, 255));
                        // Eye shape
                        canvas.drawCircle(cx, y - 4, 4, paint);
                        paint.setColor(Color.argb(255, 100, 0, 200));
                        canvas.drawCircle(cx, y - 4, 2, paint);
                    }
                    break;

                case TYPE_BOMBER:
                    // Orange bomb below with animated fuse
                    paint.setColor(Color.argb(220, 255, 150, 0));
                    canvas.drawCircle(cx, y + height + 7, 6, paint);
                    paint.setColor(Color.argb(180, 200, 100, 0));
                    canvas.drawCircle(cx, y + height + 7, 4, paint);
                    // Fuse line
                    paint.setColor(Color.argb(200, 150, 100, 50));
                    canvas.drawLine(cx + 3, y + height + 3, cx + 7, y + height - 2, paint);
                    // Fuse spark
                    float sparkPh = 0.5f + 0.5f * (float) Math.sin(now / 100.0);
                    paint.setColor(Color.argb((int) (255 * sparkPh), 255, 255, 100));
                    canvas.drawCircle(cx + 7, y + height - 2, 3 * sparkPh, paint);
                    // Warning tint
                    paint.setColor(Color.argb(25, 255, 150, 0));
                    canvas.drawRect(rect, paint);
                    break;

                case TYPE_ELITE:
                    // Gold crown (larger, more prominent)
                    paint.setColor(Color.argb(240, 255, 215, 0));
                    canvas.drawRect(cx - 7, y - 5, cx + 7, y - 3, paint);
                    canvas.drawRect(cx - 7, y - 9, cx - 4, y - 5, paint);
                    canvas.drawRect(cx - 1, y - 10, cx + 1, y - 5, paint);
                    canvas.drawRect(cx + 4, y - 9, cx + 7, y - 5, paint);
                    // Gold tint + sparkle
                    paint.setColor(Color.argb(50, 255, 215, 0));
                    canvas.drawRect(rect, paint);
                    // Gold outline
                    paint.setColor(Color.argb(120, 255, 215, 0));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    canvas.drawRect(x - 2, y - 2, x + length + 2, y + height + 2, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setStrokeWidth(1);
                    break;
            }
        }
    }

}
