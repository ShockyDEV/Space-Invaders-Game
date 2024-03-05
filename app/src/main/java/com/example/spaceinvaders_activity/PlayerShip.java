package com.example.spaceinvaders_activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

public class PlayerShip {

    private RectF rect;
    private Bitmap bitmap;
    private float length_EGG;
    private float height_EGG;
    private float x;
    private float y;
    private long lastShotTime_EGG = System.currentTimeMillis();
    private long shotCooldown_EGG = 500; // Cooldown que puede usar el jugador entre disparos

    // Constructor
    public PlayerShip(Context context, int screenX, int screenY){
        rect = new RectF();

        length_EGG = screenX/10;
        height_EGG = screenY/10;

        // Colocar la nave en el centro al comenzar la partida
        x = screenX / 2;
        y = screenY - height_EGG;

        // Inicializar el bitmap
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.playership);

        // Ajustar el bitmap a la resolución de pantalla
        bitmap = Bitmap.createScaledBitmap(bitmap, (int) (length_EGG), (int) (height_EGG), false);

    }

    public RectF getRect(){
        return rect;
    }

    public Bitmap getBitmap(){
        return bitmap;
    }

    public float getX(){
        return x;
    }

    public float getHeight_EGG(){
        return height_EGG;
    }

    public float getLength_EGG(){
        return length_EGG;
    }

    // Actualizar la posición del barco según el dedo del usuario
    public void updatePosition(float touchX){
        x = touchX - length_EGG / 2; // Centramos el barco donde toca el usuario
        // Updateamos rect que se usa para detectar los golpes
        rect.left = x;
        rect.right = x + length_EGG;
        rect.top = y;
        rect.bottom = y + height_EGG;
    }

    // Se intenta disparar. Si está en cooldown, dará false, sino, true.
    public boolean tryShoot() {
        long currentTime = System.currentTimeMillis();
        // Checkea si ha pasado suficiente tiempo entre disparos
        if (currentTime - lastShotTime_EGG >= shotCooldown_EGG) {
            lastShotTime_EGG = currentTime; // Resetea el timer
            return true; // Se puede, por tanto, dispara
        }
        return false; // En cooldown, por lo que no dispara
    }

    // El método update se llamará en el update en SpaceInvadersView
    public void update(long fps){
        // Actualiza el movimiento del barco basado en el fps y la posición del dedo
        rect.left = x;
        rect.right = x + length_EGG;
        rect.top = y;
        rect.bottom = y + height_EGG;
    }
}

