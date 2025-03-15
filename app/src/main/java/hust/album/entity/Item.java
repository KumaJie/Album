package hust.album.entity;

import java.util.List;

public class Item {

    public final static int ITEM_TITLE = 0x1001;

    public final static int ITEM_IMG = 0x1002;

    private int type;

    private String title;

    private List<Integer> images;

    public Item(int type, String title, List<Integer> images) {
        this.type = type;
        this.title = title;
        this.images = images;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getImageByPos(int pos) {
        return images.get(pos);
    }

    public int getSize() {
        return images.size();
    }

    public void setImages(List<Integer> images) {
        this.images = images;
    }
}
