package com.zangetsu.client;

public class CameraShakeHandler {
    public static int shakeTicks = 0;
    public static int maxShakeTicks = 1;
    public static float intensity = 0.0f;

    public static void addShake(int ticks, float shakeIntensity) {
        if (shakeTicks <= 0 || shakeIntensity > intensity) {
            shakeTicks = ticks;
            maxShakeTicks = ticks;
            intensity = shakeIntensity;
        }
    }

    public static void tick() {
        if (shakeTicks > 0) {
            shakeTicks--;
        }
    }
}
