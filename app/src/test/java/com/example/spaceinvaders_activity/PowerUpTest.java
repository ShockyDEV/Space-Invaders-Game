package com.example.spaceinvaders_activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link PowerUp} type/constant logic.
 *
 * These assertions deliberately avoid constructing a PowerUp (which touches
 * android.graphics.RectF) so they run on the plain host JVM via `./gradlew test`
 * without Robolectric. Rendering/collision behaviour is covered by gameplay.
 */
public class PowerUpTest {

    @Test
    public void powerUpTypes_areDistinct() {
        Set<Integer> types = new HashSet<>();
        types.add(PowerUp.HEALTH);
        types.add(PowerUp.RAPID_FIRE);
        types.add(PowerUp.SHIELD_BUBBLE);
        types.add(PowerUp.SCORE_BOOST);
        types.add(PowerUp.FREEZE);
        assertEquals(5, types.size());
    }

    @Test
    public void randomType_alwaysInRange() {
        for (int i = 0; i < 2000; i++) {
            int type = PowerUp.randomType();
            assertTrue("type out of range: " + type, type >= 0 && type <= 4);
        }
    }

    @Test
    public void constants_haveExpectedValues() {
        assertEquals(0.12f, PowerUp.DROP_CHANCE, 0.0001f);
        assertEquals(8000, PowerUp.BUFF_DURATION);
    }
}
