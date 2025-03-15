package hust.album;


import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hust.album.adapter.ItemDecoration;
import hust.album.adapter.ListRecyclerViewAdapter;
import hust.album.adapter.RAdapter;
import hust.album.adapter.RecyclerViewAdapter;
import hust.album.entity.Image;
import hust.album.entity.Item;
import hust.album.entity.ListItem;
import hust.album.dbscan.DBSCANClusterer;
import hust.album.dbscan.metric.DistanceMetricImage;
import hust.album.jni.FeatureExtractor;
import hust.album.jni.Index;
import hust.album.view.Global;

public class MainActivity extends AppCompatActivity {

    private final FeatureExtractor fe = new FeatureExtractor();

    private final List<float[]> featrues = new ArrayList<>();


    private RecyclerView rv = null;

    private void checkAndRequestPermissions() {
//       访问照片权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                }, 1);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                // 如果权限未开启，请求权限
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                }, 1);
            }
        }
//        访问照片位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
            }, 1);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
        // 初始化 JNI
        fe.Init(getAssets());
//        初始化 opencv
        OpenCVLoader.initLocal();

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

//        wrapper = findViewById(R.id.wrapper);


        getAlbumPhotos(this);

        rv = findViewById(R.id.main_recycler_view);

        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));


        int space = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        rv.addItemDecoration(new ItemDecoration(space));

//        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
//        rv.setLayoutManager(layoutManager);
//
//        List<ListItem> items = new ArrayList<>();
//        items.add(new ListItem("All", images));
//        rv.setAdapter(new ListRecyclerViewAdapter(images, this));


//        GridView gv = findViewById(R.id.sssss);
//        gv.setNumColumns(4);
//        gv.setAdapter(new CommonAdapter<>(images, R.layout.item_small_image) {
//            @Override
//            public void bindView(ViewHolder holder, Image obj) {
//                holder.setImageByURI(R.id.small_image_view, obj.getUri());
//            }
//
//        });

