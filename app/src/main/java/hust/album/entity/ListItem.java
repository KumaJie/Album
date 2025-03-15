package hust.album.entity;

import java.util.List;

public class ListItem {
    private String title;
    private List<Image> images;


    public ListItem(String title, List<Image> images) {
        this.title = title;
        this.images = images;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Image> getImages() {
        return images;
    }
}
