package hust.album.ffmpeg;

import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import hust.album.view.Global;

public class FFmpegProc {
    private String codec;
    private int qp;
    private String root;

    public FFmpegProc() {
        this.codec = "libx265";
        this.qp = 22;
    }
    public FFmpegProc(String targetDir) {
        this();
        this.root = targetDir;
    }

    public void compressAlbum(List<Integer> images) {
//        创建 list.txt，存放压缩的图片序列
        File listFile = new File(root, "list.txt");
        if (listFile.exists()) {
            listFile.delete();
        }
        try {
            BufferedWriter writer =  new BufferedWriter(new FileWriter(listFile));
            for (int i : images) {
                String path = Global.getInstance().getImagesByPos(i).getAbsolutePath();
                writer.write(String.format("file '%s'\n", path));
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String listPath = listFile.getAbsolutePath();
        String outputPath = root + "/t2.mp4";

        String cmd = String.format(Locale.getDefault(),"-f concat -safe 0 -i %s -pix_fmt yuvj422p -c:v %s -bf 0 -x265-params qp=%d -y %s", listPath, codec, qp, outputPath);
        FFmpegKit.executeAsync(cmd, session -> {
            if (!ReturnCode.isSuccess(session.getReturnCode())) {
                Log.e("FFmpegProc", "compressAlbum: failed with state %s" + session.getState());
            }
        });
    }

    public void extractPhoto(int pos) {
        String path = root + "/t2.mp4";
        String outputPath = root + "/t2.jpg";
        String cmd = String.format(Locale.getDefault(), "-i %s -q:v 2 -vf 'select=eq(n\\,%d)' -y %s", path, pos, outputPath);
        FFmpegSession session = FFmpegKit.execute(cmd);
        if (!ReturnCode.isSuccess(session.getReturnCode())) {
            Log.e("FFmpegProc", "extractPhote: failed with state %s" + session.getState());
        }
    }

}
