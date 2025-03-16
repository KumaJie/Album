package hust.album;


import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hust.album.adapter.ItemDecoration;
import hust.album.adapter.RAdapter;
import hust.album.dbscan.DBSCANClusterer;
import hust.album.dbscan.metric.DistanceMetricImage;
import hust.album.entity.Image;
import hust.album.entity.Item;
import hust.album.jni.FeatureExtractor;
import hust.album.jni.Index;
import hust.album.view.Global;

public class MainActivity extends AppCompatActivity {

    private final FeatureExtractor fe = new FeatureExtractor();

    private final List<float[]> featrues = new ArrayList<>();


    private RecyclerView rv = null;

    private TextRoundCornerProgressBar progressBar = null;

    private final Handler handler = new Handler(Looper.getMainLooper());

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
        progressBar = findViewById(R.id.progress_bar);

        getAlbumPhotos(this);

        rv = findViewById(R.id.main_recycler_view);

        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));


        int space = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        rv.addItemDecoration(new ItemDecoration(space));

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
            new Thread(() -> {
                sortByDBSCAN();
            }).start();
            return true;
        } else if (item.getItemId() == R.id.deep) {
            new Thread(() -> {
                sortByFeature();
            }).start();
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
    }

    public void sortByDBSCAN() {
        if (!Global.getInstance().isGPSInfo()) {
            getGPSInfo();
        }
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
        }
        handler.post(() -> rv.setAdapter(new RAdapter(this, items)));

    }

    public void sortByFeature() {
        if (featrues.isEmpty()) {
            getFeature();
        }


        int d = featrues.get(0).length;
        float[] data = new float[d * featrues.size()];
        for (int i = 0; i < featrues.size(); i++) {
            System.arraycopy(featrues.get(i), 0, data, i * d, d);
        }

        Index index = new Index();
        int k = 5;
        int[] labels = index.match(data, d, k);
        if (labels.length != k * featrues.size()) {
            throw new RuntimeException("match error");
        }

        List<Integer> pos = new ArrayList<>();
        for (int label : labels) {
            pos.add(label);
        }

        List<Image> images = Global.getInstance().getImages();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            items.add(new Item(Item.ITEM_TITLE, "相似图片", null));
            items.add(new Item(Item.ITEM_IMG, null, pos.subList(i * k, (i + 1) * k)));
        }
        handler.post(() -> rv.setAdapter(new RAdapter(this, items)));
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

                    Global.getInstance().addImage(new Image(contentUri, name, time, 0, 0, 0));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.d("getAlbumPhotos", "spend " + (System.currentTimeMillis() - start) + " ms");
    }

    private void getGPSInfo() {
        handler.post(() -> {
            progressBar.disableAnimation();
            progressBar.setProgress(0);
            progressBar.setProgressText("正在获取图片时空信息");
            progressBar.enableAnimation();
            progressBar.setVisibility(View.VISIBLE);
        });

        long start = System.currentTimeMillis();
        List<Image> images = Global.getInstance().getImages();
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            try {
                InputStream is = getContentResolver().openInputStream(Uri.parse(image.getUri()));
                if (is != null) {
                    ExifInterface exifInterface = new ExifInterface(is);
                    double[] latlong = exifInterface.getLatLong();
                    if (latlong != null) {
                        image.setLatitude(latlong[0]);
                        image.setLongitude(latlong[1]);
                    }

                    Bitmap thumbnail = exifInterface.getThumbnailBitmap();
                    if (thumbnail != null) {
                        image.setPhash(phash(thumbnail));
                    }
                    is.close();
                }
            } catch (Exception e) {
                Log.e("EXIF", e.getMessage());
            }
            int finalI = i;
            handler.post(() -> progressBar.setProgress((float) finalI / images.size() * 100));
        }
        handler.post(() ->progressBar.setVisibility(View.GONE));
        Global.getInstance().setGPSInfo(true);
        Log.d("getGPSInfo", "spend " + (System.currentTimeMillis() - start) + " ms");
    }

    private long phash(Bitmap bm) {
        long phash = 0;
        // 预处理
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
        return phash;
    }

    private void getFeature() {
        handler.post(() -> {
            progressBar.disableAnimation();
            progressBar.setProgress(0);
            progressBar.setProgressText("正在提取图片特征");
            progressBar.enableAnimation();
            progressBar.setVisibility(View.VISIBLE);
        });
        long start = System.currentTimeMillis();
        List<Image> images = Global.getInstance().getImages();
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            try {
                InputStream is = getContentResolver().openInputStream(Uri.parse(image.getUri()));
                if (is != null) {
                    ExifInterface exifInterface = new ExifInterface(is);
                    Bitmap thumbnail = exifInterface.getThumbnailBitmap();
                    if (thumbnail != null) {
                        float[] feature = fe.ExtractFeature(thumbnail);
                        featrues.add(feature);
                    }
                    is.close();
                }
            } catch (Exception e) {
                Log.e("getFeature", e.getMessage());
            }
            int finalI = i;
            handler.post(() -> progressBar.setProgress((float) finalI / images.size() * 100));
        }
        handler.post(() ->progressBar.setVisibility(View.GONE));

        Log.d("getFeature", "spend " + (System.currentTimeMillis() - start) + " ms");
    }

}