//         按时间排序
        sortByTime();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.origin) {
            sortByTime();
            return true;
        } else if (item.getItemId() == R.id.time) {
            sortByDBSCAN();
            return true;
        } else if (item.getItemId() == R.id.deep) {
            sortByFeature();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sortByTime() {
        Map<String, List<Integer>> mp = new HashMap<>();
        for (int i = 0; i < Global.getInstance().getSize(); i++) {
            String date = Global.getInstance().getImagesByPos(i).getDate("yyyy-MM-dd");
            if (mp.containsKey(date)) {
                mp.get(date).add(i);
            } else {
                ArrayList<Integer> temp = new ArrayList<>();
                temp.add(i);
                mp.put(date, temp);
            }
        }

        ArrayList<String> keys = new ArrayList<>(mp.keySet());
        keys.sort((o1, o2) -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date d1 = dateFormat.parse(o1);
                Date d2 = dateFormat.parse(o2);
                return d2.compareTo(d1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        List<Item> items = new ArrayList<>();

        for (String key : keys) {
            items.add(new Item(Item.ITEM_TITLE, key, null));

            int len = mp.get(key).size();
            for (int i = 0; i < len; i += 4) {
                if (i + 4 <= len) {
                    items.add(new Item(Item.ITEM_IMG, null, mp.get(key).subList(i, i + 4)));
                } else {
                    items.add(new Item(Item.ITEM_IMG, null, mp.get(key).subList(i, len)));
                }
            }
        }

        rv.setAdapter(new RAdapter(this, items));


//        lv.setAdapter(new CommonAdapter<>(items, R.layout.item_list_view) {
//            @Override
//            public void bindView(ViewHolder holder, ListItem obj) {
//                holder.setText(R.id.list_title, obj.getTitle());
//                Log.d("bindView", obj.getTitle() + " " + obj.getImages().size());
//                GridView gv = holder.getView(R.id.list_grid_view);
//                gv.setNumColumns(4);
//                gv.setHorizontalSpacing(5);
//                gv.setVerticalSpacing(5);
//                gv.setAdapter(new CommonAdapter<>(obj.getImages(), R.layout.item_small_image) {
//                    @Override
//                    public void bindView(ViewHolder holder, Image obj) {
//                        holder.setImageByURI(R.id.small_image_view, obj.getUri());
//                    }
//                });
//            }
//        });


//        for (String key : keys) {
//            TextView tv = new TextView(this);
//            tv.setText(key);
//            wrapper.addView(tv);
//
//            GridLayout gl = new GridLayout(this);
//            gl.setColumnCount(4);
//            for (int i : mp.get(key)) {
//                Image obj = images.get(i);
//                ImageView iv = new ImageView(this);
//                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//                params.setMargins(1, 1,1, 1);
//                iv.setLayoutParams(params);
//
//
//                iv.setOnClickListener(v -> {
//                    showFullImage(images, i);
//                });
//                Glide.with(this)
//                        .load(obj.getUri())
//                        .downsample(DownsampleStrategy.AT_MOST)
//                        .override(224)
//                        .centerCrop()
//                        .into(iv);
//                gl.addView(iv);
//
//            }
//            wrapper.addView(gl);
//        }
    }

    public void sortByDBSCAN() {

        List<ArrayList<Integer>> res = new ArrayList<>();

        try {
            DBSCANClusterer<Image> clusterer = new DBSCANClusterer<>(Global.getInstance().getImages(), 2, 200, new DistanceMetricImage());
            long s = System.currentTimeMillis();
            res = clusterer.performClustering();
            long elapse = System.currentTimeMillis() - s;
            Log.d("DBSCAN", "spend" + elapse + " ms");
            Log.d("DBSCAN", "find " + res.size() + " cluster");

        } catch (Exception e) {
            Log.e("DBSCAN", e.getMessage());
        }

        List<Item> items = new ArrayList<>();
        for (ArrayList<Integer> cluster : res) {
            if (cluster.size() < 2) {
                continue;
            }
            items.add(new Item(Item.ITEM_TITLE, "强关联图片", null));

            for (int i = 0; i < cluster.size(); i += 4) {
                if (i + 4 <= cluster.size()) {
                    items.add(new Item(Item.ITEM_IMG, null, cluster.subList(i, i + 4)));
                } else {
                    items.add(new Item(Item.ITEM_IMG, null, cluster.subList(i, cluster.size())));
                }
            }
            RecyclerView.Adapter adapter = rv.getAdapter();
            if (adapter instanceof RAdapter) {
                ((RAdapter) adapter).replace(items);
            }
        }
    }

    public void sortByFeature() {
//        Mat trainData = new Mat(featrues.size(), featrues.get(0).length, CvType.CV_32F);
//        for (int i = 0; i < featrues.size(); i++) {
//            trainData.put(i, 0, featrues.get(i));
//        }
//        Mat t = trainData.clone().t();
//        Mat dist = new Mat();
//        long l = System.currentTimeMillis();
//        Core.gemm(trainData, t, 1, new Mat(), 0, dist);
//        Log.d("Feature", "spend " + (System.currentTimeMillis() - l) + " ms");
//        Set<Integer> flag = new HashSet<>();
//
//        for (int i = 0; i < images.size(); i++) {
//            if (flag.contains(i)) {
//                continue;
//            }
//
//            List<Image> clusterImage = new ArrayList<>();
//            for (int j = 0; j < images.size(); j++) {
//                if (flag.contains(j) || dist.get(i, j)[0] < 0.75) {
//                    continue;
//                }
//                clusterImage.add(images.get(j));
//                flag.add(j);
//            }
//            if (clusterImage.size() < 2) {
//                continue;
//            }
//
//            TextView tv = new TextView(this);
//            tv.setText(R.string.item_name);
//            tv.setTextSize(20);
//            tv.setBackgroundColor(getResources().getColor(R.color.item_background));
//            wrapper.addView(tv);
//
//            GridLayout gl = new GridLayout(this);
//            gl.setColumnCount(4);
//
//            for (int j = 0; j < clusterImage.size(); j++) {
//                Image obj = clusterImage.get(j);
//                ImageView iv = new ImageView(this);
//                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//                params.setMargins(1, 1, 1, 1);
//                iv.setLayoutParams(params);
//
//                int finalJ = j;
//                iv.setOnClickListener(v -> {
//                    showFullImage(clusterImage, finalJ);
//                });
//                Glide.with(this)
//                        .load(obj.getUri())
//                        .thumbnail(Glide
//                                .with(this)
//                                .load(obj.getUri()))
//                        .override(224)
//                        .centerCrop()
//                        .into(iv);
//                gl.addView(iv);
//            }
//            flag.add(i);
//            wrapper.addView(gl);
//        }

//        int d = featrues.get(0).length;
//        float[] data = new float[d * featrues.size()];
//        for (int i = 0; i < featrues.size(); i++) {
//            System.arraycopy(featrues.get(i), 0, data, i * d, d);
//        }
//
//        Index index = new Index();
//        int k = 5;
//        int[] labels = index.match(data, d, k);
//
//        for (int i = 0; i < images.size(); i++) {
//
//            TextView tv = new TextView(this);
//            tv.setText(R.string.item_name);
//            tv.setTextSize(20);
//            tv.setBackgroundColor(getResources().getColor(R.color.item_background));
//            wrapper.addView(tv);
//
//            GridLayout gl = new GridLayout(this);
//            gl.setColumnCount(4);
//
//            for (int j = 0; j < k; j++) {
//                int pos = labels[i * k + j];
//                Image obj = images.get(pos);
//
//                ImageView iv = new ImageView(this);
//                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//                params.setMargins(1, 1, 1, 1);
//                iv.setLayoutParams(params);
//
//                Glide.with(this)
//                        .load(obj.getUri())
//                        .thumbnail(Glide
//                                .with(this)
//                                .load(obj.getUri()))
//                        .override(224)
//                        .centerCrop()
//                        .into(iv);
//                gl.addView(iv);
//            }
//
//            wrapper.addView(gl);
//        }
    }

    public void getAlbumPhotos(Context context) {
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
        };
        long start = System.currentTimeMillis();
        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                null,
                null,
                null)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long time = cursor.getLong(dateColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    contentUri = MediaStore.setRequireOriginal(contentUri);
                    InputStream is = context.getContentResolver().openInputStream(contentUri);
                    double latitude = 0;
                    double longitude = 0;
                    long phash = 0;
                    if (is != null) {
                        ExifInterface exifInterface = new ExifInterface(is);
                        double[] latlong = exifInterface.getLatLong();
                        if (latlong != null) {
                            latitude = latlong[0];
                            longitude = latlong[1];
                        }
                        Bitmap bm = exifInterface.getThumbnailBitmap();
                        if (bm != null) {
                            Log.i("EXIF", "Thumbnail exists" + bm.getWidth() + " " + bm.getHeight());

//                              预处理
                            Mat image = new Mat();
                            Utils.bitmapToMat(bm, image);
                            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2GRAY);
                            Imgproc.resize(image, image, new Size(32, 32));
                            image.convertTo(image, CvType.CV_32F);

                            Mat dctMat = new Mat();
                            Core.dct(image, dctMat);
                            Mat part = dctMat.rowRange(1, 9).colRange(1, 9);
                            Scalar mean = Core.mean(part);
                            int shift = 63;
                            for (int i = 0; i < 8; i++) {
                                for (int j = 0; j < 8; j++) {
                                    long code = part.get(i, j)[0] > mean.val[0] ? 1 : 0;
                                    phash |= (code << shift);
                                    shift--;
                                }
                            }

//                            featrues.add(fe.ExtractFeature(bm));
                        }

                    }
                    Global.getInstance().addImage(new Image(contentUri, name, time, latitude, longitude, phash));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.d("Load", "spend " + (System.currentTimeMillis() - start) + " ms");
    }

//    private void showFullImage(List<Image> images, int pos) {
//        Intent intent = new Intent(this, FullImageActivity.class);
//        intent.putExtra("images", (Serializable) images);
//        intent.putExtra("position", pos);
//        startActivity(intent);
//    }
}
