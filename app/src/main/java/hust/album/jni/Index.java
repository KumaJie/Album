package hust.album.jni;

public class Index {
    static {
        System.loadLibrary("index");
    }

    public native int[] match(float[] data, int d, int k);


}
