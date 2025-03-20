package hust.album.ffmpeg;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;
import android.util.Size;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hust.album.entity.Image;
import hust.album.view.Global;

public class FFmpegProc {
    private String codec;
    private int qp;
    Context context;

    private FFmpegProc() {
        this.codec = "libx265";
        this.qp = 22;
    }
    public FFmpegProc(Context context) {
        this();
        this.context = context;
    }

    public void compressAlbum(List<Integer> images, FFmpegHandler handler) {
        if (images.size() == 0) {
            return;
        }
//        创建 list.txt，存放压缩的图片序列
        File listFile = new File(context.getExternalFilesDir(null), "list.txt");
        try {
            BufferedWriter writer =  new BufferedWriter(new FileWriter(listFile, false));
            for (int i : images) {
                String path = Global.getInstance().getImagesByPos(i).getAbsolutePath();
                writer.write(String.format("file '%s'\n", path));
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        String listPath = listFile.getAbsolutePath();
        String outputPath = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), generateFileName()).getAbsolutePath();

        int frame = 0;
        for (int i : images) {
            Image image= Global.getInstance().getImagesByPos(i);

            try {
                Bitmap bm = ThumbnailUtils.createImageThumbnail(new File(image.getAbsolutePath()), new Size(1280, 720), null);
                File thumbnailPath = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),  image.getName());
                FileOutputStream fos = new FileOutputStream(thumbnailPath);
                bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                image.setCompressed(true);
                image.setThumbnailPath(thumbnailPath.getAbsolutePath());
                image.setFramePos(frame++);
                image.setVideoPath(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        String cmd = String.format(Locale.getDefault(),"-f concat -safe 0 -i %s -pix_fmt yuvj422p -c:v %s -bf 0 -x265-params qp=%d -y %s", listPath, codec, qp, outputPath);
        FFmpegKit.executeAsync(cmd, session -> {
            if (!ReturnCode.isSuccess(session.getReturnCode())) {
                Log.e("FFmpegProc", "compressAlbum: failed with state %s" + session.getState());
            } else {
                handler.handle(null);
            }
        });

    }

    /**
     * 根据时间生成文件名
     * @return .mp4 文件名
     */
    private String generateFileName() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String prefix = df.format(new Date());
        return "VID_" + prefix + ".mp4";
    }

    public void extractPhoto(Image image, FFmpegHandler handler) {
        if (!image.isCompressed()) {
            Log.d("FFmpegProc", "extractPhoto: image is not compressed");
            return;
        }

        String path = image.getVideoPath();
        String outputPath = context.getExternalFilesDir("tmp").getAbsolutePath() + "/tmp.jpg";
        String cmd = String.format(Locale.getDefault(), "-i %s -q:v 2 -vf 'select=eq(n\\,%d)' -y %s", path, image.getFramePos(), outputPath);
        Log.d("FFmpegProc", "extractPhoto: " + cmd);
        FFmpegKit.executeAsync(cmd, session -> {
            if (!ReturnCode.isSuccess(session.getReturnCode())) {
                Log.e("FFmpegProc", "extractPhote: failed with state %s" + session.getState());
            } else {
                handler.handle(outputPath);
            }
        });
    }
}
