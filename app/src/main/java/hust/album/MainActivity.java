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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

        progressBar = findViewById(R.id.progress_bar);

        if (!readStatus()) {
            getAlbumPhotos(this);
        }

        rv = findViewById(R.id.main_recycler_view);

        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));


        int space = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        rv.addItemDecoration(new ItemDecoration(space));

//         按时间排序
        sortByTime();
    }

    @Override
    protected void onPause() {
        saveStatus();
        super.onPause();
    }

    private void saveStatus() {
        File externalFilesDir = getExternalFilesDir(null);
        File statusFile = new File(externalFilesDir, "status.txt");
        if (statusFile.exists()) {
            statusFile.delete();
        }
        try {
            long start = System.currentTimeMillis();
            ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(statusFile));
            oo.writeObject(Global.getInstance().getImages());
            oo.writeObject(Global.getInstance().isGPSInfo());
            Log.d("Album", "save status success: " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean readStatus() {
        File externalFilesDir = getExternalFilesDir(null);
        File statusFile = new File(externalFilesDir, "status.txt");
        if (statusFile.exists()) {
            try {
                long start = System.currentTimeMillis();
                ObjectInputStream oi = new ObjectInputStream(new FileInputStream(statusFile));
                Object o = oi.readObject();
                if (o instanceof List) {
                    Global.getInstance().setImages((List<Image>) o);
                } else {
                    return false;
                }
                o = oi.readObject();
                if (o instanceof Boolean) {
                    Global.getInstance().setGPSInfo((Boolean) o);
                } else {
                    return false;
                }
                Log.d("Album", "load status success: " + (System.currentTimeMillis() - start) + " ms");
                return true;
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
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
        long start = System.currentTimeMillis();
        Map<String, List<Integer>> mp = new TreeMap<>();
        for (int i = 0; i < Global.getInstance().getSize(); i++) {
            String date = Global.getInstance().getImagesByPos(i).getDate("yyyy-MM-dd");
            if (!mp.containsKey(date)) {
                mp.put(date, new ArrayList<>());
            }
            mp.get(date).add(i);
        }

        Log.d("Album", "sortByTime spend " + (System.currentTimeMillis() - start) + " ms");

        List<Item> items = new ArrayList<>();
        List<String> keys = new ArrayList<>(mp.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) {
            String key = keys.get(i);
            List<Integer> cluster = mp.get(key);
            items.add(new Item(Item.ITEM_TITLE, key, cluster));
            for (int j = 0; j < cluster.size(); j += 4) {
                if (j + 4 <= cluster.size()) {
                    items.add(new Item(Item.ITEM_IMG, null, cluster.subList(j, j + 4)));
                } else {
                    items.add(new Item(Item.ITEM_IMG, null, cluster.subList(j, cluster.size())));
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
            Log.d("Album", "DBSCAN spend " + elapse + " ms, find " + res.size() + " cluster");

        } catch (Exception e) {
            Log.e("Album", "DBSCAN error: " + e.getMessage());
        }

        List<Item> items = new ArrayList<>();
        for (ArrayList<Integer> cluster : res) {
            if (cluster.size() < 2) {
                continue;
            }
            items.add(new Item(Item.ITEM_TITLE, "强关联图片", cluster));

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
            items.add(new Item(Item.ITEM_TITLE, "强关联图片", pos.subList(i * k, (i + 1) * k)));
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
                MediaStore.Images.Media.DATA
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
                int absolutePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long time = cursor.getLong(dateColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    contentUri = MediaStore.setRequireOriginal(contentUri);

                    String absolutePath = cursor.getString(absolutePathColumn);

                    Global.getInstance().addImage(new Image(contentUri, name, time, 0, 0, 0, absolutePath));
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
        handler.post(() -> progressBar.setVisibility(View.GONE));
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
        handler.post(() -> progressBar.setVisibility(View.GONE));

        Log.d("getFeature", "spend " + (System.currentTimeMillis() - start) + " ms");
    }

}
