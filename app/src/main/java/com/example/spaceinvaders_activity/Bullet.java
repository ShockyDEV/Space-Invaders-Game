package com.example.spaceinvaders_activity;

import android.graphics.RectF;

public class Bullet {

    private float x;
    private float y;

    private RectF rect;

    // Si dispara hacia arriba o hacia abajo
    public static final int UP = 0;
    public static final int DOWN = 1;

    int heading = -1; // Dirección del laser
    float speed = 400; // Velocidad del laser

    private int width = 1; // Anchura del laser
    private int height; // Altura del laser

    public Bullet(int screenY) {
        height = screenY / 20;
        this.width = 10;
        rect = new RectF();
    }

    public RectF getRect(){
        return rect;
    }

    // Disparar un nuevo laser
    public void shoot(float startX, float startY, int direction) {
        x = startX;
        y = startY;
        heading = direction;
    }

    public float getImpactPointY(){
        if (heading == DOWN) {
            return y + height;
        } else {
            return y;
        }
    }

    public void update(long fps) {
        // El laser debe ir o hacia arriba o hacia abajo
        if (heading == UP) {
            y -= speed / fps;
        } else {
            y += speed / fps;
        }

        // Actualizamos la posición rectagular del laser
        rect.left = x;
        rect.right = x + width;
        rect.top = y;
        rect.bottom = y + height;
    }

    // Checkeamos que el laser se haya ido de la pantalla
    public boolean isOffScreen(int screenY) {
        // Checkeamos si el laser se ha ido fuera de los parámetros de la pantalla
        return (heading == UP && y < 0) || (heading == DOWN && y > screenY);
    }
}

