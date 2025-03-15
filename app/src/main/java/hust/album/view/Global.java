package hust.album.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;

import hust.album.entity.Image;

public class Global extends Application {
    private List<Image> images;

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


    @Override
    public void onCreate() {
        super.onCreate();
        images = new ArrayList<>();
        instance = this;
    }

}
