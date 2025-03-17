package hust.album;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;


import hust.album.view.Global;
import hust.album.adapter.ImageAdapter;
import hust.album.entity.Image;

public class FullImageActivity extends AppCompatActivity {

    private List<Image> images;
    private int pos;

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        Toolbar tb = findViewById(R.id.toolbar2);
        tb.setNavigationOnClickListener(v -> {
            finish();
        });

        tv = findViewById(R.id.detail);

        ViewPager2 viewPager = findViewById(R.id.view_pager);

        pos = getIntent().getIntExtra("position", 0);
        List<Integer> mask = (List<Integer>) getIntent().getSerializableExtra("images");

        images = new ArrayList<>();

        for (int m : mask) {
            images.add(Global.getInstance().getImagesByPos(m));
        }

        tb.setTitle(images.get(pos).getName());


        ImageAdapter adapter = new ImageAdapter(this, images);

        // 添加回调监听滑动状态
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                pos = position;
                tb.setTitle(images.get(position).getName());
                tb.setSubtitle(images.get(position).getDate("yyyy-MM-dd HH:mm:ss"));

                tv.setText(images.get(position).toString());

            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(pos, false);

    }

}
