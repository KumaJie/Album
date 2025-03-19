package hust.album;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import hust.album.ffmpeg.FFmpegHandler;
import hust.album.ffmpeg.FFmpegProc;
import hust.album.view.Global;
import hust.album.adapter.ImageAdapter;
import hust.album.entity.Image;

public class FullImageActivity extends AppCompatActivity {

    private List<Image> images;
    private int pos;

    private TextView tv;

    private FloatingActionsMenu actionsMenu;

    private FloatingActionButton downloadButton;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        Toolbar tb = findViewById(R.id.toolbar2);
        tb.setNavigationOnClickListener(v -> {
            finish();
        });

        tv = findViewById(R.id.detail);
        actionsMenu = findViewById(R.id.multiple_actions);
        downloadButton = findViewById(R.id.download_action);

        ViewPager2 viewPager = findViewById(R.id.view_pager);

        pos = getIntent().getIntExtra("position", 0);
        List<Integer> mask = (List<Integer>) getIntent().getSerializableExtra("images");

        images = new ArrayList<>();

        for (int m : mask) {
            images.add(Global.getInstance().getImagesByPos(m));
        }

        tb.setTitle(images.get(pos).getName());


        ImageAdapter adapter = new ImageAdapter(this, images);

        FFmpegProc ffmpegProc = new FFmpegProc(this);

        // 添加回调监听滑动状态
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                pos = position;
                Image image = images.get(position);

                tb.setTitle(image.getName());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                tb.setSubtitle(image.getDate(formatter));
                tv.setText(image.toString());

                actionsMenu.setEnabled(image.isCompressed());

                downloadButton.setOnClickListener(v -> {
                     ffmpegProc.extractPhoto(image, new FFmpegHandler() {
                        @Override
                        protected void handle() {
                            handler.post(() -> {
                                Toast.makeText(FullImageActivity.this, "图片已保存", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });

            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(pos, false);

    }

}
