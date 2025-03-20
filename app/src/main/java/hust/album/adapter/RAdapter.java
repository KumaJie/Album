package hust.album.adapter;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dd.CircularProgressButton;

import java.util.ArrayList;
import java.util.List;

import hust.album.FullImageActivity;
import hust.album.R;
import hust.album.entity.Image;
import hust.album.entity.Item;
import hust.album.ffmpeg.FFmpegHandler;
import hust.album.ffmpeg.FFmpegProc;
import hust.album.view.Global;

public class RAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;

    private List<Item> data;

    private List<Integer> title;

    public RAdapter(Context context, List<Item> data) {
        this.context = context;
        this.data = data;
        this.title = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getType() == Item.ITEM_TITLE) {
                this.title.add(i);
            }
        }
    }

    public class TitleViewHolder extends RecyclerView.ViewHolder {
        private TextView tv;
        private CircularProgressButton button;

        public TitleViewHolder(View view) {
            super(view);

            this.tv = view.findViewById(R.id.item_title_textView);
            this.button = view.findViewById(R.id.item_title_button);
        }
    }

    public class ImgViewHolder extends RecyclerView.ViewHolder {
        private final ImageView[] img = new ImageView[4];

        public ImgViewHolder(View view, int allWidth) {
            super(view);

            int[] imgRes = {R.id.item_4_image1, R.id.item_4_image2, R.id.item_4_image3, R.id.item_4_image4};
            for (int i = 0; i < img.length; i++) {
                img[i] = view.findViewById(imgRes[i]);
                ViewGroup.LayoutParams params = img[i].getLayoutParams();
                int space = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 3, context.getResources().getDisplayMetrics());
                params.width = (allWidth - space) / 4;
                img[i].setLayoutParams(params);
            }

        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = data.get(position);
        switch (item.getType()) {
            case Item.ITEM_TITLE: {
                TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
                titleViewHolder.tv.setText(item.getTitle());
                if (item.getTitle().equals(context.getString(R.string.item_name)) && !Global.getInstance().getImagesByPos(item.getImageByPos(0)).isCompressed()) {
                    titleViewHolder.button.setVisibility(View.VISIBLE);
                    titleViewHolder.button.setIndeterminateProgressMode(true);
                    titleViewHolder.button.setOnClickListener(v -> {
                        titleViewHolder.button.setProgress(50);
                        FFmpegProc ffmpegProc = new FFmpegProc(context);
                        ffmpegProc.compressAlbum(item.getImages(), new FFmpegHandler() {
                            @Override
                            protected void handle(String msg) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    titleViewHolder.button.setProgress(100);
                                });
                            }
                        });
                    });
                } else {
                    titleViewHolder.button.setVisibility(View.GONE);
                }
                break;
            }
            case Item.ITEM_IMG: {
                ImgViewHolder imgViewHolder = (ImgViewHolder) holder;
                for (int i = 0; i < 4; i++) {
                    if (i < item.getSize()) {
                        imgViewHolder.img[i].setVisibility(View.VISIBLE);
                        int finalI = i;
                        imgViewHolder.img[i].setOnClickListener(v -> {
                            Intent intent = new Intent(context, FullImageActivity.class);
                            int titlePosition = getNearestTitlePosition(position);
                            int imagePosition = (position - titlePosition - 1) * 4 + finalI;
                            Log.d("RAdapter", "titlePosition: " + titlePosition + " imagePosition: " + imagePosition);
                            intent.putExtra("images", new ArrayList<>(data.get(titlePosition).getImages()));
                            intent.putExtra("position", imagePosition);
                            context.startActivity(intent);
                        });
                        Image image = Global.getInstance().getImagesByPos(item.getImageByPos(i));
                        if (image.isCompressed()) {
                            Glide.with(context).load(image.getThumbnailPath()).override(224).into(imgViewHolder.img[i]);
                        } else {
                            Glide.with(context).load(image.getUri()).override(224).into(imgViewHolder.img[i]);
                        }

                    } else {
                        imgViewHolder.img[i].setVisibility(View.INVISIBLE);
                    }
                }
                break;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType) {
            case Item.ITEM_TITLE: {
                View view = LayoutInflater.from(context).inflate(R.layout.item_title, parent, false);

                return new TitleViewHolder(view);
            }
            case Item.ITEM_IMG: {
                View view = LayoutInflater.from(context).inflate(R.layout.item_4_image, parent, false);

                return new ImgViewHolder(view, parent.getWidth());
            }
        }
        return null;
    }

    private int getNearestTitlePosition(int position) {
        if (data.get(position).getType() == Item.ITEM_TITLE) {
            return position;
        }
        int i = 0, j = title.size() - 1;
        while (i < j) {
            int mid = (i + j + 1) >> 1;
            if (title.get(mid) < position) {
                i = mid;
            } else {
                j = mid - 1;
            }
        }
        return title.get(i);
    }
}
