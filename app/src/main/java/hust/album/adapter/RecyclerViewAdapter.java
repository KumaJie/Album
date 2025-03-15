package hust.album.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hust.album.R;
import hust.album.entity.ListItem;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private List<ListItem> data;
    private Context context;

    public RecyclerViewAdapter(List<ListItem> data, Context context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListItem itemData = data.get(position);
        Log.d("RecyclerViewAdapter", "onBindViewHolder: " + itemData.getImages().size());
        holder.title.setText(itemData.getTitle());
        GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
        holder.view.setLayoutManager(layoutManager);
        holder.view.setAdapter(new ListRecyclerViewAdapter(itemData.getImages(), context));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private RecyclerView view;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.list_title);
            view = itemView.findViewById(R.id.list_recycler_view);
        }
    }
}
