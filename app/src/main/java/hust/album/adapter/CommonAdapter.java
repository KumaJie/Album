package hust.album.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonAdapter<T> extends BaseAdapter {


        private List<T> data;
        private int layoutRes;           //布局id


        public CommonAdapter() {
        }

        public CommonAdapter(List<T> mData, int mLayoutRes) {
            this.data = mData;
            this.layoutRes = mLayoutRes;
        }

        @Override
        public int getCount() {
            return data != null ? data.size() : 0;
        }

        @Override
        public T getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = ViewHolder.bind(parent.getContext(), convertView, parent, layoutRes
                    , position);
            bindView(holder, getItem(position));
            return holder.getItemView();
        }

        public abstract void bindView(ViewHolder holder, T obj);

        //添加一个元素
        public void add(T t) {
            if (data == null) {
                data = new ArrayList<>();
            }
            data.add(t);
            notifyDataSetChanged();
        }

        //往特定位置，添加一个元素
        public void add(int position, T t) {
            if (data == null) {
                data = new ArrayList<>();
            }
            data.add(position, t);
            notifyDataSetChanged();
        }

        public void remove(T t) {
            if (data != null) {
                data.remove(t);
            }
            notifyDataSetChanged();
        }

        public void remove(int position) {
            if (data != null) {
                data.remove(position);
            }
            notifyDataSetChanged();
        }

        public void clear() {
            if (data != null) {
                data.clear();
            }
            notifyDataSetChanged();
        }


        public static class ViewHolder {

            private SparseArray<View> views;   //存储ListView 的 item中的View
            private View convertView;                  //存放convertView
            private int position;               //游标
            private Context context;            //Context上下文

            //构造方法，完成相关初始化
            private ViewHolder(Context context, ViewGroup parent, int layoutRes) {
                views = new SparseArray<>();
                this.context = context;
                convertView = LayoutInflater.from(context).inflate(layoutRes, parent, false);
                convertView.setTag(this);
            }

            //绑定ViewHolder与item
            public static ViewHolder bind(Context context, View convertView, ViewGroup parent,
                                          int layoutRes, int position) {
                ViewHolder holder;
                if (convertView == null) {
                    holder = new ViewHolder(context, parent, layoutRes);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                    holder.convertView = convertView;
                }
                holder.position = position;
                return holder;
            }

            @SuppressWarnings("unchecked")
            public <T extends View> T getView(int id) {
                T t = (T) views.get(id);
                if (t == null) {
                    t = convertView.findViewById(id);
                    views.put(id, t);
                }
                return t;
            }


            /**
             * 获取当前条目
             */
            public View getItemView() {
                return convertView;
            }

            /**
             * 获取条目位置
             */
            public int getItemPosition() {
                return position;
            }

            /**
             * 设置文字
             */
            public ViewHolder setText(int id, CharSequence text) {
                View view = getView(id);
                if (view instanceof TextView) {
                    ((TextView) view).setText(text);
                }
                return this;
            }


            public ViewHolder setImageByURI(int id, String uri) {
                View view = getView(id);
                if (view instanceof ImageView) {
                    Glide.with(context).load(uri).override(224).into((ImageView) view);
                }
                return this;
            }


            /**
             * 设置点击监听
             */
            public ViewHolder setOnClickListener(int id, View.OnClickListener listener) {
                getView(id).setOnClickListener(listener);
                return this;
            }

            /**
             * 设置可见
             */
            public ViewHolder setVisibility(int id, int visible) {
                getView(id).setVisibility(visible);
                return this;
            }

            /**
             * 设置标签
             */
            public ViewHolder setTag(int id, Object obj) {
                getView(id).setTag(obj);
                return this;
            }

            //其他方法可自行扩展

        }



}
