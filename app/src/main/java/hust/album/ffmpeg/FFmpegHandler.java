package hust.album.ffmpeg;

public  abstract class FFmpegHandler {
    private long time = 0;
    protected abstract void handle(String msg);

    public void timeOn() {
        time = System.currentTimeMillis();
    }

    public long timeEnd() {
        return System.currentTimeMillis() - time;
    }
}
