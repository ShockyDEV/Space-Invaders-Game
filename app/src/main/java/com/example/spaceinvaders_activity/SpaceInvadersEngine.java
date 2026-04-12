package com.example.spaceinvaders_activity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpaceInvadersEngine extends SurfaceView implements Runnable {

    private Context context;
    private Thread gameThread = null;
    private SurfaceHolder ourHolder;
    private volatile boolean playing;
    private Canvas canvas;
    private Paint paint;
    private long fps;
    private long timeThisFrame;
    private int screenX, screenY;

    // Game objects
    private PlayerShip playerShip;
    private List<Bullet> playerBullets = new ArrayList<>();
    private Bullet[] invadersBullets = new Bullet[200];
    private int nextBullet;
    private ArrayList<Invader> invaders = new ArrayList<>();
    private Invader boss = null;
    private DefenceBrick[] bricks = new DefenceBrick[400];
    private int numBricks;
    private List<PowerUp> powerUps = new ArrayList<>();

    // Systems
    private GameState state;
    private GameData gameData;
    private HUDRenderer hudRenderer;
    private ParticleEffect particles;
    private AchievementManager achievementManager;
    private VFXManager vfx;
    private BossController bossController;

    // Sound
    private SoundPool soundPool;
    private int playerExplodeID = -1;
    private int invaderExplodeID = -1;
    private int shootID = -1;
    private int damageShelterID = -1;
    private int playerLoseID = -1;

    // Graphics
    private Bitmap brickBitmap;
    private Bitmap backgroundBitmap;
    private MediaPlayer backgroundMusic;

    // Timing
    private long lastDropDownTime = System.nanoTime();
    private long dropDownCooldown = 1_000_000_000;

    // Screen shake
    private float shakeOffsetX = 0, shakeOffsetY = 0;
    private float shakeIntensity = 0;
    private long shakeEndTime = 0;
    private Random shakeRandom = new Random();

    // Ultimate laser touch tracking
    private long touchDownTime = 0;
    private float touchDownX = 0, touchDownY = 0;
    private boolean isHoldingForUltimate = false;
    private static final float HOLD_MOVE_THRESHOLD = 40f;

    // Audio volume settings
    private float musicVolume = 0.8f;
    private float sfxVolume = 0.8f;

    private SpaceInvadersActivity activity;

    public SpaceInvadersEngine(Context context, int x, int y) {
        super(context);
        this.context = context;
        this.activity = (SpaceInvadersActivity) context;
        ourHolder = getHolder();
        paint = new Paint();
        screenX = x;
        screenY = y;

        // Init systems
        state = new GameState();
        gameData = new GameData(context);
        hudRenderer = new HUDRenderer(screenX, screenY);
        particles = new ParticleEffect();
        achievementManager = new AchievementManager(gameData);
        vfx = new VFXManager(screenX, screenY);

        // Load graphics
        brickBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.brick);
        backgroundBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.universe);
        backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, screenX, screenY, false);

        // Load sounds (fixed: no duplicate loading)
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("shoot.mp3");
            shootID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("invaderexplode.mp3");
            invaderExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("damageshelter.mp3");
            damageShelterID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("playerexplode.mp3");
            playerExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("playerlose.mp3");
            playerLoseID = soundPool.load(descriptor, 0);
        } catch (IOException e) {
            Log.e("SpaceInvaders", "Failed to load sound files", e);
        }

        // Load volume settings
        reloadSettings();
    }

    // --- Screen Shake ---

    private void triggerShake(float intensity, long durationMs) {
        shakeIntensity = intensity;
        shakeEndTime = System.currentTimeMillis() + durationMs;
    }

    private void updateShake() {
        long now = System.currentTimeMillis();
        if (now < shakeEndTime) {
            float decay = Math.min(1f, (shakeEndTime - now) / 300f);
            shakeOffsetX = (shakeRandom.nextFloat() - 0.5f) * 2 * shakeIntensity * decay;
            shakeOffsetY = (shakeRandom.nextFloat() - 0.5f) * 2 * shakeIntensity * decay;
        } else {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
        }
    }

    // --- Settings ---

    public void reloadSettings() {
        musicVolume = gameData.getMusicVolume() / 100f;
        sfxVolume = gameData.getSfxVolume() / 100f;
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(musicVolume, musicVolume);
        }
    }

    private void playSfx(int soundId) {
        soundPool.play(soundId, sfxVolume, sfxVolume, 0, 0, 1);
    }

    // Called by activity to start a new game with selected skills
    public void startNewGame(List<Integer> activeSkills) {
        startNewGame(activeSkills, GameConfig.DIFF_NORMAL);
    }

    public void startNewGame(List<Integer> activeSkills, int difficulty) {
        startNewGame(activeSkills, difficulty, GameState.MODE_CAMPAIGN);
    }

    public void startNewGame(List<Integer> activeSkills, int difficulty, int gameMode) {
        int extraLives = 0;
        if (activeSkills != null && activeSkills.contains(Skill.SHIELD)) {
            extraLives = 2;
        }
        state.startNewGame(activeSkills, extraLives, difficulty, gameMode);
        prepareLevel();

        // Apply skills to player
        playerShip.applySkills(activeSkills);

        // Apply skill effects on game state
        if (activeSkills != null) {
            if (activeSkills.contains(Skill.COMBO_MASTER)) {
                state.effectiveComboWindow = 3500; // 3.5s instead of 2s
            }
            if (activeSkills.contains(Skill.SALVAGE_BOT)) {
                state.xpMultiplier = 1.5f; // +50% XP
            }
        }

        // Apply selected skin
        playerShip.applySkin(gameData.getSelectedSkin());

        // Start tutorial if first time
        if (!gameData.isTutorialDone()) {
            state.tutorialActive = true;
            state.tutorialStep = 0;
            state.tutorialStepShownAt = System.currentTimeMillis();
            state.paused = true; // Pause during tutorial
        }

        // Start music
        startMusic();
    }

    private void prepareLevel() {
        GameConfig config = state.levelConfig;
        numBricks = 0;
        nextBullet = 0;

        // Player ship
        playerShip = new PlayerShip(context, screenX, screenY);
        playerShip.applySkills(state.activeSkills);

        // Clear bullets
        playerBullets.clear();
        for (int i = 0; i < invadersBullets.length; i++) {
            invadersBullets[i] = new Bullet(screenY);
        }

        // Clear power-ups
        powerUps.clear();
        particles.clear();

        // Create invaders from level config with enemy type assignment
        invaders.clear();
        for (int column = 0; column < config.numColumns; column++) {
            for (int row = 0; row < config.numRows; row++) {
                int enemyType = config.getEnemyType(row, config.numRows);
                Invader inv = new Invader(context, row, column, screenX, screenY,
                        config.invaderBaseSpeed, config.shotChance, enemyType);
                invaders.add(inv);
            }
        }

        // Create boss if level has one
        boss = null;
        bossController = null;
        if (config.hasBoss) {
            boss = new Invader(context, screenX, screenY, config.bossSpeed, config.bossHealth);
            bossController = new BossController(config.bossType, boss, screenX, screenY);
        }

        // Build defence shelters
        int brickWidth = screenX / 90;
        int brickHeight = screenY / 40;
        Bitmap scaledBrick = Bitmap.createScaledBitmap(brickBitmap, brickWidth, brickHeight, false);

        boolean fortress = playerShip.hasFortressModeSkill();
        for (int shelterNumber = 0; shelterNumber < config.numShelters; shelterNumber++) {
            for (int column = 0; column < 10; column++) {
                for (int row = 0; row < 5; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber,
                            screenX, screenY, scaledBrick);
                    if (fortress) bricks[numBricks].setHitPoints(3);
                    numBricks++;
                }
            }
        }

        // Background music
        startMusic();
    }

    private void startMusic() {
        if (backgroundMusic == null) {
            backgroundMusic = MediaPlayer.create(context, R.raw.background);
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(musicVolume, musicVolume);
                backgroundMusic.start();
            }
        } else if (!backgroundMusic.isPlaying()) {
            backgroundMusic.setVolume(musicVolume, musicVolume);
            backgroundMusic.start();
        }
    }

    // ==================== GAME LOOP ====================

    @Override
    public void run() {
        while (playing) {
            long startFrameTime = System.currentTimeMillis();
            if (!state.paused) {
                update();
            }
            draw();
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    // ==================== UPDATE ====================

    private void update() {
        // Handle level transition (campaign mode only)
        if (state.levelTransition) {
            if (state.isLevelTransitionDone()) {
                state.levelTransition = false;
            }
            return;
        }

        // Time attack: check if time expired
        if (state.isTimeAttackExpired()) {
            endGame();
            return;
        }

        // Update freeze state
        state.updateFreeze();

        // Regeneration skill check
        if (playerShip.checkRegeneration() && state.lives < state.startingLives) {
            state.lives++;
            particles.addBanner("+1 LIFE", Color.GREEN, screenX, screenY, screenY / 15f);
        }

        // Update player
        playerShip.update(fps);

        // Update invaders
        boolean hitWall = false;
        boolean reachedBottom = false;
        int aliveCount = 0;

        // Temporal Shift: slow all enemies 20%
        float enemySpeedMult = playerShip.hasTemporalShiftSkill() ? 0.8f : 1.0f;

        for (Invader inv : invaders) {
            if (inv.getVisibility()) {
                aliveCount++;
                inv.setSpeedMultiplier(enemySpeedMult);
                if (state.enemiesFrozen) {
                    inv.freeze(100);
                }

                // Update kamikaze target
                if (inv.isKamikaze()) {
                    inv.setTargetPlayerX(playerShip.getX() + playerShip.getLength_EGG() / 2);
                }

                inv.update(fps);

                // Enemy shooting
                if (inv.takeAim(playerShip.getX(), playerShip.getLength_EGG())) {
                    if (invadersBullets[nextBullet] != null) {
                        invadersBullets[nextBullet].shoot(
                                inv.getX() + inv.getLength() / 2,
                                inv.getY(), Bullet.DOWN);
                    }
                    nextBullet = (nextBullet + 1) % state.levelConfig.maxInvaderBullets;
                }

                // Wall collision (skip for kamikaze, they move freely)
                if (!inv.isKamikaze()) {
                    if (inv.getX() > screenX - inv.getLength() || inv.getX() < 0) {
                        hitWall = true;
                    }
                }
                // Reached bottom
                if (inv.getY() > screenY - screenY / 10f) {
                    reachedBottom = true;
                }
            }
        }

        // Update boss
        if (boss != null && boss.getVisibility()) {
            aliveCount++;
            boss.setSpeedMultiplier(enemySpeedMult);
            boss.update(fps);
            // Boss shooting (more frequent)
            if (boss.takeAim(playerShip.getX(), playerShip.getLength_EGG())) {
                if (invadersBullets[nextBullet] != null) {
                    invadersBullets[nextBullet].shoot(
                            boss.getX() + boss.getLength() / 2,
                            boss.getY() + boss.getHeight(), Bullet.DOWN);
                }
                nextBullet = (nextBullet + 1) % state.levelConfig.maxInvaderBullets;
            }
            // Boss bounces at walls
            if (boss.getX() > screenX - boss.getLength() || boss.getX() < 0) {
                boss.dropDownAndReverse();
            }

            // Boss AI special attacks
            if (bossController != null) {
                BossController.BossAction action = bossController.update(fps,
                        playerShip.getX() + playerShip.getLength_EGG() / 2, state);
                handleBossAction(action);
            }
        }

        // Wall hit -> drop and reverse
        if (hitWall && (System.nanoTime() - lastDropDownTime) > dropDownCooldown) {
            for (Invader inv : invaders) {
                if (!inv.isKamikaze()) { // Kamikaze don't follow formation
                    inv.dropDownAndReverse();
                }
            }
            lastDropDownTime = System.nanoTime();
        }

        // Enemies reached bottom = lose a life and reset level
        if (reachedBottom) {
            playSfx(playerLoseID);
            state.onPlayerHit();
            if (state.gameOver) {
                endGame();
            } else {
                prepareLevel();
            }
            return;
        }

        // Healer enemies: periodically heal adjacent invaders
        for (Invader inv : invaders) {
            if (inv.getVisibility() && inv.shouldHeal()) {
                // Find nearest visible non-healer invader and heal +1 HP
                float hx = inv.getX() + inv.getLength() / 2;
                float hy = inv.getY() + inv.getHeight() / 2;
                float bestDist = Float.MAX_VALUE;
                Invader target = null;
                for (Invader other : invaders) {
                    if (other == inv || !other.getVisibility()) continue;
                    if (other.getEnemyType() == Invader.TYPE_HEALER) continue;
                    if (other.getHealth() >= other.getMaxHealth()) continue;
                    float dx = other.getX() + other.getLength() / 2 - hx;
                    float dy = other.getY() + other.getHeight() / 2 - hy;
                    float d = dx * dx + dy * dy;
                    if (d < bestDist) {
                        bestDist = d;
                        target = other;
                    }
                }
                if (target != null) {
                    target.heal(1);
                    float tx = target.getX() + target.getLength() / 2;
                    float ty = target.getY() + target.getHeight() / 2;
                    particles.addSparkleBurst(tx, ty, Color.rgb(50, 255, 50), 5);
                }
            }
        }

        // Update player bullets and check collisions
        updatePlayerBullets();

        // Update invader bullets and check collisions
        updateInvaderBullets();

        // Update power-ups
        updatePowerUps();

        // Update particles, VFX, and screen shake
        particles.update(fps);
        vfx.update(fps);
        vfx.setComboIntensity(state.comboCount);
        updateShake();

        // Ship thruster
        if (!state.paused) {
            float shipCX = playerShip.getX() + playerShip.getLength_EGG() / 2;
            float shipBottom = screenY;
            vfx.emitThruster(playerShip.getX(), shipCX, shipBottom);
        }

        // Bullet trails
        for (Bullet b : playerBullets) {
            vfx.addBulletTrail(b.getCenterX(), b.getCenterY(),
                    b.getColor(), b.getWidth(), 8);
        }

        // Check level complete
        if (aliveCount == 0) {
            onLevelComplete();
        }
    }

    // Temporary list for splitter children to spawn after iteration
    private List<Invader> pendingSplitterChildren = new ArrayList<>();

    private void updatePlayerBullets() {
        pendingSplitterChildren.clear();

        Iterator<Bullet> it = playerBullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(fps);

            if (b.getImpactPointY() < 0) {
                it.remove();
                continue;
            }

            boolean hitSomething = false;

            // Check vs invaders
            for (Invader inv : invaders) {
                if (inv.getVisibility() && RectF.intersects(b.getRect(), inv.getRect())) {
                    // Cloaker: immune while cloaked
                    if (inv.isCloaked()) {
                        continue;
                    }

                    // Shielded enemy: absorb first hit
                    if (inv.hasShield()) {
                        inv.hitShield();
                        playSfx(damageShelterID);
                        particles.addBanner("SHIELD BREAK!", Color.rgb(100, 200, 255),
                                screenX, screenY, screenY / 18f);
                        // VFX: blue debris + shockwave for shield break
                        float sx = inv.getX() + inv.getLength() / 2;
                        float sy = inv.getY() + inv.getHeight() / 2;
                        particles.addDebris(sx, sy, Color.rgb(80, 160, 255), 10);
                        vfx.addShockwave(sx, sy, 60f, Color.rgb(100, 200, 255), 3f);
                        if (!b.isPiercing()) hitSomething = true;
                        break;
                    }

                    // Tank type: has health > 1
                    if (inv.getEnemyType() == Invader.TYPE_TANK && inv.getHealth() > 1) {
                        inv.takeDamage(b.getDamage());
                        playSfx(invaderExplodeID);
                        if (inv.getHealth() <= 0) {
                            inv.setInvisible();
                        } else {
                            if (!b.isPiercing()) hitSomething = true;
                            break;
                        }
                    } else {
                        inv.setInvisible();
                    }

                    playSfx(invaderExplodeID);

                    int points = inv.getPointValue();
                    state.registerKill(points, playerShip.getScoreMultiplier());

                    float ex = inv.getX() + inv.getLength() / 2;
                    float ey = inv.getY() + inv.getHeight() / 2;

                    // Type-specific death effects
                    switch (inv.getEnemyType()) {
                        case Invader.TYPE_KAMIKAZE:
                            // Big fiery explosion with shockwave
                            particles.addBigExplosion(ex, ey);
                            vfx.addShockwave(ex, ey, 100f, Color.rgb(255, 80, 0), 4f);
                            vfx.triggerScreenFlash(Color.rgb(255, 100, 0), 0.15f);
                            break;
                        case Invader.TYPE_SPLITTER:
                            // Green energy burst
                            particles.addSparkleBurst(ex, ey, Color.rgb(50, 255, 50), 18);
                            particles.addExplosion(ex, ey, Color.rgb(100, 255, 100), 10);
                            vfx.addShockwave(ex, ey, 70f, Color.rgb(50, 255, 50), 3f);
                            break;
                        case Invader.TYPE_SHIELDED:
                            // Blue shattering burst
                            particles.addDebris(ex, ey, Color.rgb(100, 180, 255), 12);
                            particles.addExplosion(ex, ey, Color.rgb(150, 200, 255), 10);
                            break;
                        case Invader.TYPE_TANK:
                            // Heavy explosion with debris
                            particles.addExplosion(ex, ey, Color.rgb(255, 150, 50), 15);
                            particles.addDebris(ex, ey, Color.rgb(180, 120, 60), 8);
                            vfx.addShockwave(ex, ey, 80f, Color.rgb(255, 180, 80), 3f);
                            break;
                        case Invader.TYPE_SCOUT:
                            // Fast sparky explosion
                            particles.addExplosion(ex, ey, Color.rgb(255, 255, 100), 10);
                            particles.addImpactSparks(ex, ey, Color.rgb(255, 255, 150), 8, 270f);
                            break;
                        case Invader.TYPE_HEALER:
                            // Green healing burst
                            particles.addSparkleBurst(ex, ey, Color.rgb(50, 255, 50), 15);
                            particles.addExplosion(ex, ey, Color.rgb(100, 255, 100), 10);
                            break;
                        case Invader.TYPE_CLOAKER:
                            // Purple phasing effect
                            particles.addSparkleBurst(ex, ey, Color.rgb(180, 50, 255), 12);
                            vfx.addShockwave(ex, ey, 60f, Color.rgb(180, 50, 255), 2f);
                            break;
                        case Invader.TYPE_BOMBER:
                            // Orange explosion (handled by bomberExplosion, just add sparks)
                            particles.addImpactSparks(ex, ey, Color.rgb(255, 200, 50), 10, 0);
                            break;
                        case Invader.TYPE_ELITE:
                            // Gold explosion with debris
                            particles.addExplosion(ex, ey, Color.rgb(255, 215, 0), 15);
                            particles.addDebris(ex, ey, Color.rgb(200, 180, 50), 8);
                            vfx.addShockwave(ex, ey, 80f, Color.rgb(255, 215, 0), 3f);
                            break;
                        default:
                            // Standard explosion
                            particles.addExplosion(ex, ey, Color.rgb(255, 150, 50), 12);
                            break;
                    }
                    // Impact sparks toward the bullet direction
                    particles.addImpactSparks(ex, ey, Color.rgb(255, 200, 100), 5, 270f);

                    int displayPoints = points * state.getScoreMultiplier() * playerShip.getScoreMultiplier();
                    String popupText = "+" + displayPoints;
                    if (state.comboCount > 1) popupText += " x" + state.comboCount;
                    particles.addScorePopup(ex, ey, popupText, Color.YELLOW, screenY / 30f);

                    // Splitter: queue children to spawn
                    if (inv.isSplitter()) {
                        pendingSplitterChildren.add(
                                Invader.createSplitterChild(context, ex, ey, screenX, screenY,
                                        state.levelConfig.invaderBaseSpeed, true));
                        pendingSplitterChildren.add(
                                Invader.createSplitterChild(context, ex, ey, screenX, screenY,
                                        state.levelConfig.invaderBaseSpeed, false));
                    }

                    // Bomber: timed explosion on death, destroys nearby bricks
                    if (inv.hasBombOnDeath()) {
                        bomberExplosion(ex, ey);
                    }

                    // Power-up drop (Lucky Drops doubles chance)
                    if (PowerUp.shouldDrop(playerShip.hasLuckyDropsSkill())) {
                        powerUps.add(PowerUp.createRandom(ex, ey, screenX));
                    }

                    // Scavenger: 25% chance kills drop micro-heal
                    if (playerShip.hasScavengerSkill() && Math.random() < 0.25) {
                        powerUps.add(new PowerUp(PowerUp.HEALTH, ex, ey, screenX));
                    }

                    // Chain Lightning: arc to 2 adjacent enemies
                    if (playerShip.hasChainLightningSkill()) {
                        chainLightning(ex, ey, inv);
                    }

                    if (!b.isPiercing()) {
                        hitSomething = true;
                    }
                    break;
                }
            }

            // Check vs boss
            if (!hitSomething && boss != null && boss.getVisibility()
                    && RectF.intersects(b.getRect(), boss.getRect())) {
                // Boss invulnerable during shield phases
                if (bossController != null && bossController.isInvulnerable()) {
                    float sx = boss.getX() + boss.getLength() / 2;
                    float sy = boss.getY() + boss.getHeight() / 2;
                    particles.addSparkleBurst(sx, sy, Color.rgb(100, 200, 255), 8);
                    if (!b.isPiercing()) hitSomething = true;
                    if (hitSomething) { it.remove(); }
                    continue;
                }
                boolean defeated = boss.takeDamage(b.getDamage());
                playSfx(invaderExplodeID);

                float bx = boss.getX() + boss.getLength() / 2;
                float by = boss.getY() + boss.getHeight() / 2;
                // Boss hit: sparks fly off impact point + small explosion
                particles.addExplosion(bx, by, Color.rgb(255, 100, 100), 6);
                particles.addImpactSparks(b.getCenterX(), b.getCenterY(),
                        Color.rgb(255, 200, 80), 6, 270f);

                if (defeated) {
                    state.registerBossKill(playerShip.getScoreMultiplier());
                    // Epic multi-layer boss death
                    particles.addBigExplosion(bx, by);
                    particles.addBigExplosion(bx - 40, by - 20);
                    particles.addBigExplosion(bx + 40, by + 20);
                    particles.addSparkleBurst(bx, by, Color.rgb(255, 215, 0), 25);
                    particles.addBanner("BOSS DEFEATED!", Color.rgb(255, 215, 0),
                            screenX, screenY, screenY / 12f);
                    triggerShake(20f, 500);
                    // VFX: massive shockwave + white flash
                    vfx.addShockwave(bx, by, screenX * 0.6f, Color.rgb(255, 200, 50), 8f);
                    vfx.addShockwave(bx, by, screenX * 0.4f, Color.rgb(255, 100, 0), 5f);
                    vfx.triggerScreenFlash(Color.WHITE, 0.6f);
                    checkAchievements();
                }

                if (!b.isPiercing()) hitSomething = true;
            }

            // Check vs bricks
            if (!hitSomething) {
                for (int j = 0; j < numBricks; j++) {
                    if (bricks[j].getVisibility() && RectF.intersects(b.getRect(), bricks[j].getRect())) {
                        bricks[j].takeHit();
                        playSfx(damageShelterID);
                        float bkx = bricks[j].getRect().centerX();
                        float bky = bricks[j].getRect().centerY();
                        particles.addDebris(bkx, bky, Color.rgb(0, 180, 0), 5);
                        hitSomething = true;
                        break;
                    }
                }
            }

            if (hitSomething) {
                it.remove();
            }
        }

        // Spawn splitter children outside the iteration
        if (!pendingSplitterChildren.isEmpty()) {
            invaders.addAll(pendingSplitterChildren);
            particles.addBanner("SPLITTER!", Color.rgb(50, 255, 50),
                    screenX, screenY, screenY / 18f);
        }
    }

    /** Bomber enemy: on death, explodes destroying nearby bricks and damaging player if close */
    private void bomberExplosion(float bx, float by) {
        float blastRadius = screenX / 6f;
        // Destroy nearby bricks
        for (int j = 0; j < numBricks; j++) {
            if (bricks[j].getVisibility()) {
                float dx = bricks[j].getRect().centerX() - bx;
                float dy = bricks[j].getRect().centerY() - by;
                if (dx * dx + dy * dy < blastRadius * blastRadius) {
                    bricks[j].setInvisible();
                }
            }
        }
        // VFX
        particles.addBigExplosion(bx, by);
        vfx.addShockwave(bx, by, blastRadius, Color.rgb(255, 150, 0), 5f);
        vfx.triggerScreenFlash(Color.rgb(255, 100, 0), 0.15f);
        triggerShake(8f, 200);
        particles.addBanner("BOMB!", Color.rgb(255, 150, 0), screenX, screenY, screenY / 18f);
    }

    /** Chain Lightning: arc damage to up to 2 nearby enemies on kill */
    private void chainLightning(float originX, float originY, Invader killed) {
        int arcs = 0;
        float range = screenX / 4f; // arc range
        for (Invader inv : invaders) {
            if (arcs >= 2) break;
            if (!inv.getVisibility() || inv == killed) continue;
            float dx = inv.getX() + inv.getLength() / 2 - originX;
            float dy = inv.getY() + inv.getHeight() / 2 - originY;
            if (dx * dx + dy * dy < range * range) {
                // Arc hits this enemy
                float tx = inv.getX() + inv.getLength() / 2;
                float ty = inv.getY() + inv.getHeight() / 2;

                boolean defeated = inv.takeDamage(1);
                if (defeated || inv.getHealth() <= 0) {
                    inv.setInvisible();
                    state.registerKill(inv.getPointValue(), playerShip.getScoreMultiplier());
                    particles.addExplosion(tx, ty, Color.rgb(100, 180, 255), 10);
                }

                // Lightning arc VFX
                particles.addImpactSparks(tx, ty, Color.rgb(100, 200, 255), 6, 0);
                vfx.addShockwave(tx, ty, 40f, Color.rgb(100, 200, 255), 2f);
                arcs++;
            }
        }
        if (arcs > 0) {
            particles.addBanner("CHAIN LIGHTNING!", Color.rgb(100, 200, 255),
                    screenX, screenY, screenY / 18f);
        }
    }

    private void updateInvaderBullets() {
        for (int i = 0; i < invadersBullets.length; i++) {
            Bullet b = invadersBullets[i];
            if (b == null || b.isOffScreen(screenY)) continue;

            b.update(fps);

            // Check vs player
            if (RectF.intersects(b.getRect(), playerShip.getRect())) {
                float hitX = playerShip.getX() + playerShip.getLength_EGG() / 2;
                float hitY = screenY - playerShip.getHeight_EGG() / 2;

                // Reflect Barrier: 15% chance to reflect bullet back
                if (playerShip.rollReflect()) {
                    b.shoot(hitX, hitY, Bullet.UP); // Reflect upward
                    particles.addSparkleBurst(hitX, hitY, Color.rgb(180, 100, 255), 10);
                    vfx.addShockwave(hitX, hitY, 40f, Color.rgb(180, 100, 255), 2f);
                    particles.addBanner("REFLECT!", Color.rgb(180, 100, 255),
                            screenX, screenY, screenY / 15f);
                    continue; // Don't destroy the bullet, it's now going up
                }

                if (playerShip.isShielded()) {
                    // Shield absorbs hit — sparkle deflection effect
                    particles.addSparkleBurst(hitX, hitY, Color.rgb(80, 180, 255), 12);
                    vfx.addShockwave(hitX, hitY, 50f, Color.rgb(80, 180, 255), 2f);
                    particles.addBanner("SHIELD!", Color.rgb(0, 150, 255),
                            screenX, screenY, screenY / 15f);
                } else if (playerShip.tryNanoShieldAbsorb()) {
                    // Nano Shield absorbs one hit, then goes on cooldown
                    particles.addSparkleBurst(hitX, hitY, Color.rgb(0, 255, 200), 12);
                    vfx.addShockwave(hitX, hitY, 50f, Color.rgb(0, 255, 200), 2f);
                    particles.addBanner("NANO SHIELD!", Color.rgb(0, 255, 200),
                            screenX, screenY, screenY / 15f);
                } else if (playerShip.tryAutoRepair()) {
                    // Auto Repair: 30% chance to survive a lethal hit
                    particles.addSparkleBurst(hitX, hitY, Color.rgb(100, 255, 100), 12);
                    particles.addBanner("AUTO REPAIR!", Color.rgb(100, 255, 100),
                            screenX, screenY, screenY / 15f);
                } else {
                    state.onPlayerHit();
                    playSfx(playerExplodeID);
                    triggerShake(12f, 300);
                    particles.addExplosion(hitX, hitY, Color.RED, 15);
                    particles.addDebris(hitX, hitY, Color.rgb(200, 50, 50), 8);
                    particles.addImpactSparks(hitX, hitY, Color.rgb(255, 150, 50), 10, 90f);
                    vfx.triggerScreenFlash(Color.RED, 0.3f);
                    vfx.addShockwave(hitX, hitY, 80f, Color.RED, 3f);

                    // Emergency Warp: teleport to safe spot on hit
                    if (playerShip.tryEmergencyWarp()) {
                        particles.addBanner("WARP!", Color.rgb(0, 200, 255),
                                screenX, screenY, screenY / 15f);
                        vfx.triggerScreenFlash(Color.rgb(0, 200, 255), 0.2f);
                    }

                    if (state.gameOver) {
                        endGame();
                        return;
                    }
                }
                invadersBullets[i] = new Bullet(screenY);
                continue;
            }

            // Check vs bricks
            for (int j = 0; j < numBricks; j++) {
                if (bricks[j].getVisibility() && RectF.intersects(b.getRect(), bricks[j].getRect())) {
                    bricks[j].takeHit();
                    playSfx(damageShelterID);
                    float bkx = bricks[j].getRect().centerX();
                    float bky = bricks[j].getRect().centerY();
                    particles.addDebris(bkx, bky, Color.rgb(0, 180, 0), 5);
                    invadersBullets[i] = new Bullet(screenY);
                    break;
                }
            }
        }
    }

    private void updatePowerUps() {
        float playerCX = playerShip.getX() + playerShip.getLength_EGG() / 2;
        boolean magnetPull = playerShip.hasMagnetPullSkill();

        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp pu = it.next();
            pu.update(fps);

            // Magnet Pull: power-ups drift toward player
            if (magnetPull && pu.isActive()) {
                pu.attractToward(playerCX, screenY - playerShip.getHeight_EGG(), fps);
            }

            if (pu.isOffScreen(screenY)) {
                it.remove();
                continue;
            }

            // Check collection by player
            if (pu.isActive() && RectF.intersects(pu.getRect(), playerShip.getRect())) {
                pu.collect();
                applyPowerUp(pu);
                it.remove();
            }
        }
    }

    private void applyPowerUp(PowerUp pu) {
        particles.addBanner(pu.getName(), pu.getColor(), screenX, screenY, screenY / 15f);
        // VFX: sparkle burst on power-up collection
        particles.addSparkleBurst(playerShip.getX() + playerShip.getLength_EGG() / 2,
                screenY - playerShip.getHeight_EGG() / 2, pu.getColor(), 15);
        vfx.triggerScreenFlash(pu.getColor(), 0.12f);
        state.powerupsCollectedThisGame++;
        gameData.addPowerupCollected();

        switch (pu.getType()) {
            case PowerUp.HEALTH:
                state.lives++;
                break;
            case PowerUp.RAPID_FIRE:
                playerShip.applyPowerUp(PowerUp.RAPID_FIRE);
                break;
            case PowerUp.SHIELD_BUBBLE:
                playerShip.applyPowerUp(PowerUp.SHIELD_BUBBLE);
                break;
            case PowerUp.SCORE_BOOST:
                playerShip.applyPowerUp(PowerUp.SCORE_BOOST);
                break;
            case PowerUp.FREEZE:
                state.freezeEnemies(playerShip.hasFreezeWaveSkill());
                break;
        }
    }

    private void onLevelComplete() {
        if (state.gameMode == GameState.MODE_SURVIVAL) {
            // Survival: wave complete, immediately spawn next wave
            state.advanceSurvivalWave();
            particles.addBanner("WAVE " + state.survivalWave + "!",
                    Color.rgb(100, 255, 100), screenX, screenY, screenY / 12f);
            prepareSurvivalWave();
            return;
        }

        if (state.gameMode == GameState.MODE_TIME_ATTACK) {
            // Time attack: enemies cleared, respawn immediately
            state.currentLevel++;
            state.levelConfig = GameConfig.getLevel(
                    Math.min(state.currentLevel, GameConfig.MAX_LEVEL))
                    .applyDifficulty(state.difficulty);
            prepareLevel();
            return;
        }

        // Campaign mode
        state.onLevelComplete();

        // XP Surge: triple level-complete XP
        if (playerShip.hasXPSurgeSkill()) {
            state.xpEarned += GameData.XP_PER_LEVEL_COMPLETE * state.currentLevel * 2; // +2x on top of base
        }

        particles.addBanner("LEVEL " + state.currentLevel + " COMPLETE!",
                Color.rgb(100, 255, 100), screenX, screenY, screenY / 12f);

        // No death bonus
        if (state.lives == state.startingLives) {
            state.xpEarned += GameData.XP_BONUS_NO_DEATHS;
        }

        // Check achievements
        checkAchievements();

        // Advance and prepare next level
        state.advanceLevel();
        prepareLevel();
    }

    private void prepareSurvivalWave() {
        GameConfig config = state.levelConfig;
        invaders.clear();
        playerBullets.clear();
        powerUps.clear();
        nextBullet = 0;
        for (int i = 0; i < invadersBullets.length; i++) {
            invadersBullets[i] = new Bullet(screenY);
        }

        for (int column = 0; column < config.numColumns; column++) {
            for (int row = 0; row < config.numRows; row++) {
                int enemyType = config.getEnemyType(row, config.numRows);
                Invader inv = new Invader(context, row, column, screenX, screenY,
                        config.invaderBaseSpeed, config.shotChance, enemyType);
                invaders.add(inv);
            }
        }
        boss = null; // No bosses in survival
    }

    private void endGame() {
        state.gameOver = true;
        state.paused = true;

        // Check achievements before saving
        checkAchievements();

        // Save game data
        boolean newHighScore = gameData.updateHighScore(state.score);
        gameData.updateHighestLevel(state.currentLevel);
        gameData.addXP(state.xpEarned);
        gameData.addKills(state.enemiesKilled);
        gameData.saveGameRecord(state.score, state.currentLevel,
                state.enemiesKilled, state.lives,
                state.getGameDuration(), state.xpEarned);

        if (newHighScore) {
            particles.addBanner("NEW HIGH SCORE!", Color.rgb(255, 215, 0),
                    screenX, screenY, screenY / 10f);
        }
    }

    /** Checks achievements and shows banners for newly unlocked ones */
    private void checkAchievements() {
        List<AchievementManager.Achievement> newlyUnlocked =
                achievementManager.checkAndUnlock(state, gameData);
        for (AchievementManager.Achievement a : newlyUnlocked) {
            particles.addBanner("ACHIEVEMENT: " + a.name, Color.rgb(255, 215, 0),
                    screenX, screenY, screenY / 15f);
            state.xpEarned += a.xpReward;
        }
    }

    // Bomb skill - clear all visible enemies
    public void useBomb() {
        if (!playerShip.hasBombSkill() || !playerShip.isBombAvailable()) return;
        playerShip.useBomb();
        state.bombUsedThisLevel = true;

        int bombKills = 0;
        for (Invader inv : invaders) {
            if (inv.getVisibility()) {
                inv.setInvisible();
                float ex = inv.getX() + inv.getLength() / 2;
                float ey = inv.getY() + inv.getHeight() / 2;
                particles.addExplosion(ex, ey, Color.rgb(255, 100, 50), 8);
                state.registerKill(inv.getPointValue(), playerShip.getScoreMultiplier());
                bombKills++;
            }
        }
        state.bombKillCount = bombKills;

        particles.addBanner("ORBITAL BOMB!", Color.RED, screenX, screenY, screenY / 10f);
        playSfx(playerExplodeID);
        triggerShake(15f, 400);
        // VFX: full-screen shockwave from center + white-orange flash
        vfx.addShockwave(screenX / 2f, screenY / 2f, screenX * 0.8f,
                Color.rgb(255, 150, 50), 10f);
        vfx.triggerScreenFlash(Color.rgb(255, 200, 100), 0.5f);

        // Check bomb master achievement
        checkAchievements();
    }

    // ==================== BOSS ACTION HANDLER ====================

    private void handleBossAction(BossController.BossAction action) {
        if (action == null || action.type == BossController.BossAction.TYPE_NONE) return;

        switch (action.type) {
            case BossController.BossAction.TYPE_MULTI_SHOT:
                // Fire multiple bullets in spread
                for (int i = 0; i < action.count; i++) {
                    float spread = (i - action.count / 2f) * 30;
                    if (invadersBullets[nextBullet] != null) {
                        invadersBullets[nextBullet].shoot(
                                action.x + spread, action.y, Bullet.DOWN);
                    }
                    nextBullet = (nextBullet + 1) % state.levelConfig.maxInvaderBullets;
                }
                break;

            case BossController.BossAction.TYPE_SHIELD_PHASE:
                particles.addSparkleBurst(action.x, action.y, Color.rgb(100, 200, 255), 15);
                vfx.addShockwave(action.x, action.y, 80f, Color.rgb(100, 200, 255), 3f);
                particles.addBanner("SHIELD!", Color.rgb(100, 200, 255),
                        screenX, screenY, screenY / 18f);
                break;

            case BossController.BossAction.TYPE_TELEPORT_SHOOT:
                // Fire homing bullet before teleporting
                if (invadersBullets[nextBullet] != null) {
                    invadersBullets[nextBullet].shoot(action.x, action.y, Bullet.DOWN);
                }
                nextBullet = (nextBullet + 1) % state.levelConfig.maxInvaderBullets;
                vfx.triggerScreenFlash(Color.rgb(150, 50, 255), 0.15f);
                particles.addSparkleBurst(action.x, action.y, Color.rgb(150, 50, 255), 12);
                break;

            case BossController.BossAction.TYPE_BEAM_WARNING:
                particles.addBanner("WARNING!", Color.rgb(255, 150, 0),
                        screenX, screenY, screenY / 18f);
                break;

            case BossController.BossAction.TYPE_BEAM:
                // Beam hits player if within range
                float playerCX = playerShip.getX() + playerShip.getLength_EGG() / 2;
                if (Math.abs(playerCX - action.x) < 40) {
                    state.onPlayerHit();
                    playSfx(playerExplodeID);
                    triggerShake(15f, 300);
                    if (state.gameOver) { endGame(); }
                }
                vfx.triggerScreenFlash(Color.rgb(255, 180, 50), 0.25f);
                break;

            case BossController.BossAction.TYPE_FROST_ZONE:
                // Spread shots + slow player
                for (int i = 0; i < action.count; i++) {
                    float spread = (i - action.count / 2f) * 40;
                    if (invadersBullets[nextBullet] != null) {
                        invadersBullets[nextBullet].shoot(
                                action.x + spread, action.y, Bullet.DOWN);
                    }
                    nextBullet = (nextBullet + 1) % state.levelConfig.maxInvaderBullets;
                }
                particles.addBanner("FROST ZONE!", Color.rgb(100, 220, 255),
                        screenX, screenY, screenY / 18f);
                break;

            case BossController.BossAction.TYPE_LIGHTNING:
                playSfx(playerExplodeID); // Reuse explosion sound for lightning
                triggerShake(8f, 200);
                vfx.triggerScreenFlash(Color.rgb(200, 200, 255), 0.2f);
                // Damage player if in lightning zone
                float px = playerShip.getX() + playerShip.getLength_EGG() / 2;
                int playerZone = (int) (px / (screenX / 3f));
                if (playerZone >= 0 && playerZone < 3) {
                    boolean[] zones = bossController.getLightningZones();
                    if (zones[playerZone]) {
                        state.onPlayerHit();
                        playSfx(playerExplodeID);
                        if (state.gameOver) { endGame(); }
                    }
                }
                break;

            case BossController.BossAction.TYPE_SPAWN_MINIONS:
                for (int i = 0; i < action.count; i++) {
                    float mx = action.x + (i - action.count / 2f) * 60;
                    Invader minion = Invader.createSplitterChild(context,
                            mx, action.y, screenX, screenY,
                            state.levelConfig.invaderBaseSpeed * 1.2f,
                            i % 2 == 0);
                    invaders.add(minion);
                }
                particles.addBanner("MINIONS!", Color.rgb(255, 100, 100),
                        screenX, screenY, screenY / 18f);
                break;

            case BossController.BossAction.TYPE_TIME_WARP:
                particles.addBanner("TIME WARP!", Color.rgb(180, 100, 255),
                        screenX, screenY, screenY / 15f);
                vfx.triggerScreenFlash(Color.rgb(150, 50, 255), 0.2f);
                break;

            case BossController.BossAction.TYPE_SPEED_BURST:
                // Temporarily increase boss speed (handled by boss AI)
                break;

            case BossController.BossAction.TYPE_VFX:
                if (action.vfxSubtype == BossController.BossAction.VFX_TELEPORT_ARRIVE) {
                    particles.addSparkleBurst(action.x, action.y, Color.rgb(150, 50, 255), 15);
                    vfx.addShockwave(action.x, action.y, 60f, Color.rgb(150, 50, 255), 3f);
                } else if (action.vfxSubtype == BossController.BossAction.VFX_VANISH) {
                    particles.addSparkleBurst(action.x, action.y, Color.rgb(100, 0, 200), 10);
                }
                break;
        }
    }

    // ==================== DRAW ====================

    private void draw() {
        if (!ourHolder.getSurface().isValid()) return;

        canvas = ourHolder.lockCanvas();

        // Apply screen shake
        canvas.save();
        canvas.translate(shakeOffsetX, shakeOffsetY);

        // Background
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);

        // VFX Layer 1: Starfield + ambient dust (behind everything)
        vfx.drawStarfield(canvas, paint);
        vfx.drawDust(canvas, paint);

        // VFX Layer 2: Bullet trails (behind ships)
        vfx.drawTrails(canvas, paint);

        // Defence bricks
        for (int i = 0; i < numBricks; i++) {
            if (bricks[i] != null && bricks[i].getVisibility()) {
                canvas.drawBitmap(bricks[i].getBitmap(),
                        bricks[i].getRect().left, bricks[i].getRect().top, null);
            }
        }

        // VFX: Ship thruster (behind player ship)
        vfx.drawThruster(canvas, paint);

        // Invaders
        for (Invader inv : invaders) {
            if (inv.getVisibility()) {
                canvas.drawBitmap(inv.getBitmap(), inv.getX(), inv.getY(), paint);
                inv.drawEffects(canvas, paint);
            }
        }

        // Boss
        if (boss != null && boss.getVisibility()) {
            // Skip drawing if boss is invisible (Void Reaper)
            if (bossController == null || !bossController.isInvisible()) {
                canvas.drawBitmap(boss.getBitmap(), boss.getX(), boss.getY(), paint);
            }
            boss.drawEffects(canvas, paint);
            // Draw boss controller effects (shield, beam, frost, lightning)
            if (bossController != null) {
                bossController.drawEffects(canvas, paint);
            }
        }

        // Player ship (with skin tint)
        Paint shipPaint = playerShip.getSkinPaint();
        canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(),
                screenY - playerShip.getHeight_EGG(), shipPaint != null ? shipPaint : paint);
        playerShip.drawEffects(canvas, paint);

        // Player bullets
        for (Bullet b : playerBullets) {
            b.draw(canvas, paint, true);
        }

        // Invader bullets (enhanced with glow)
        for (Bullet b : invadersBullets) {
            if (b != null && b.getRect().bottom > 0) {
                b.draw(canvas, paint, false);
            }
        }

        // VFX Layer 3: Shockwave rings (over ships, under UI)
        vfx.drawShockwaves(canvas, paint);

        // Power-ups
        for (PowerUp pu : powerUps) {
            pu.draw(canvas, paint);
        }

        // Particle effects
        particles.draw(canvas, paint);

        // VFX Layer 4: Screen flash + combo aura (over everything, under HUD)
        vfx.drawScreenFlash(canvas, paint);
        vfx.drawComboAura(canvas, paint);

        // HUD
        if (!state.gameOver) {
            hudRenderer.drawGameHUD(canvas, paint, state, playerShip);
        }

        // Level transition overlay
        if (state.levelTransition) {
            hudRenderer.drawLevelTransition(canvas, paint, state);
        }

        // Game over overlay
        if (state.gameOver) {
            hudRenderer.drawGameOver(canvas, paint, state);
        }

        // Tutorial overlay
        if (state.tutorialActive) {
            hudRenderer.drawTutorial(canvas, paint, state);
        }

        // Pause overlay (but not during game over, transition, or tutorial)
        if (state.paused && !state.gameOver && !state.levelTransition
                && !state.isFirstRun && !state.tutorialActive) {
            hudRenderer.drawPauseScreen(canvas, paint);
        }

        // Restore from screen shake translation
        canvas.restore();

        ourHolder.unlockCanvasAndPost(canvas);
    }

    // ==================== INPUT ====================

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction() & MotionEvent.ACTION_MASK;

        // Two finger tap = bomb
        if (motionEvent.getPointerCount() >= 2 && action == MotionEvent.ACTION_POINTER_DOWN) {
            useBomb();
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            // Advance tutorial on tap
            if (state.tutorialActive) {
                state.tutorialStep++;
                if (state.tutorialStep >= GameState.TUTORIAL_STEPS) {
                    state.tutorialActive = false;
                    state.paused = false; // Resume game after tutorial
                    gameData.setTutorialDone(true);
                }
                state.tutorialStepShownAt = System.currentTimeMillis();
                return true;
            }

            // Tap to dismiss game over
            if (state.gameOver) {
                state.isFirstRun = true;
                activity.showMainMenu();
                return true;
            }

            // Tap to unpause
            if (state.paused && !state.gameOver) {
                state.paused = false;
            }

            // Record touch down for ultimate laser hold detection
            touchDownTime = System.currentTimeMillis();
            touchDownX = motionEvent.getX();
            touchDownY = motionEvent.getY();

            // Start charging ultimate if available and touching upper area
            if (!state.paused && playerShip.hasUltimateLaserSkill()
                    && playerShip.isUltimateReady()
                    && motionEvent.getY() < screenY - screenY / 8f) {
                isHoldingForUltimate = true;
                playerShip.startCharging();
            }

            // Move ship
            if (!state.paused) {
                playerShip.updatePosition(motionEvent.getX());
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!state.paused) {
                playerShip.updatePosition(motionEvent.getX());
            }

            // Cancel charge if finger moved too far
            if (isHoldingForUltimate) {
                float dx = motionEvent.getX() - touchDownX;
                float dy = motionEvent.getY() - touchDownY;
                if (Math.sqrt(dx * dx + dy * dy) > HOLD_MOVE_THRESHOLD) {
                    playerShip.cancelCharge();
                    isHoldingForUltimate = false;
                }
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (!state.paused && !state.gameOver && !state.levelTransition) {
                // Check if releasing a charged ultimate laser
                if (isHoldingForUltimate && playerShip.releaseCharge()) {
                    shootUltimate();
                    isHoldingForUltimate = false;
                } else {
                    isHoldingForUltimate = false;
                    playerShip.cancelCharge();
                    // Normal shot
                    if (motionEvent.getY() < screenY - screenY / 8f) {
                        if (playerShip.tryShoot()) {
                            shootBullets();
                        }
                    }
                }
            } else {
                isHoldingForUltimate = false;
                playerShip.cancelCharge();
            }
        }

        return true;
    }

    private void shootBullets() {
        float shipCenterX = playerShip.getX() + playerShip.getLength_EGG() / 2;
        float shipTopY = screenY - playerShip.getHeight_EGG();

        // Homing Missiles: every 5th shot is a homing missile
        if (playerShip.checkHomingShot()) {
            Bullet homing = Bullet.createHoming(screenY);
            homing.shoot(shipCenterX, shipTopY, Bullet.UP);
            Invader target = findNearestInvader(shipCenterX, shipTopY);
            if (target != null) {
                homing.setHomingTarget(target.getX() + target.getLength() / 2,
                        target.getY() + target.getHeight() / 2);
            }
            playerBullets.add(homing);
            // VFX for homing missile
            particles.addSparkleBurst(shipCenterX, shipTopY, Color.rgb(255, 200, 0), 8);
        }

        if (playerShip.isMultiShot()) {
            // Multi-shot: 3 bullets in spread
            for (int i = -1; i <= 1; i++) {
                Bullet b = createPlayerBullet();
                applyMomentum(b);
                b.shoot(shipCenterX + i * 30, shipTopY, Bullet.UP);
                playerBullets.add(b);
            }
        } else {
            Bullet b = createPlayerBullet();
            applyMomentum(b);
            b.shoot(shipCenterX, shipTopY, Bullet.UP);
            playerBullets.add(b);
        }

        playSfx(shootID);
    }

    /** Momentum skill: +5% bullet speed per combo kill */
    private void applyMomentum(Bullet b) {
        if (playerShip.hasMomentumSkill() && state.comboCount > 0) {
            float bonus = 1.0f + state.comboCount * 0.05f;
            b.speed *= bonus;
        }
    }

    private void shootUltimate() {
        float shipCenterX = playerShip.getX() + playerShip.getLength_EGG() / 2;
        float shipTopY = screenY - playerShip.getHeight_EGG();
        Bullet b = Bullet.createUltimate(screenY);
        b.shoot(shipCenterX, shipTopY, Bullet.UP);
        playerBullets.add(b);
        playSfx(shootID);
        triggerShake(10f, 200);
        particles.addBanner("ULTIMATE LASER!", Color.rgb(255, 0, 255),
                screenX, screenY, screenY / 12f);
        // VFX: purple flash + upward shockwave
        vfx.triggerScreenFlash(Color.rgb(200, 50, 255), 0.35f);
        vfx.addShockwave(shipCenterX, shipTopY, 120f, Color.rgb(255, 100, 255), 5f);
    }

    private Bullet createPlayerBullet() {
        // Ultimate laser is fired only via hold-to-charge (shootUltimate), not here

        // Critical Hit: 15% chance for 3x damage bullet
        if (playerShip.rollCriticalHit()) {
            return Bullet.createCritical(screenY, playerShip.isBigLaser(), playerShip.isPiercingShot());
        }

        if (playerShip.isBigLaser()) {
            return Bullet.createBigLaser(screenY);
        } else if (playerShip.isPiercingShot()) {
            return Bullet.createPiercing(screenY);
        }
        return new Bullet(screenY);
    }

    /** Find the nearest visible invader to the given position */
    private Invader findNearestInvader(float fromX, float fromY) {
        Invader nearest = null;
        float bestDist = Float.MAX_VALUE;
        for (Invader inv : invaders) {
            if (inv.getVisibility()) {
                float dx = inv.getX() + inv.getLength() / 2 - fromX;
                float dy = inv.getY() + inv.getHeight() / 2 - fromY;
                float d = dx * dx + dy * dy;
                if (d < bestDist) {
                    bestDist = d;
                    nearest = inv;
                }
            }
        }
        // Also check boss
        if (boss != null && boss.getVisibility()) {
            float dx = boss.getX() + boss.getLength() / 2 - fromX;
            float dy = boss.getY() + boss.getHeight() / 2 - fromY;
            float d = dx * dx + dy * dy;
            if (d < bestDist) {
                nearest = boss;
            }
        }
        return nearest;
    }

    // ==================== LIFECYCLE ====================

    public void pause() {
        playing = false;
        state.paused = true;

        if (backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            backgroundMusic.release();
            backgroundMusic = null;
        }

        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException e) {
            Log.e("SpaceInvaders", "Error joining thread", e);
        }
    }

    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();

        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    // Getters for activity
    public GameData getGameData() { return gameData; }
    public GameState getGameState() { return state; }
}
