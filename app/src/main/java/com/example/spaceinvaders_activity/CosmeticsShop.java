package com.example.spaceinvaders_activity;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines ship skins and manages purchases.
 * Skins are tint-based (PorterDuffColorFilter applied to ship bitmap).
 */
public class CosmeticsShop {

    public static class ShipSkin {
        public final int id;
        public final String name;
        public final int cost; // XP cost (0 = free/default)
        public final int tintColor; // Color to tint the ship (0 = no tint)
        public final boolean isRainbow; // Special: cycles through hues

        public ShipSkin(int id, String name, int cost, int tintColor, boolean isRainbow) {
            this.id = id;
            this.name = name;
            this.cost = cost;
            this.tintColor = tintColor;
            this.isRainbow = isRainbow;
        }
    }

    public static List<ShipSkin> getAllSkins() {
        List<ShipSkin> skins = new ArrayList<>();
        skins.add(new ShipSkin(0, "Default", 0, 0, false));
        skins.add(new ShipSkin(1, "Crimson", 500, Color.rgb(220, 50, 50), false));
        skins.add(new ShipSkin(2, "Ocean", 500, Color.rgb(50, 120, 220), false));
        skins.add(new ShipSkin(3, "Solar", 1000, Color.rgb(255, 200, 50), false));
        skins.add(new ShipSkin(4, "Phantom", 1500, Color.rgb(80, 0, 120), false));
        skins.add(new ShipSkin(5, "Neon", 1000, Color.rgb(0, 255, 150), false));
        skins.add(new ShipSkin(6, "Galaxy", 3000, 0, true)); // Rainbow cycling
        return skins;
    }

    public static ShipSkin getSkinById(int id) {
        for (ShipSkin skin : getAllSkins()) {
            if (skin.id == id) return skin;
        }
        return getAllSkins().get(0); // Default
    }
}
