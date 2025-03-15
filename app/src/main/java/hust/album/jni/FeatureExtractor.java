package hust.album.jni;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class FeatureExtractor {
    static {
        System.loadLibrary("net");
    }

    public native boolean Init(AssetManager mgr);

    public native float[] ExtractFeature(Bitmap bitmap);

}
