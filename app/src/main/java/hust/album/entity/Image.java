package hust.album.entity;

import android.net.Uri;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Image implements Serializable {
    private  String uri;
    private  String name;

    private  long date;

    private  double latitude;
    private  double longitude;

    private long phash;

    @Override
    public String toString() {
        return "Image{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", phash=" + Long.toHexString(phash) +
                '}';
    }

    public long getPhash() {
        return phash;
    }

    public void setPhash(long phash) {
        this.phash = phash;
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
        DateFormat df = new SimpleDateFormat(format);
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

    public Image(Uri uri, String name, long time, double latitude, double longitude, long phash) {
        this.uri = uri.toString();
        this.name = name;
        this.date = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phash = phash;
    }

}
