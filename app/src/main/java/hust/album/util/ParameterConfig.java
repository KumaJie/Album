package hust.album.util;

import android.content.Context;

import androidx.preference.PreferenceManager;

public class ParameterConfig {
    public static boolean FORCE_CREATE_ALBUM;

    public static boolean FORCE_EXTRACT_EXIF;

    public static boolean FORCE_EXTRACT_FEATURES;

    public static boolean FORCE_TRAIN_INDEX;

    public static float SIMILARITY_THRESHOLD;

    public static void getDefaults(Context context) {
        FORCE_CREATE_ALBUM = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("force_create_album", false);
        FORCE_EXTRACT_EXIF = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("force_extract_exif", false);
        FORCE_EXTRACT_FEATURES = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("force_extract_features", false);
        FORCE_TRAIN_INDEX = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("force_train_index", false);
        String number = PreferenceManager.getDefaultSharedPreferences(context).getString("similarity_threshold", "0.9");
        try {
            SIMILARITY_THRESHOLD = Float.parseFloat(number);
        } catch (NumberFormatException | NullPointerException e) {
            SIMILARITY_THRESHOLD = 0.9f;
        }

    }

    public static String fmtString() {
        return "FORCE_CREATE_ALBUM: " + FORCE_CREATE_ALBUM + "\n" +
               "FORCE_EXTRACT_EXIF: " + FORCE_EXTRACT_EXIF + "\n" +
               "FORCE_EXTRACT_FEATURES: " + FORCE_EXTRACT_FEATURES + "\n" +
               "FORCE_TRAIN_INDEX: " + FORCE_TRAIN_INDEX + "\n" +
               "SIMILARITY_THRESHOLD: " + SIMILARITY_THRESHOLD + "\n";
    }
}
