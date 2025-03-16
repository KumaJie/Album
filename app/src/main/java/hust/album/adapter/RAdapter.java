package hust.album.adapter;


import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;


import java.util.ArrayList;
import java.util.List;

import hust.album.FullImageActivity;
import hust.album.R;
import hust.album.entity.Item;
import hust.album.view.Global;

public class RAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;

    private List<Item> data;

    public RAdapter(Context context, List<Item> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType) {
            case Item.ITEM_TITLE: {
                TextView tv = new TextView(context);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                int space = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
                params.setMargins(space, 0, 0, 0);
                tv.setLayoutParams(params);
                return new TitleViewHolder(tv);
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
                            intent.putExtra("images", new ArrayList<>(item.getImages()));
                            intent.putExtra("position", finalI);
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

    public class TitleViewHolder extends RecyclerView.ViewHolder
    {
        private TextView tv;

        public TitleViewHolder(TextView tv) {
            super(tv);

            this.tv = tv;
        }
    }



    public class ImgViewHolder extends RecyclerView.ViewHolder
    {
        private final ImageView[] img = new ImageView[4];

        public ImgViewHolder(View view, int allWidth) {
            super(view);

            int[] imgRes = {R.id.item_4_image1, R.id.item_4_image2, R.id.item_4_image3, R.id.item_4_image4};
            for (int i =0 ; i < img.length; i++) {
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
