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
    private boolean paused = true;

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

    // La bala del jugador
    private Bullet bullet;

    // Las balas de los invasores
    private Bullet[] invadersBullets = new Bullet[200];
    private int nextBullet;
    private int maxInvaderBullets = 10;

    // Hasta 60 invasores
    Invader[] invaders = new Invader[60];
    int numInvaders = 0;

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

    private List<Bullet> bullets = new ArrayList<>();

    // Imágenes de ladrillos y fondo
    private Bitmap brickBitmap;
    private Bitmap backgroundBitmap;

    // Música de fondo
    private MediaPlayer backgroundMusic;

    // Dificultad del juego
    private String difficulty;

    private SpaceInvadersActivity activity;

    // Tiempo de la última llamada a dropDownAndReverse en nanosegundos
    private long lastDropDownTime = System.nanoTime();
    // Tiempo de enfriamiento en nanosegundos (1 segundo = 1_000_000_000 nanosegundos)
    private long dropDownCooldown = 1_000_000_000;

    private boolean isFirstRun = true;




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

            descriptor = assetManager.openFd("damageshelter.mp3");
            damageShelterID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("playerlose.mp3");
            playerLoseID = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            // Imprimimos un mensaje de error en la consola
            Log.e("error", "no se pudieron cargar los archivos de sonido");
        }

        this.difficulty = difficulty;
        this.activity = (SpaceInvadersActivity) context;

        prepareLevel();
    }


    public void setDifficulty(String difficulty) {
        // Nos aseguramos de que la lista o el array de invasores no sea nula y que contenga objetos inicializados
        if (invaders != null) {
            for (Invader invader : invaders) {
                if (invader != null) {
                    invader.setDifficulty(difficulty);
                }
            }
        }
    }



    private void prepareLevel(){

        // Solo muestra la pantalla de Game Over si no es la primera ejecución, para evitar problemas al iniciar la aplicación por primera vez
        if (!isFirstRun && activity != null) {
            activity.showGameOverScreen();
        }

        isFirstRun = false;
        numBricks = 0; // Reseteamos el número de bricks cada vez que se prepara un nivel

        // Inicializamos todos los objetos del juego

        // Creamos la nave espacial del jugador
        playerShip = new PlayerShip(context, screenX, screenY);

        // Prepara los laseres del jugador
        bullet = new Bullet(screenY);

        // Inicializamos los laseres de los enemigos
        for(int i = 0; i < invadersBullets.length; i++){
            invadersBullets[i] = new Bullet(screenY);
        }

        int numRows; //TODO:Esta parte no está funcionando bien porque se llama primero a initgame antes que a preparelevel, arreglar si da tiempo.....

        if (difficulty.equals("Facil")) {
            numRows = 5; // Una fila menos para la dificultad fácil
        } else if (difficulty.equals("Dificil")) {
            numRows = 6; // Una fila más para la dificultad difícil
        } else {
            numRows = 5; // Número estándar de filas
            }


        // Creamos un ejercito de enemigos invasores
        numInvaders = 0;
        for (int column = 0; column < 6; column++) {
            for (int row = 0; row < numRows; row++) {
                invaders[numInvaders] = new Invader(context, row, column, screenX, screenY, difficulty);
                numInvaders++;
            }
        }

        if(backgroundMusic == null) { //Inicializamos la música base que sonará mientras jugamos
            backgroundMusic = MediaPlayer.create(context, R.raw.background);
            backgroundMusic.setLooping(true);
        } else if(!backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }

        // Construimos los bloques de defensa
        int brickWidth = screenX / 90;
        int brickHeight = screenY / 40;

        brickBitmap = Bitmap.createScaledBitmap(brickBitmap, brickWidth, brickHeight, false); //Usamos una imagen para los bloques

        for (int shelterNumber = 0; shelterNumber < 4; shelterNumber++) {
            for (int column = 0; column < 10; column++) {
                for (int row = 0; row < 5; row++) {
                    bricks[numBricks] = new DefenceBrick(row, column, shelterNumber, screenX, screenY, brickBitmap);
                    numBricks++;
                }
            }
        }
    }

    @Override
    public void run() {
        while (playing) {

            // Captura el tiempo actual en milisegundos en startFrameTime
            long startFrameTime = System.currentTimeMillis();

            // Actualiza el fotograma (frame) si no está pausado
            if (!paused) {
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
        // Si el enemigo invasor ha chocado contra la pared
        boolean chocado = false;

        // Si el jugador ha perdido
        boolean perdido = false;

        // Mueve la nave del jugador
        playerShip.update(fps);

        // Actualiza a los invasores enemigos si son visibles
        for (int i = 0; i < numInvaders; i++) {
            if (invaders[i].getVisibility()) {
                invaders[i].update(fps);

                if (invaders[i].takeAim(playerShip.getX(), playerShip.getLength_EGG())) {
                    if (invadersBullets[nextBullet] == null) {
                        invadersBullets[nextBullet] = new Bullet(screenY); // Reemplaza screenHeight con la altura de la pantalla
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
            } else {
                // Ahora, verifica colisiones con invasores
                boolean golpeoInvasor = false;
                for (int j = 0; j < numInvaders; j++) {
                    if (invaders[j].getVisibility() && RectF.intersects(bala.getRect(), invaders[j].getRect())) {
                        invaders[j].setInvisible();
                        soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);
                        score += 10;

                        if (score == numInvaders * 10) {
                            paused = true;
                            score = 0;
                            lives = 3;
                            prepareLevel();
                        }
                        golpeoInvasor = true;
                        break; // El laser ha golpeado a un invasor enemigo, por tanto no se verifica más
                    }
                }

                // Si el laser  golpeó a un invasor, la elimina
                if (golpeoInvasor) {
                    bullets.remove(i);
                    i--; // Se ajusta el índice después de la eliminación
                    continue; // Saltar al siguiente láser
                }

                // Si la bala no golpeó a un invasor, verifica colisiones con los bloques defensivos
                for (int j = 0; j < numBricks; j++) {
                    DefenceBrick brick = bricks[j];
                    if (brick.getVisibility() && RectF.intersects(bala.getRect(), brick.getRect())) {
                        brick.setInvisible();
                        soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                        bullets.remove(i);
                        i--; // Se ajusta después de la eliminación
                        break; // El laser ha golpeado a un brick defensivo, no es necesario verificar otros bricks
                    }
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
                    lives--;
                    soundPool.play(playerExplodeID, 1, 1, 0, 0, 1);

                    if (lives <= 0) {
                        paused = true;
                        lives = 3;
                        score = 0;
                        prepareLevel();
                    }
                    // Elimina el laser del array
                    invadersBullets[i] = null;
                    continue; // Saltar el procesamiento adicional para este laser
                }

                // Verifica la colisión con los bloques defensivos
                for (int j = 0; j < numBricks; j++) {
                    if (bricks[j].getVisibility() && RectF.intersects(balaInvasor.getRect(), bricks[j].getRect())) {
                        bricks[j].setInvisible();
                        soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                        // Elimina el laser del array
                        invadersBullets[i] = null;

                        break;
                    }
                }
            }
        }

        // Si Un invasor chocó contra el borde de la pantalla
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
                if (invaders[i].getY() > screenY - screenY / 10) {
                    perdido = true;
                }
            }
        }

        if (perdido) {
            soundPool.play(playerLoseID, 1, 1, 0, 0, 1);
            prepareLevel();
        }

        // Verifica colisiones entre los laseres de los invasores y los bricks de la defensa
        boolean colisionConBloque = false;
        for (int i = 0; i < invadersBullets.length; i++) {
            Bullet balaInvasor = invadersBullets[i];
            if (balaInvasor != null && !balaInvasor.isOffScreen(screenY)) {
                balaInvasor.update(fps);

                for (int j = 0; j < numBricks; j++) {
                    if (bricks[j].getVisibility() && RectF.intersects(balaInvasor.getRect(), bricks[j].getRect())) {
                        bricks[j].setInvisible();
                        soundPool.play(damageShelterID, 1, 1, 0, 0, 1);
                        colisionConBloque = true;

                        // Elimina el laser del array
                        invadersBullets[i] = null;

                        break;
                    }

                    if (colisionConBloque) {
                        invadersBullets[i] = null;
                    }
                }
            }
        }
    }





    private void draw() {
        if (ourHolder.getSurface().isValid()) {
            canvas = ourHolder.lockCanvas();

            // Dibuja la imagen de fondo del juego
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);


            paint.setColor(Color.argb(255, 255, 255, 255));

            // Dibuja la nave espacial del jugador
            canvas.drawBitmap(playerShip.getBitmap(), playerShip.getX(), screenY - playerShip.getHeight_EGG(), paint);

            // Dibuja a los invasores
            for (int i = 0; i < numInvaders; i++) {
                if (invaders[i].getVisibility()) {
                    canvas.drawBitmap(invaders[i].getBitmap(), invaders[i].getX(), invaders[i].getY(), paint);
                }
            }

            // Dibuja los bricks si son visibles
            for (DefenceBrick brick : bricks) {
                if (brick != null && brick.getVisibility()) {
                    canvas.drawBitmap(brick.getBitmap(), brick.getRect().left, brick.getRect().top, null);
                }
            }

            // Establece el color en verde para los laseres del jugador
            paint.setColor(Color.GREEN);
            // Dibuja todos los laseres del jugador
            for (Bullet bullet : bullets) {
                canvas.drawRect(bullet.getRect(), paint);
            }

            // Establece el color en rojo para los laseres de los invasores
            paint.setColor(Color.RED);
            // Dibuja los laseres de los invasores
            for (Bullet invaderBullet : invadersBullets) {
                if (invaderBullet != null) {
                    canvas.drawRect(invaderBullet.getRect(), paint);
                }
            }

            // Dibuja la puntuación y las vidas restantes
            paint.setColor(Color.argb(255, 249, 129, 0));
            paint.setTextSize(80);
            canvas.drawText("Puntuación: " + score + " Vidas: " + lives, 20, 90, paint);

            // Dibuja en en la pantalla
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }






    // La clase SurfaceView implementa onTouchListener
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
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
                    // Crea y dispara una nueva bala
                    Bullet nuevaBala = new Bullet(screenY);
                    nuevaBala.shoot(playerShip.getX() + playerShip.getLength_EGG() / 2, screenY - playerShip.getHeight_EGG(), Bullet.UP);
                    bullets.add(nuevaBala); // Agrega a la lista de balas
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
            gameThread.join();
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

