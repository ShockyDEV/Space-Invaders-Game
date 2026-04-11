package com.example.spaceinvaders_activity;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class DefenceBrick {

    private RectF rect;

    private boolean isVisible;
    private int hitPoints = 1; // Fortress Mode sets this to 3

    private Bitmap bitmap; // Añadimos un bitmap para la imagen de los bloques

    public DefenceBrick(int row, int column, int shelterNumber, int screenX, int screenY, Bitmap bitmap){

        int width = screenX / 90;
        int height = screenY / 40;

        isVisible = true;

        // Padding de los bloques

        int brickPadding = 1;

        // Número de bloques defensivos
        int shelterPadding = screenX / 9;
        int startHeight = screenY - (screenY /8 * 2);

        rect = new RectF(column * width + brickPadding +
                (shelterPadding * shelterNumber) +
                shelterPadding + shelterPadding * shelterNumber,
                row * height + brickPadding + startHeight,
                column * width + width - brickPadding +
                        (shelterPadding * shelterNumber) +
                        shelterPadding + shelterPadding * shelterNumber,
                row * height + height - brickPadding + startHeight);
    // Asignamos el bitmap y lo reescalamos
        this.bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public RectF getRect(){
        return this.rect;
    }

    public Bitmap getBitmap(){
        return bitmap;
    }

    public void setInvisible(){
        isVisible = false;
    }

    /** Takes a hit. Returns true if destroyed. */
    public boolean takeHit() {
        hitPoints--;
        if (hitPoints <= 0) {
            isVisible = false;
            return true;
        }
        return false;
    }

    public void setHitPoints(int hp) { this.hitPoints = hp; }
    public int getHitPoints() { return hitPoints; }

    public boolean getVisibility(){
        return isVisible;
    }
}
