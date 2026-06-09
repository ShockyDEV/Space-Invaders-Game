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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpaceInvadersEngine extends SurfaceView implements Runnable{

    Context context;

    // Este es nuestro hilo (thread)
    private Thread gameThread = null;

    // Nuestro SurfaceHolder para bloquear la superficie antes de dibujar nuestros gráficos
    private SurfaceHolder ourHolder;

    // Un booleano que estableceremos y desestableceremos
    // cuando el juego esté en ejecución o no.
    private volatile boolean playing;

    // El juego está pausado al inicio
    private volatile boolean paused = true;

    // Indica si ya se ha preparado una partida (startGame). Evita dibujar/actualizar objetos nulos.
    private volatile boolean gameStarted = false;

    // Un objeto Canvas y Paint
    private Canvas canvas;
    private Paint paint;

    // Esta variable rastrea la velocidad de fotogramas del juego
    private long fps;

    // Se utiliza para calcular la velocidad de fotogramas por segundo (fps)
    private long timeThisFrame;

    // El tamaño de la pantalla en píxeles
    private int screenX;
    private int screenY;

    // La nave del jugador
    private PlayerShip playerShip;

    // Las balas de los invasores (el tamaño del pool se ajusta por nivel)
    private Bullet[] invadersBullets;
    private int nextBullet;
    private int maxInvaderBullets = 10;

    // Hasta 60 invasores
    Invader[] invaders = new Invader[60];
    int numInvaders = 0;

    // Invasores que quedan por destruir para completar el nivel
    private int invadersRemaining = 0;

    // Los refugios del jugador están construidos con ladrillos
    private DefenceBrick[] bricks = new DefenceBrick[400];
    private int numBricks;

    // Para efectos de sonido (FX)
    private SoundPool soundPool;
    private int playerExplodeID = -1;
    private int invaderExplodeID = -1;
    private int shootID = -1;
    private int damageShelterID = -1;
    private int playerLoseID = -1;

    // La puntuación
    int score = 0;

    // Vidas
    private int lives = 3;

    // Balas del jugador, propiedad exclusiva del hilo del juego
    private List<Bullet> bullets = new ArrayList<>();

    // Balas disparadas desde el hilo de UI (onTouchEvent); se vacían al inicio de update()
    private final Queue<Bullet> pendingPlayerBullets = new ConcurrentLinkedQueue<>();

    // --- Capa de progresión ---
    private GameData gameData;
    private final ParticleEffect particleEffect = new ParticleEffect();
    private final List<PowerUp> powerUps = new ArrayList<>();

    private int currentLevel = 1;
    private GameConfig levelConfig;
    private boolean endlessMode = false;
    private List<Integer> activeSkillIds = new ArrayList<>();

    // Contadores por partida (se reinician en startGame)
    private int killsThisGame = 0;
    private long gameStartTime = 0;
    private int xpEarnedThisGame = 0;
    private boolean deathThisGame = false;

    // Imágenes de ladrillos y fondo
    private Bitmap brickBitmap;
    private Bitmap backgroundBitmap;

    // Música de fondo
    private MediaPlayer backgroundMusic;

    // Dificultad del juego (modificador heredado; la escala real la aporta GameConfig)
    private String difficulty;

    private SpaceInvadersActivity activity;

    // Tiempo de la última llamada a dropDownAndReverse en nanosegundos
    private long lastDropDownTime = System.nanoTime();
    // Tiempo de enfriamiento en nanosegundos (1 segundo = 1_000_000_000 nanosegundos)
    private long dropDownCooldown = 1_000_000_000;




    // Cuando inicializamos (llamamos a new()) en gameView
    // Ejecutamos el método principal de la clase
    public SpaceInvadersEngine(Context context, int x, int y, String difficulty) {

        super(context);

        // Creamos una copia globalmente disponible del contexto para poder usarlo en otro método
        this.context = context;

        // Inicializamos los objetos ourHolder y paint
        ourHolder = getHolder();
        paint = new Paint();

        screenX = x;
        screenY = y;

        gameData = new GameData(context);

        brickBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.brick);

        backgroundBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.universe);

        // Escalamos la imagen para que se ajuste a la pantalla
        backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, screenX, screenY, false);

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

        try {
            // Creamos objetos de las 2 clases requeridas
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Cargamos nuestros efectos de sonido en la memoria

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
            // Imprimimos un mensaje de error en la consola
            Log.e("error", "no se pudieron cargar los archivos de sonido");
        }

        this.difficulty = difficulty;
        this.activity = (SpaceInvadersActivity) context;

        // NOTA: no preparamos un nivel aquí. La Activity llama a startGame() con el
        // nivel y las habilidades seleccionadas, evitando construir un nivel desechable.
    }


    // Inicia una nueva partida desde un nivel concreto con las habilidades elegidas.
    public void startGame(int level, List<Integer> skillIds, boolean endless) {
        this.endlessMode = endless;
        this.currentLevel = Math.max(1, level);
        this.activeSkillIds = (skillIds != null) ? new ArrayList<>(skillIds) : new ArrayList<>();

        // Reinicio de los contadores de la partida
        score = 0;
        killsThisGame = 0;
        xpEarnedThisGame = 0;
        deathThisGame = false;
        gameStartTime = System.currentTimeMillis();

        levelConfig = configForLevel(currentLevel);
        prepareLevel(levelConfig);

        // Vidas base + bonificación de la habilidad Escudo (solo al iniciar la partida)
        lives = 3 + playerShip.getExtraLives();

        gameStarted = true;
        paused = true; // Espera a que el jugador toque para empezar
    }

    private GameConfig configForLevel(int lvl) {
        return lvl <= GameConfig.MAX_LEVEL ? GameConfig.getLevel(lvl) : GameConfig.getEndlessLevel(lvl);
    }

    // Avanza al siguiente nivel manteniendo puntuación y vidas (la progresión se conserva).
    private void advanceLevel() {
        awardXP(GameData.XP_PER_LEVEL_COMPLETE * currentLevel);
        if (!deathThisGame) {
            awardXP(GameData.XP_BONUS_NO_DEATHS);
        }
        gameData.updateHighestLevel(currentLevel);

        currentLevel++;
        if (currentLevel > GameConfig.MAX_LEVEL) {
            endlessMode = true;
        }
        levelConfig = configForLevel(currentLevel);

        paused = true; // El jugador toca para iniciar el siguiente nivel
        prepareLevel(levelConfig);
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        if (invaders != null) {
            for (Invader invader : invaders) {
                if (invader != null) {
                    invader.setDifficulty(difficulty);
                }
            }
        }
    }


    private void prepareLevel(GameConfig config) {

        numBricks = 0; // Reseteamos el número de bricks cada vez que se prepara un nivel
        particleEffect.clear();

        // Creamos la nave espacial del jugador y aplicamos las habilidades activas
        playerShip = new PlayerShip(context, screenX, screenY);
        playerShip.applySkills(activeSkillIds);

        // Las balas en vuelo se descartan al preparar un nivel
        bullets.clear();
        pendingPlayerBullets.clear();
        powerUps.clear();

        // Inicializamos el pool de laseres de los enemigos según la configuración del nivel
        maxInvaderBullets = config.maxInvaderBullets;
        invadersBullets = new Bullet[maxInvaderBullets];
        for (int i = 0; i < invadersBullets.length; i++) {
            invadersBullets[i] = new Bullet(screenY);
        }
        nextBullet = 0;

        // Creamos un ejercito de enemigos invasores según la rejilla del nivel
        numInvaders = 0;
        for (int column = 0; column < config.numColumns; column++) {
            for (int row = 0; row < config.numRows; row++) {
                invaders[numInvaders] = new Invader(context, row, column, screenX, screenY,
                        config.invaderBaseSpeed, config.shotChance);
                numInvaders++;
            }
        }

        // Jefe (boss) si el nivel lo incluye
        if (config.hasBoss) {
            invaders[numInvaders] = new Invader(context, screenX, screenY,
                    config.bossSpeed, config.bossHealth);
            numInvaders++;
        }
        invadersRemaining = numInvaders;

        if (backgroundMusic == null) { //Inicializamos la música base que sonará mientras jugamos
            backgroundMusic = MediaPlayer.create(context, R.raw.background);
            backgroundMusic.setLooping(true);
        } else if (!backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }

        // Construimos los bloques de defensa
        int brickWidth = screenX / 90;
        int brickHeight = screenY / 40;

        brickBitmap = Bitmap.createScaledBitmap(brickBitmap, brickWidth, brickHeight, false); //Usamos una imagen para los bloques

        for (int shelterNumber = 0; shelterNumber < config.numShelters; shelterNumber++) {
            for (int column = 0; column < 10; column++) {
                for (int row = 0; row < 5; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber, screenX, screenY, brickBitmap);
                    numBricks++;
                }
            }
        }

        // Cartel de inicio de nivel
        particleEffect.addBanner("Nivel " + currentLevel + ": " + config.levelName,
                Color.CYAN, screenX, screenY, 60);
    }

    @Override
    public void run() {
        while (playing) {

            // Captura el tiempo actual en milisegundos en startFrameTime
            long startFrameTime = System.currentTimeMillis();

            // Actualiza el fotograma (frame) si no está pausado
            if (!paused && gameStarted) {
                update();
            }

            // Dibuja el fotograma
            draw();

            // Calcula los fps (fotogramas por segundo) en este fotograma
            // Luego podemos usar el resultado para
            // sincronizar animaciones.
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }


    private void update() {
        // Vacía las balas encoladas desde el hilo de UI antes de iterar la lista (evita ConcurrentModificationException)
        Bullet queued;
        while ((queued = pendingPlayerBullets.poll()) != null) {
            bullets.add(queued);
        }

        // Efectos visuales (partículas, popups, carteles)
        particleEffect.update(fps);

        // Regeneración de vida (habilidad)
        if (playerShip.checkRegeneration()) {
            lives++;
            particleEffect.addScorePopup(playerShip.getX(), screenY - playerShip.getHeight_EGG(),
                    "+1 VIDA", Color.GREEN, 45);
        }

        // Si el enemigo invasor ha chocado contra la pared
        boolean chocado = false;

        // Si el jugador ha perdido (los invasores llegaron abajo)
        boolean perdido = false;

        // Mueve la nave del jugador
        playerShip.update(fps);

        // Actualiza a los invasores enemigos si son visibles
        for (int i = 0; i < numInvaders; i++) {
            if (invaders[i].getVisibility()) {
                invaders[i].update(fps);

                if (invaders[i].takeAim(playerShip.getX(), playerShip.getLength_EGG())) {
                    if (invadersBullets[nextBullet] == null) {
                        invadersBullets[nextBullet] = new Bullet(screenY);
                    }
                    invadersBullets[nextBullet].shoot(invaders[i].getX() + invaders[i].getLength() / 2, invaders[i].getY(), Bullet.DOWN);

                    // Incrementa nextBullet y restablece si es necesario
                    nextBullet++;
                    if (nextBullet == maxInvaderBullets) {
                        nextBullet = 0;
                    }
                }

                if (invaders[i].getX() > screenX - invaders[i].getLength() || invaders[i].getX() < 0) {
                    chocado = true;
                }
            }
        }

        // Actualiza y verifica colisiones para cada uno de los laseres del jugador
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bala = bullets.get(i);
            bala.update(fps);

            // Primero, verifica si el laser está fuera de la pantalla
            if (bala.getImpactPointY() < 0) {
                bullets.remove(i);
                i--; // Ajusta el índice después de eliminar
                continue;
            }

            // Colisiones con invasores
            boolean consumida = false;
            for (int j = 0; j < numInvaders; j++) {
                if (invaders[j].getVisibility() && RectF.intersects(bala.getRect(), invaders[j].getRect())) {
                    boolean nivelCompletado = hitInvader(j, bala);
                    if (nivelCompletado) {
                        // advanceLevel() ha reconstruido los arrays; abandona este frame
                        return;
                    }
                    if (!bala.isPiercing()) {
                        consumida = true;
                        break; // Las balas normales se detienen en el primer invasor
                    }
                    // Las balas perforantes continúan hacia los invasores de detrás
                }
            }

            if (consumida) {
                bullets.remove(i);
                i--;
                continue;
            }

            // Las balas perforantes atraviesan también los bloques defensivos
            if (bala.isPiercing()) {
                continue;
            }

            // Si la bala no golpeó a un invasor, verifica colisiones con los bloques defensivos
            for (int j = 0; j < numBricks; j++) {
                DefenceBrick brick = bricks[j];
                if (brick.getVisibility() && RectF.intersects(bala.getRect(), brick.getRect())) {
                    brick.setInvisible();
                    soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                    bullets.remove(i);
                    i--;
                    break;
                }
            }
        }

        // Actualiza todas los laseres de los invasores si están activos
        for (int i = 0; i < invadersBullets.length; i++) {
            Bullet balaInvasor = invadersBullets[i];
            if (balaInvasor != null && !balaInvasor.isOffScreen(screenY)) {
                balaInvasor.update(fps);

                // Verifica la colisión con la nave del jugador
                if (RectF.intersects(balaInvasor.getRect(), playerShip.getRect())) {
                    invadersBullets[i] = null;

                    if (playerShip.isShielded()) {
                        // El escudo absorbe el impacto sin perder vida
                        particleEffect.addExplosion(playerShip.getX() + playerShip.getLength_EGG() / 2,
                                screenY - playerShip.getHeight_EGG(), Color.rgb(0, 150, 255), 8);
                        continue;
                    }

                    lives--;
                    deathThisGame = true;
                    soundPool.play(playerExplodeID, 1, 1, 0, 0, 1);
                    particleEffect.addExplosion(playerShip.getX() + playerShip.getLength_EGG() / 2,
                            screenY - playerShip.getHeight_EGG(), Color.RED, 15);

                    if (lives <= 0) {
                        gameOver();
                        return;
                    }
                    continue; // Saltar el procesamiento adicional para este laser
                }

                // Verifica la colisión con los bloques defensivos
                for (int j = 0; j < numBricks; j++) {
                    if (bricks[j].getVisibility() && RectF.intersects(balaInvasor.getRect(), bricks[j].getRect())) {
                        bricks[j].setInvisible();
                        soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                        invadersBullets[i] = null;
                        break;
                    }
                }
            }
        }

        // Power-ups: caída, recogida y descarte
        for (int i = 0; i < powerUps.size(); i++) {
            PowerUp pu = powerUps.get(i);
            pu.update(fps);

            if (!pu.isActive() || pu.isOffScreen(screenY)) {
                powerUps.remove(i);
                i--;
                continue;
            }

            if (RectF.intersects(pu.getRect(), playerShip.getRect())) {
                applyPowerUp(pu);
                pu.collect();
                powerUps.remove(i);
                i--;
            }
        }

        // Si un invasor chocó contra el borde de la pantalla
        if (chocado) {
            // Verifica si ha pasado suficiente tiempo desde la última llamada
            if ((System.nanoTime() - lastDropDownTime) > dropDownCooldown) {
                for (int i = 0; i < numInvaders; i++) {
                    invaders[i].dropDownAndReverse();
                }
                // Actualiza el tiempo de la última llamada
                lastDropDownTime = System.nanoTime();
            }

            for (int i = 0; i < numInvaders; i++) {
                if (invaders[i].getVisibility() && invaders[i].getY() > screenY - screenY / 10) {
                    perdido = true;
                }
            }
        }

        if (perdido) {
            soundPool.play(playerLoseID, 1, 1, 0, 0, 1);
            deathThisGame = true;
            gameOver();
        }
    }

    // Procesa el impacto de una bala del jugador sobre el invasor j.
    // Devuelve true si con esta muerte se completa el nivel (y se ha avanzado de nivel).
    private boolean hitInvader(int j, Bullet bala) {
        Invader inv = invaders[j];
        float cx = inv.getX() + inv.getLength() / 2;
        float cy = inv.getY() + inv.getHeight() / 2;

        if (inv.isBoss()) {
            boolean defeated = inv.takeDamage(bala.getDamage());
            particleEffect.addExplosion(cx, cy, Color.YELLOW, 6); // chispas de impacto
            if (!defeated) {
                return false; // El jefe sigue vivo
            }
            int pts = inv.getPointValue() * playerShip.getScoreMultiplier();
            score += pts;
            awardXP(GameData.XP_PER_BOSS_KILL);
            killsThisGame++;
            invadersRemaining--;
            particleEffect.addBigExplosion(cx, cy);
            particleEffect.addScorePopup(cx, cy, "+" + pts, Color.YELLOW, 55);
            soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);
            maybeDropPowerUp(cx, cy);
        } else {
            inv.setInvisible();
            int pts = inv.getPointValue() * playerShip.getScoreMultiplier();
            score += pts;
            awardXP(GameData.XP_PER_KILL);
            killsThisGame++;
            invadersRemaining--;
            particleEffect.addExplosion(cx, cy, Color.GREEN, 12);
            particleEffect.addScorePopup(cx, cy, "+" + pts, Color.WHITE, 40);
            soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);
            maybeDropPowerUp(cx, cy);
        }

        if (invadersRemaining <= 0) {
            advanceLevel();
            return true;
        }
        return false;
    }

    private void maybeDropPowerUp(float x, float y) {
        if (PowerUp.shouldDrop()) {
            powerUps.add(PowerUp.createRandom(x, y, screenX));
        }
    }

    private void applyPowerUp(PowerUp pu) {
        int type = pu.getType();
        switch (type) {
            case PowerUp.HEALTH:
                lives++;
                break;
            case PowerUp.RAPID_FIRE:
            case PowerUp.SHIELD_BUBBLE:
            case PowerUp.SCORE_BOOST:
                playerShip.applyPowerUp(type);
                break;
            case PowerUp.FREEZE:
                for (int k = 0; k < numInvaders; k++) {
                    if (invaders[k].getVisibility()) {
                        invaders[k].freeze(PowerUp.BUFF_DURATION);
                    }
                }
                break;
        }
        particleEffect.addBanner(pu.getName(), pu.getColor(), screenX, screenY, 50);
    }

    // Otorga XP, sube de nivel de jugador y desbloquea habilidades nuevas.
    private void awardXP(int amount) {
        xpEarnedThisGame += amount;
        boolean leveledUp = gameData.addXP(amount);
        if (leveledUp) {
            int pl = gameData.getPlayerLevel();
            for (Skill s : Skill.getAvailableSkills(pl)) {
                if (!gameData.isSkillUnlocked(s.id)) {
                    gameData.unlockSkill(s.id);
                }
            }
            particleEffect.addBanner("¡Nivel de jugador " + pl + "!", Color.YELLOW, screenX, screenY, 55);
        }
    }

    // Fin de la partida: persiste estadísticas y muestra la pantalla de Game Over.
    private void gameOver() {
        paused = true;
        gameStarted = false;

        long timePlayed = System.currentTimeMillis() - gameStartTime;
        boolean newHigh = gameData.updateHighScore(score);
        gameData.updateHighestLevel(currentLevel);
        gameData.addKills(killsThisGame);
        gameData.saveGameRecord(score, currentLevel, killsThisGame, lives, timePlayed, xpEarnedThisGame);

        int finalScore = score;
        int reachedLevel = currentLevel;
        int playerLevel = gameData.getPlayerLevel();
        int xpEarned = xpEarnedThisGame;
        int highScore = gameData.getHighScore();

        if (activity != null) {
            activity.showGameOverScreen(finalScore, reachedLevel, playerLevel, xpEarned, highScore, newHigh);
        }
    }


    private void draw() {
        if (!ourHolder.getSurface().isValid()) {
            return;
        }
        canvas = ourHolder.lockCanvas();
        if (canvas == null) {
            return;
        }

        // Dibuja la imagen de fondo del juego
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);

        // Antes de iniciar la primera partida solo mostramos el fondo
        if (!gameStarted || playerShip == null) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(70);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Space Invaders", screenX / 2f, screenY / 2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            ourHolder.unlockCanvasAndPost(canvas);
            return;
        }

        paint.setColor(Color.argb(255, 255, 255, 255));

        // Dibuja la nave espacial del jugador y sus efectos (escudo, buffs)
        canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(), screenY - playerShip.getHeight_EGG(), paint);
        playerShip.drawEffects(canvas, paint);

        // Dibuja a los invasores y sus efectos (barra de vida del jefe, parpadeo, congelación)
        for (int i = 0; i < numInvaders; i++) {
            if (invaders[i].getVisibility()) {
                canvas.drawBitmap(invaders[i].getBitmap(), invaders[i].getX(), invaders[i].getY(), paint);
                invaders[i].drawEffects(canvas, paint);
            }
        }

        // Dibuja los bricks si son visibles
        for (DefenceBrick brick : bricks) {
            if (brick != null && brick.getVisibility()) {
                canvas.drawBitmap(brick.getBitmap(), brick.getRect().left, brick.getRect().top, null);
            }
        }

        // Dibuja los power-ups que están cayendo
        for (PowerUp pu : powerUps) {
            pu.draw(canvas, paint);
        }

        // Dibuja todos los laseres del jugador (con sus variantes y brillo)
        for (Bullet bullet : bullets) {
            bullet.draw(canvas, paint, true);
        }

        // Dibuja los laseres de los invasores
        for (Bullet invaderBullet : invadersBullets) {
            if (invaderBullet != null) {
                invaderBullet.draw(canvas, paint, false);
            }
        }

        // Efectos de partículas/popups/carteles por encima del juego
        particleEffect.draw(canvas, paint);

        // HUD: puntuación, vidas, nivel y récord
        paint.setColor(Color.argb(255, 249, 129, 0));
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Puntuación: " + score + "   Vidas: " + lives, 20, 70, paint);
        String levelLabel = endlessMode ? ("Infinito " + (currentLevel - GameConfig.MAX_LEVEL)) : ("Nivel " + currentLevel);
        canvas.drawText(levelLabel + "   Récord: " + gameData.getHighScore(), 20, 140, paint);

        // Dibuja en la pantalla
        ourHolder.unlockCanvasAndPost(canvas);
    }


    // Dispara una o varias balas según las habilidades activas (multi-shot, variantes).
    private void fireBullets() {
        float startX = playerShip.getX() + playerShip.getLength_EGG() / 2;
        float startY = screenY - playerShip.getHeight_EGG();

        if (playerShip.isMultiShot()) {
            enqueueBullet(startX - 40, startY);
            enqueueBullet(startX, startY);
            enqueueBullet(startX + 40, startY);
        } else {
            enqueueBullet(startX, startY);
        }
    }

    private void enqueueBullet(float x, float y) {
        Bullet b;
        if (playerShip.isBigLaser()) {
            b = Bullet.createBigLaser(screenY);
        } else if (playerShip.isPiercingShot()) {
            b = Bullet.createPiercing(screenY);
        } else {
            b = new Bullet(screenY);
        }
        b.shoot(x, y, Bullet.UP);
        pendingPlayerBullets.add(b);
    }


    // La clase SurfaceView implementa onTouchListener
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!gameStarted || playerShip == null) {
            return true;
        }

        int action = motionEvent.getAction();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (paused && motionEvent.getY() > screenY - screenY / 8) {
                paused = false;
            }
            playerShip.updatePosition(motionEvent.getX());
        } else if (action == MotionEvent.ACTION_UP) {
            if (!paused && motionEvent.getY() < screenY - screenY / 8) {
                // Verifica si la nave del jugador está lista para disparar
                if (playerShip.tryShoot()) {
                    fireBullets();
                    soundPool.play(shootID, 1, 1, 0, 0, 1);
                }
                // De lo contrario, no disparar porque la nave está en cooldown
            }
        }

        return true;
    }


    // Si SpaceInvadersActivity se pausa/detiene,
    // detiene el hilo.
    public void pause() {
        playing = false;

        // Detiene y libera la música solo si está reproduciéndose
        if (backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.stop(); // Detiene la música
            }
            backgroundMusic.release(); // Libera los recursos de MediaPlayer
            backgroundMusic = null; // Anula el objeto MediaPlayer
        }

        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            Log.e("Error:", "unirse al hilo");
        }
    }

    // Si SpaceInvadersActivity se inicia,
    // se inicia nuestro hilo.
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();

        // Inicia la música al reanudar el juego
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }
}
