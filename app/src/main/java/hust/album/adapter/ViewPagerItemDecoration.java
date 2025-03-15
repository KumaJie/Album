package hust.album.adapter;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ViewPagerItemDecoration extends RecyclerView.ItemDecoration {
    private int spaceHeight;

    public ViewPagerItemDecoration(int spaceHeight) {
        this.spaceHeight = spaceHeight;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
        // 只在非第一个和最后一个项添加间距
        int position = parent.getChildAdapterPosition(view);
        if (position == 0 || position == parent.getAdapter().getItemCount() - 1) {
            outRect.left = 0;
            outRect.right = 0;
        } else {
            outRect.left = spaceHeight;
            outRect.right = spaceHeight;
        }
    }
}
