package com.android.settingslib.display;

/**
 * Konversi ruang gamma (slider) <-> linear untuk kecerahan, memakai kurva HLG
 * seperti SettingsLib AOSP.
 */
public class BrightnessUtils {
    public static final int GAMMA_SPACE_MIN = 0;
    public static final int GAMMA_SPACE_MAX = 65535;

    private static final float R = 0.5f;
    private static final float A = 0.17883277f;
    private static final float B = 0.28466892f;
    private static final float C = 0.55991073f;

    private BrightnessUtils() {}

    public static float convertGammaToLinearFloat(int val, float min, float max) {
        final float normalizedVal = (val - GAMMA_SPACE_MIN) / (float) (GAMMA_SPACE_MAX - GAMMA_SPACE_MIN);
        final float ret;
        if (normalizedVal <= R) {
            final float t = normalizedVal / R;
            ret = t * t;
        } else {
            ret = (float) Math.exp((normalizedVal - C) / A) + B;
        }
        final float normalizedRet = Math.max(0f, Math.min(12f, ret));
        return min + (max - min) * (normalizedRet / 12f);
    }

    public static int convertLinearToGammaFloat(float val, float min, float max) {
        final float range = max - min;
        final float normalizedVal = (range == 0f ? 0f : (val - min) / range) * 12f;
        final float ret;
        if (normalizedVal <= 1f) {
            ret = (float) Math.sqrt(normalizedVal) * R;
        } else {
            ret = A * (float) Math.log(normalizedVal - B) + C;
        }
        return Math.round(GAMMA_SPACE_MIN + (GAMMA_SPACE_MAX - GAMMA_SPACE_MIN) * ret);
    }
}
