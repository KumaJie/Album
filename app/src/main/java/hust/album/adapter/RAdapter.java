package hust.album.adapter;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import hust.album.FullImageActivity;
import hust.album.R;
import hust.album.entity.Item;
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

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
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

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = data.get(position);
        switch (item.getType()) {
            case Item.ITEM_TITLE: {
                TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
                titleViewHolder.tv.setText(item.getTitle());
                titleViewHolder.button.setOnClickListener(v -> {
                    FFmpegProc ffmpegProc = new FFmpegProc(context.getExternalFilesDir(null).getAbsolutePath());
                    ffmpegProc.compressAlbum(item.getImages());
                });
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
                        Glide.with(context).load(Global.getInstance().getImagesByPos(item.getImageByPos(i)).getUri()).override(224).into(imgViewHolder.img[i]);
                    } else {
                        imgViewHolder.img[i].setVisibility(View.INVISIBLE);
                    }
                }
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
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


    public class TitleViewHolder extends RecyclerView.ViewHolder {
        private TextView tv;
        private Button button;

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
}
