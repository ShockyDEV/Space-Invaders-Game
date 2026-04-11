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
    private Invader[] invaders = new Invader[80];
    private Invader boss = null;
    private int numInvaders = 0;
    private DefenceBrick[] bricks = new DefenceBrick[400];
    private int numBricks;
    private List<PowerUp> powerUps = new ArrayList<>();

    // Systems
    private GameState state;
    private GameData gameData;
    private HUDRenderer hudRenderer;
    private ParticleEffect particles;

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
    }

    // Called by activity to start a new game with selected skills
    public void startNewGame(List<Integer> activeSkills) {
        int extraLives = 0;
        if (activeSkills != null && activeSkills.contains(Skill.SHIELD)) {
            extraLives = 2;
        }
        state.startNewGame(activeSkills, extraLives);
        prepareLevel();

        // Apply skills to player
        playerShip.applySkills(activeSkills);

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

        // Create invaders from level config
        numInvaders = 0;
        for (int column = 0; column < config.numColumns; column++) {
            for (int row = 0; row < config.numRows; row++) {
                invaders[numInvaders] = new Invader(context, row, column, screenX, screenY,
                        config.invaderBaseSpeed, config.shotChance);
                numInvaders++;
            }
        }

        // Create boss if level has one
        boss = null;
        if (config.hasBoss) {
            boss = new Invader(context, screenX, screenY, config.bossSpeed, config.bossHealth);
        }

        // Build defence shelters
        int brickWidth = screenX / 90;
        int brickHeight = screenY / 40;
        Bitmap scaledBrick = Bitmap.createScaledBitmap(brickBitmap, brickWidth, brickHeight, false);

        for (int shelterNumber = 0; shelterNumber < config.numShelters; shelterNumber++) {
            for (int column = 0; column < 10; column++) {
                for (int row = 0; row < 5; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber,
                            screenX, screenY, scaledBrick);
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
                backgroundMusic.start();
            }
        } else if (!backgroundMusic.isPlaying()) {
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
        // Handle level transition
        if (state.levelTransition) {
            if (state.isLevelTransitionDone()) {
                state.levelTransition = false;
            }
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

        for (int i = 0; i < numInvaders; i++) {
            if (invaders[i].getVisibility()) {
                aliveCount++;
                if (state.enemiesFrozen) {
                    invaders[i].freeze(100);
                }
                invaders[i].update(fps);

                // Enemy shooting
                if (invaders[i].takeAim(playerShip.getX(), playerShip.getLength_EGG())) {
                    if (invadersBullets[nextBullet] != null) {
                        invadersBullets[nextBullet].shoot(
                                invaders[i].getX() + invaders[i].getLength() / 2,
                                invaders[i].getY(), Bullet.DOWN);
                    }
                    nextBullet = (nextBullet + 1) % state.levelConfig.maxInvaderBullets;
                }

                // Wall collision
                if (invaders[i].getX() > screenX - invaders[i].getLength() || invaders[i].getX() < 0) {
                    hitWall = true;
                }
                // Reached bottom
                if (invaders[i].getY() > screenY - screenY / 10f) {
                    reachedBottom = true;
                }
            }
        }

        // Update boss
        if (boss != null && boss.getVisibility()) {
            aliveCount++;
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
        }

        // Wall hit -> drop and reverse
        if (hitWall && (System.nanoTime() - lastDropDownTime) > dropDownCooldown) {
            for (int i = 0; i < numInvaders; i++) {
                invaders[i].dropDownAndReverse();
            }
            lastDropDownTime = System.nanoTime();
        }

        // Enemies reached bottom = lose a life and reset level
        if (reachedBottom) {
            soundPool.play(playerLoseID, 1, 1, 0, 0, 1);
            state.onPlayerHit();
            if (state.gameOver) {
                endGame();
            } else {
                prepareLevel();
            }
            return;
        }

        // Update player bullets and check collisions
        updatePlayerBullets();

        // Update invader bullets and check collisions
        updateInvaderBullets();

        // Update power-ups
        updatePowerUps();

        // Update particles
        particles.update(fps);

        // Check level complete
        if (aliveCount == 0) {
            onLevelComplete();
        }
    }

    private void updatePlayerBullets() {
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
            for (int j = 0; j < numInvaders; j++) {
                if (invaders[j].getVisibility() && RectF.intersects(b.getRect(), invaders[j].getRect())) {
                    invaders[j].setInvisible();
                    soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);

                    int points = invaders[j].getPointValue();
                    state.registerKill(points, playerShip.getScoreMultiplier());

                    // Particle explosion
                    float ex = invaders[j].getX() + invaders[j].getLength() / 2;
                    float ey = invaders[j].getY() + invaders[j].getHeight() / 2;
                    particles.addExplosion(ex, ey, Color.rgb(255, 150, 50), 12);

                    // Score popup
                    int displayPoints = points * state.getScoreMultiplier() * playerShip.getScoreMultiplier();
                    String popupText = "+" + displayPoints;
                    if (state.comboCount > 1) popupText += " x" + state.comboCount;
                    particles.addScorePopup(ex, ey, popupText, Color.YELLOW, screenY / 30f);

                    // Power-up drop
                    if (PowerUp.shouldDrop()) {
                        powerUps.add(PowerUp.createRandom(ex, ey, screenX));
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
                boolean defeated = boss.takeDamage(b.getDamage());
                soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);

                float bx = boss.getX() + boss.getLength() / 2;
                float by = boss.getY() + boss.getHeight() / 2;
                particles.addExplosion(bx, by, Color.rgb(255, 100, 100), 6);

                if (defeated) {
                    state.registerBossKill(playerShip.getScoreMultiplier());
                    particles.addBigExplosion(bx, by);
                    particles.addBanner("BOSS DEFEATED!", Color.rgb(255, 215, 0),
                            screenX, screenY, screenY / 12f);
                }

                if (!b.isPiercing()) hitSomething = true;
            }

            // Check vs bricks
            if (!hitSomething) {
                for (int j = 0; j < numBricks; j++) {
                    if (bricks[j].getVisibility() && RectF.intersects(b.getRect(), bricks[j].getRect())) {
                        bricks[j].setInvisible();
                        soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                        hitSomething = true;
                        break;
                    }
                }
            }

            if (hitSomething) {
                it.remove();
            }
        }
    }

    private void updateInvaderBullets() {
        for (int i = 0; i < invadersBullets.length; i++) {
            Bullet b = invadersBullets[i];
            if (b == null || b.isOffScreen(screenY)) continue;

            b.update(fps);

            // Check vs player
            if (RectF.intersects(b.getRect(), playerShip.getRect())) {
                if (playerShip.isShielded()) {
                    // Shield absorbs hit
                    particles.addBanner("SHIELD!", Color.rgb(0, 150, 255),
                            screenX, screenY, screenY / 15f);
                } else {
                    state.onPlayerHit();
                    soundPool.play(playerExplodeID, 1, 1, 0, 0, 1);
                    particles.addExplosion(playerShip.getX() + playerShip.getLength_EGG() / 2,
                            screenY - playerShip.getHeight_EGG() / 2, Color.RED, 15);

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
                    bricks[j].setInvisible();
                    soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                    invadersBullets[i] = new Bullet(screenY);
                    break;
                }
            }
        }
    }

    private void updatePowerUps() {
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp pu = it.next();
            pu.update(fps);

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
                state.freezeEnemies();
                break;
        }
    }

    private void onLevelComplete() {
        state.onLevelComplete();

        particles.addBanner("LEVEL " + state.currentLevel + " COMPLETE!",
                Color.rgb(100, 255, 100), screenX, screenY, screenY / 12f);

        // No death bonus
        if (state.lives == state.startingLives) {
            state.xpEarned += GameData.XP_BONUS_NO_DEATHS;
        }

        // Advance and prepare next level
        state.advanceLevel();
        prepareLevel();
    }

    private void endGame() {
        state.gameOver = true;
        state.paused = true;

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

    // Bomb skill - clear all visible enemies
    public void useBomb() {
        if (!playerShip.hasBombSkill() || !playerShip.isBombAvailable()) return;
        playerShip.useBomb();
        state.bombUsedThisLevel = true;

        for (int i = 0; i < numInvaders; i++) {
            if (invaders[i].getVisibility()) {
                invaders[i].setInvisible();
                float ex = invaders[i].getX() + invaders[i].getLength() / 2;
                float ey = invaders[i].getY() + invaders[i].getHeight() / 2;
                particles.addExplosion(ex, ey, Color.rgb(255, 100, 50), 8);
                state.registerKill(invaders[i].getPointValue(), playerShip.getScoreMultiplier());
            }
        }

        particles.addBanner("ORBITAL BOMB!", Color.RED, screenX, screenY, screenY / 10f);
        soundPool.play(playerExplodeID, 1, 1, 0, 0, 1);
    }

    // ==================== DRAW ====================

    private void draw() {
        if (!ourHolder.getSurface().isValid()) return;

        canvas = ourHolder.lockCanvas();

        // Background
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);

        // Defence bricks
        for (int i = 0; i < numBricks; i++) {
            if (bricks[i] != null && bricks[i].getVisibility()) {
                canvas.drawBitmap(bricks[i].getBitmap(),
                        bricks[i].getRect().left, bricks[i].getRect().top, null);
            }
        }

        // Invaders
        for (int i = 0; i < numInvaders; i++) {
            if (invaders[i].getVisibility()) {
                canvas.drawBitmap(invaders[i].getBitmap(),
                        invaders[i].getX(), invaders[i].getY(), paint);
                invaders[i].drawEffects(canvas, paint);
            }
        }

        // Boss
        if (boss != null && boss.getVisibility()) {
            canvas.drawBitmap(boss.getBitmap(), boss.getX(), boss.getY(), paint);
            boss.drawEffects(canvas, paint);
        }

        // Player ship
        canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(),
                screenY - playerShip.getHeight_EGG(), paint);
        playerShip.drawEffects(canvas, paint);

        // Player bullets
        for (Bullet b : playerBullets) {
            b.draw(canvas, paint, true);
        }

        // Invader bullets
        paint.setColor(Color.RED);
        for (Bullet b : invadersBullets) {
            if (b != null) {
                canvas.drawRect(b.getRect(), paint);
            }
        }

        // Power-ups
        for (PowerUp pu : powerUps) {
            pu.draw(canvas, paint);
        }

        // Particle effects
        particles.draw(canvas, paint);

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

        // Pause overlay (but not during game over or transition)
        if (state.paused && !state.gameOver && !state.levelTransition && !state.isFirstRun) {
            hudRenderer.drawPauseScreen(canvas, paint);
        }

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

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            // Tap to dismiss game over
            if (state.gameOver && action == MotionEvent.ACTION_DOWN) {
                state.isFirstRun = true;
                activity.showMainMenu();
                return true;
            }

            // Tap to unpause
            if (state.paused && !state.gameOver) {
                state.paused = false;
            }

            // Move ship
            if (!state.paused) {
                playerShip.updatePosition(motionEvent.getX());
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (!state.paused && !state.gameOver && !state.levelTransition) {
                if (motionEvent.getY() < screenY - screenY / 8f) {
                    if (playerShip.tryShoot()) {
                        shootBullets();
                    }
                }
            }
        }

        return true;
    }

    private void shootBullets() {
        float shipCenterX = playerShip.getX() + playerShip.getLength_EGG() / 2;
        float shipTopY = screenY - playerShip.getHeight_EGG();

        if (playerShip.isMultiShot()) {
            // Multi-shot: 3 bullets in spread
            for (int i = -1; i <= 1; i++) {
                Bullet b = createPlayerBullet();
                b.shoot(shipCenterX + i * 30, shipTopY, Bullet.UP);
                playerBullets.add(b);
            }
        } else {
            Bullet b = createPlayerBullet();
            b.shoot(shipCenterX, shipTopY, Bullet.UP);
            playerBullets.add(b);
        }

        soundPool.play(shootID, 1, 1, 0, 0, 1);
    }

    private Bullet createPlayerBullet() {
        if (playerShip.hasUltimateLaserSkill()) {
            return Bullet.createUltimate(screenY);
        } else if (playerShip.isBigLaser()) {
            return Bullet.createBigLaser(screenY);
        } else if (playerShip.isPiercingShot()) {
            return Bullet.createPiercing(screenY);
        }
        return new Bullet(screenY);
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
