package hust.album;

import android.os.Bundle;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

import hust.album.adapter.ImageAdapter;
import hust.album.adapter.ViewPagerItemDecoration;
import hust.album.entity.Image;

public class FullImageActivity extends AppCompatActivity {

    private List<Image> images;
    private int pos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        Toolbar tb = findViewById(R.id.toolbar2);
        tb.setNavigationOnClickListener(v -> {
            finish();
        });

        ViewPager2 viewPager = findViewById(R.id.view_pager);

        pos = getIntent().getIntExtra("position", 0);
        images = (List<Image>)getIntent().getSerializableExtra("images");

        tb.setTitle(images.get(pos).getName());


        ImageAdapter adapter = new ImageAdapter(this, images);

        // 初始化 ItemDecoration
        int space = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        ViewPagerItemDecoration itemDecoration = new ViewPagerItemDecoration(space);
        viewPager.addItemDecoration(itemDecoration);
        // 添加回调监听滑动状态
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                pos = position;
                tb.setTitle(images.get(position).getName());
                tb.setSubtitle(images.get(position).getDate("yyyy-MM-dd HH:mm:ss"));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    for (int i = 0; i < viewPager.getItemDecorationCount(); i++) {
                        if (viewPager.getItemDecorationAt(i).equals(itemDecoration)) {
                            return;
                        }
                    }
                    // 滑动时添加间距
                    viewPager.addItemDecoration(itemDecoration);
                }
                // 静止时移除间距
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    for (int i = 0; i < viewPager.getItemDecorationCount(); i++) {
                        if (viewPager.getItemDecorationAt(i).equals(itemDecoration)) {
                            viewPager.removeItemDecoration(itemDecoration);
                            return;
                        }
                    }
                }
            }

        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(pos, false);

    }

}
