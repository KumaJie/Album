package hust.album.jni;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Index {
    private String root;
    private boolean train;

    public Index(String root, boolean train) {
        this.root = root;
        this.train = train;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public boolean isTrain() {
        return train;
    }

    public void setTrain(boolean train) {
        this.train = train;
    }

    static {
        System.loadLibrary("index");
    }

    public native void init();

    public native List<List<Integer>> match(List<float[]> data, int d, float radius);


}
