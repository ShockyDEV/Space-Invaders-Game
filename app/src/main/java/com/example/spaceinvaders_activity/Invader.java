package com.example.spaceinvaders_activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

import java.util.Random;

public class Invader {

    RectF rect;



    Random generator = new Random();

    // La nave del jugador se representa en bitmaps
    private Bitmap bitmap1;
    private Bitmap bitmap2;

    // Altura y grosor del invasor enemigo
    private float length;
    private float height;

    // X la izquierda de la pantalla que usará el invasor
    private float x;

    // Y la coordenada de arriba que usará el invasor
    private float y;

    // Velocidad de la nave
    private float shipSpeed;

    public final int LEFT = 1;
    public final int RIGHT = 2;

    // Si se mueve la nave y en que dirección
    private int shipMoving = RIGHT;

    boolean isVisible;

    private String difficulty;

    private float baseSpeed; // Velocidad base de los invasores
    private int shotChance; // Probabilidad de disparo (1 en 'shotChance')



    public Invader(Context context, int row, int column, int screenX, int screenY, String difficulty) {

        // Inicializamos RectF
        rect = new RectF();

        length = screenX / 20;
        height = screenY / 20;

        isVisible = true;

        int padding = screenX / 25;

        x = column * (length + padding);
        y = row * (length + padding/4);

        // Inicializamos el bitmap
        bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader1);
        bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.invader2);


        // Reajustamos el bitmap apropiadamente a la resolución
        bitmap1 = Bitmap.createScaledBitmap(bitmap1,
                (int) (length),
                (int) (height),
                false);

        // Reajustamos el bitmap apropiadamente a la resolución
        bitmap2 = Bitmap.createScaledBitmap(bitmap2,
                (int) (length),
                (int) (height),
                false);

        // Velocidad de los invasores en pixel por segundo
        shipSpeed = 40;

        setDifficulty(difficulty);
    }


    public void setDifficulty(String difficulty) {
        if (difficulty.equals("Dificil")) {
            baseSpeed = 200; // Velocidad más alta para dificultad difícil
            shotChance = 250; // Mayor probabilidad de disparo para dificultad difícil
        } else {
            baseSpeed = 100; // Velocidad estándar para dificultad fácil
            shotChance = 1000; // Probabilidad estándar de disparo para dificultad fácil
        }
        shipSpeed = baseSpeed; // Inicializa la velocidad de movimiento con la base
    }

    public void setInvisible(){
        isVisible = false;
    }

    public boolean getVisibility(){
        return isVisible;
    }

    public RectF getRect(){
        return rect;
    }

    public Bitmap getBitmap(){
        return bitmap1;
    }

    public Bitmap getBitmap2(){
        return bitmap2;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getLength(){
        return length;
    }

    public void update(long fps){
        if(shipMoving == LEFT){
            x = x - shipSpeed / fps;
        }

        if(shipMoving == RIGHT){
            x = x + shipSpeed / fps;
        }

        // Actualizar rect que se utiliza para detectar los golpes
        rect.top = y;
        rect.bottom = y + height;
        rect.left = x;
        rect.right = x + length;

    }

    public void dropDownAndReverse(){
        if(shipMoving == LEFT){
            shipMoving = RIGHT;
        }else{
            shipMoving = LEFT;
        }

        y = y + height;

        shipSpeed = shipSpeed * 1.12f;
    }

    public boolean takeAim(float playerShipX, float playerShipLength){
        int randomNumber;

        // Si está cerca del jugador
        if ((playerShipX + playerShipLength > x && playerShipX + playerShipLength < x + length)
                || (playerShipX > x && playerShipX < x + length)) {
            // A 1 en  'shotChance' chance de disparar
            randomNumber = generator.nextInt(shotChance);
            if(randomNumber == 0) {
                return true;
            }
        }

        // Si dispara aleatoriamente (no cerca del jugador) a 1 en 'shotChance * 2' chance
        randomNumber = generator.nextInt(shotChance * 2);
        return randomNumber == 0;
    }
}
