package hust.album.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;

import hust.album.entity.Image;

public class Global extends Application {
    private List<Image> images;

    private List<Integer> selected;

    private boolean GPSInfo;

    public boolean isGPSInfo() {
        return GPSInfo;
    }

    public void setGPSInfo(boolean GPSInfo) {
        this.GPSInfo = GPSInfo;
    }

    private static Global instance;

    public static Global getInstance() {
        return instance;
    }

    public Image getImagesByPos(int pos) {
        return images.get(pos);
    }

    public void addImage(Image image) {
        images.add(image);
    }

    public int getSize() {
        return images.size();
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        images = new ArrayList<>();
        GPSInfo = false;
        instance = this;
    }

}
