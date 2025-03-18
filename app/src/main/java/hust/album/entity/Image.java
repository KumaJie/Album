package hust.album.entity;

import android.net.Uri;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Image implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uri;
    private String name;

    private long date;

    private double latitude;
    private double longitude;

    private long phash;

    private String absolutePath;


    private boolean compressed;

    private String videoPath;

    private int framePos;

    private String thumbnailPath;

    @Override
    public String toString() {
        return "Image{" +
                "uri='" + uri + '\'' +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", phash=" + Long.toHexString(phash) +
                ", absolutePath='" + absolutePath + '\'' +
                ", compressed=" + compressed +
                ", videoPath='" + videoPath + '\'' +
                ", framePos=" + framePos +
                ", thumbnailPath='" + thumbnailPath + '\'' +
                '}';
    }

    public Image(Uri uri, String name, long time, double latitude, double longitude, long phash, String absolutePath) {
        this.uri = uri.toString();
        this.name = name;
        this.date = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phash = phash;
        this.absolutePath = absolutePath;
    }


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public String getDate(String format) {
        DateFormat df = new SimpleDateFormat(format, Locale.getDefault());
        return df.format(new Date(date));
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getPhash() {
        return phash;
    }

    public void setPhash(long phash) {
        this.phash = phash;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getFramePos() {
        return framePos;
    }

    public void setFramePos(int framePos) {
        this.framePos = framePos;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}
