package hust.album;


import android.Manifest;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.preference.PreferenceManager;
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
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hust.album.adapter.ItemDecoration;
import hust.album.adapter.RAdapter;
import hust.album.dbscan.DBSCANClusterer;
import hust.album.dbscan.metric.DistanceMetricImage;
import hust.album.entity.Image;
import hust.album.entity.Item;
import hust.album.ffmpeg.FFmpegHandler;
import hust.album.ffmpeg.FFmpegProc;
import hust.album.jni.FeatureExtractor;
import hust.album.jni.Index;
import hust.album.util.ParameterConfig;
import hust.album.view.Global;

public class MainActivity extends AppCompatActivity {

    private final FeatureExtractor fe = new FeatureExtractor();

    private final List<float[]> featrues = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        switch (key) {
            case "force_create_album":
                ParameterConfig.FORCE_CREATE_ALBUM = sharedPreferences.getBoolean("force_create_album", ParameterConfig.FORCE_CREATE_ALBUM);
                if (ParameterConfig.FORCE_CREATE_ALBUM) {
                    getAlbumPhotos();
                }
                break;
            case "force_extract_exif":
                ParameterConfig.FORCE_EXTRACT_EXIF = sharedPreferences.getBoolean("force_extract_exif", ParameterConfig.FORCE_EXTRACT_EXIF);
                break;
            case "force_extract_features":
                ParameterConfig.FORCE_EXTRACT_FEATURES = sharedPreferences.getBoolean("force_extract_features", ParameterConfig.FORCE_EXTRACT_FEATURES);
                break;
            case "force_train_index":
                ParameterConfig.FORCE_TRAIN_INDEX = sharedPreferences.getBoolean("force_train_index", ParameterConfig.FORCE_TRAIN_INDEX);
                break;
            case "similarity_threshold":
                String number = sharedPreferences.getString("similarity_threshold", "0.9");
                try {
                    ParameterConfig.SIMILARITY_THRESHOLD = Float.parseFloat(number);
                } catch (NumberFormatException | NullPointerException e) {
                    ParameterConfig.SIMILARITY_THRESHOLD = 0.9f;
                }
                break;
        }
        Log.d("Album", "config: " + ParameterConfig.fmtString());
    };
    private RecyclerView rv = null;
    private FrameLayout container = null;
    private TextRoundCornerProgressBar progressBar = null;

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
//        一定要在setSupportActionBar之后调用
        tb.setNavigationOnClickListener(v -> {
            back();
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        progressBar = findViewById(R.id.progress_bar);

        container = findViewById(R.id.settings_container);

        initConfig();
        if (ParameterConfig.FORCE_CREATE_ALBUM || !readStatus()) {
            getAlbumPhotos();
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
            new Thread(this::sortByDBSCAN).start();
            return true;
        } else if (item.getItemId() == R.id.deep) {
            new Thread(() -> {
                sortByFeature();
            }).start();
            return true;
        } else if (item.getItemId() == R.id.settings) {
            rv.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            return true;
        } else if (item.getItemId() == R.id.test) {
//            FFmpegProc proc = new FFmpegProc(this);
//            proc.test(3, new FFmpegHandler() {
//                @Override
//                protected void handle(String msg) {
//                    Log.d("FFmpegProc", "compressAlbum spend : " + timeEnd() + "ms");
//                }
//            });
            List<Image> images = Global.getInstance().getImages();

            long start = 0;
            String root = getExternalFilesDir("save").getAbsolutePath();
//            String[] pix = {"600", "1080", "1440", "4000"};
//            for (String p : pix) {
//                String path = root + "/" + p + "_1.jpg";
//                start = System.currentTimeMillis();
//                Bitmap bitmap = BitmapFactory.decodeFile(path);
//                Log.d("Album", "decode fullimage spend " + (System.currentTimeMillis() - start) + " ms height " + bitmap.getHeight() + " width " + bitmap.getWidth());
//                phash(bitmap);
//            }
            String[] type = {"xiaomi", "iphone", "iqoo"};
            for (String t : type) {
                String path = root + "/" + t + ".jpg";
                start = System.currentTimeMillis();
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                Log.d("Album", "decode fullimage spend " + (System.currentTimeMillis() - start) + " ms height " + bitmap.getHeight() + " width " + bitmap.getWidth());
//                phash(bitmap);
            }
            for (String t : type) {
                String path = root + "/" + t + ".jpg";
                ExifInterface exifInterface = null;
                try {
                    exifInterface = new ExifInterface(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                start = System.nanoTime();
                Bitmap thumbnailBitmap = exifInterface.getThumbnailBitmap();
                if (thumbnailBitmap != null) {
                    Log.d("Album", "decode thumimage spend " + (System.nanoTime() - start) + " ns height " + thumbnailBitmap.getHeight() + " width " + thumbnailBitmap.getWidth());
                }
            }


//            try {
//                for (int i = 0; i < 10; i++) {
//                    start = System.currentTimeMillis();
//                    Bitmap bitmap = BitmapFactory.decodeFile(images.get(i).getAbsolutePath());
//
//                    Log.d("Album", "decode fullimage spend " + (System.currentTimeMillis() - start) + " ms height " + bitmap.getHeight() + " width " + bitmap.getWidth());
//                }
//                start = System.currentTimeMillis();
//                for (int i = 0; i < 2000; i++) {
//                    ExifInterface exifInterface = new ExifInterface(images.get(i).getAbsolutePath());
//                    Bitmap thumbnailBitmap = exifInterface.getThumbnailBitmap();
//                    if (thumbnailBitmap != null) {
//                        phash(thumbnailBitmap);
//                    } else {
//                        Log.d("Album", "getThumbnailBitmap: " + images.get(i).getName() + " thumbnail is null");
//                    }
//                }
//                Log.d("Album", "getThumbnailBitmap spend " + (System.currentTimeMillis() - start) + " ms");
//                start = System.currentTimeMillis();
//                for (int i = 800; i < 900; i++) {
//                    is = getContentResolver().openInputStream(Uri.parse(images.get(i).getUri()));
//                    ExifInterface exifInterface = new ExifInterface(is);
//                    Bitmap thumbnailBitmap = exifInterface.getThumbnailBitmap();
//                    phash(thumbnailBitmap);
//                    is.close();
//                }
//                Log.d("Album", "stream getThumbnailBitmap spend " + (System.currentTimeMillis() - start) + " ms");


//            } catch (IOException e) {
//                    throw new RuntimeException(e);
//            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener);
        new Thread(this::saveStatus).start();
        super.onPause();
    }

    @Override
    protected void onResume() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
        super.onResume();
    }

    public void back() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            container.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            rv.setVisibility(View.VISIBLE);
        }
    }

    public void getAlbumPhotos() {
        Global.getInstance().getImages().clear();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATA
        };
        long start = System.currentTimeMillis();
        try (Cursor cursor = getContentResolver().query(
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
                    if (name.startsWith("Screenshot") || name.startsWith("ALB")) continue;
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
        Log.d("Album", "getAlbumPhotos num: "+ Global.getInstance().getSize() +" spend: " + (System.currentTimeMillis() - start) + " ms");
    }

    public void sortByDBSCAN() {
        if (ParameterConfig.FORCE_EXTRACT_EXIF || !Global.getInstance().isGPSInfo()) {
            getGPSInfo();
        }
        List<ArrayList<Integer>> res = new ArrayList<>();

        try {
            DBSCANClusterer<Image> clusterer = new DBSCANClusterer<>(Global.getInstance().getImages(), 2, 50, new DistanceMetricImage());
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
            items.add(new Item(Item.ITEM_TITLE, getString(R.string.cluster_item_name), cluster));

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
        if (ParameterConfig.FORCE_EXTRACT_FEATURES || (featrues.isEmpty() && !readFeatures())) {
            featrues.clear();
            getFeature();
            saveFeatures();
        }

        int d = featrues.get(0).length;

        Index index = new Index(getExternalCacheDir().getAbsolutePath(), ParameterConfig.FORCE_TRAIN_INDEX);
        index.init();
        List<List<Integer>> ret = index.match(featrues, d, ParameterConfig.SIMILARITY_THRESHOLD);
        Log.d("Album", "match result: " + ret.size());
        if (ret == null) {
            return;
        }

        List<Item> items = new ArrayList<>();
        for (List<Integer> cluster : ret) {
            items.add(new Item(Item.ITEM_TITLE, getString(R.string.item_name), cluster));
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

    public void sortByTime() {
        long start = System.currentTimeMillis();
        Map<String, List<Integer>> mp = new HashMap<>();
        List<Image> images = Global.getInstance().getImages();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        for (int i = 0; i < images.size(); i++) {
            String date = images.get(i).getDate(formatter);
            if (!mp.containsKey(date)) {
                mp.put(date, new ArrayList<>());
            }
            mp.get(date).add(i);
        }
        List<String> dates = new ArrayList<>(mp.keySet());
        dates.sort(Comparator.reverseOrder());

        Log.d("Album", "sortByTime spend " + (System.currentTimeMillis() - start) + " ms");

        List<Item> items = new ArrayList<>();

        for (String key : dates) {
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
//        long sum = 0;
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            try {
//                long tmp = System.currentTimeMillis();
                ExifInterface exifInterface = new ExifInterface(image.getAbsolutePath());
                Bitmap thumbnail = exifInterface.getThumbnailBitmap();
//                sum += System.currentTimeMillis() - tmp;
                if (thumbnail != null) {
                    float[] feature = fe.ExtractFeature(thumbnail);
                    featrues.add(feature);
                } else {
                    Log.d("Album", "getFeature: "+image.getName()+" thumbnail is null");
//                    long tmp = System.currentTimeMillis();
                    int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1);
                    int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1);

                    Bitmap bm = getContentResolver().loadThumbnail(Uri.parse(image.getUri()), new android.util.Size(width / 10, height / 10), null);
//                    Bitmap bm = BitmapFactory.decodeFile(image.getAbsolutePath());
//                    sum += System.currentTimeMillis() - tmp;

                    float[] feature = fe.ExtractFeature(bm);
                    featrues.add(feature);
                }

            } catch (Exception e) {
                Log.e("getFeature", e.getMessage());
            }
            int finalI = i;
            handler.post(() -> progressBar.setProgress((float) finalI / images.size() * 100));
        }
        handler.post(() -> progressBar.setVisibility(View.GONE));

//        Log.d("Album", "getFeature spend " + (System.currentTimeMillis() - start) + " ms image spend " + sum + " ms");
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
                ExifInterface  exifInterface = new ExifInterface(image.getAbsolutePath());
                double[] latlong = exifInterface.getLatLong();
                if (latlong != null) {
                    image.setLatitude(latlong[0]);
                    image.setLongitude(latlong[1]);
                } else {
                    Log.d("Album", "getGPSInfo: "+image.getName()+" latlong is null");
                }

//                Bitmap thumbnail = exifInterface.getThumbnailBitmap();
//                if (thumbnail != null) {
//                    image.setPhash(phash(thumbnail));
//                } else {
//                    Log.d("Album", "getGPSInfo: "+image.getName()+" thumbnail is null");
//                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int finalI = i;
            handler.post(() -> progressBar.setProgress((float) finalI / images.size() * 100));
        }
        handler.post(() -> progressBar.setVisibility(View.GONE));
        Global.getInstance().setGPSInfo(true);
        Log.d("Album", "getGPSInfo spend " + (System.currentTimeMillis() - start) + " ms");
    }

    private void initConfig() {
        //        初始化配置
        ParameterConfig.getDefaults(this);
    }

    private long phash(Bitmap bm) {
        long phash = 0;
        // 预处理
        Mat image = new Mat();
        Utils.bitmapToMat(bm, image);
        long start = System.currentTimeMillis();
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.resize(image, image, new Size(32, 32));
        image.convertTo(image, CvType.CV_32F);
        Log.d("Album", "phash pre-processing spend " + (System.currentTimeMillis() - start) + " ms");
        start  = System.currentTimeMillis();
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
        Log.d("Album", "phash spend " + (System.currentTimeMillis() - start) + " ms");
        return phash;
    }

    private boolean readFeatures() {
        File externalFilesDir = getExternalCacheDir();
        File featuresFile = new File(externalFilesDir, "features.txt");
        if (featuresFile.exists()) {
            try {
                long start = System.currentTimeMillis();
                ObjectInputStream oi = new ObjectInputStream(new FileInputStream(featuresFile));
                Object o = oi.readObject();
                if (o instanceof List) {
                    featrues.clear();
                    featrues.addAll((List<float[]>) o);
                } else {
                    return false;
                }
                Log.d("Album", "load features success, feature number " + featrues.size() + ", time " + (System.currentTimeMillis() - start) + " ms");
                return true;
            } catch (IOException | ClassNotFoundException e) {
                Log.e("Album", "load features error: " + e);
            }
        }
        return false;
    }

    private boolean readStatus() {
        File externalFilesDir = getExternalCacheDir();
        File statusFile = new File(externalFilesDir, "status.txt");
        if (statusFile.exists()) {
            try {
                long start = System.currentTimeMillis();
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(statusFile));
                Object o = ois.readObject();
                if (o instanceof List) {
                    Global.getInstance().setImages((List<Image>) o);
                } else {
                    return false;
                }
                Global.getInstance().setGPSInfo(ois.readBoolean());
                ois.close();
                Log.d("Album", String.format(Locale.getDefault(), "load status success, image number %d, GPSInfo %b, time %d ms",
                        Global.getInstance().getSize(), Global.getInstance().isGPSInfo(), System.currentTimeMillis() - start));
                return true;
            } catch (IOException | ClassNotFoundException e) {
                Log.e("Album", "load status error: " + e);
            }
        }
        return false;
    }

    private void saveFeatures() {
        File externalFilesDir = getExternalCacheDir();
        File statusFile = new File(externalFilesDir, "features.txt");
        try {
            long start = System.currentTimeMillis();
            ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(statusFile, false));
            oo.writeObject(featrues);
            Log.d("Album", "save features success: features number: " + featrues.size() + " time: " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveStatus() {
        File externalFilesDir = getExternalCacheDir();
        File statusFile = new File(externalFilesDir, "status.txt");
        try {
            long start = System.currentTimeMillis();
            ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(statusFile, false));
            oo.writeObject(Global.getInstance().getImages());
            oo.writeBoolean(Global.getInstance().isGPSInfo());
            oo.close();
            Log.d("Album", String.format(Locale.getDefault(), "save status success, image number %d, GPSInfo %b, time %d ms",
                    Global.getInstance().getSize(), Global.getInstance().isGPSInfo(), System.currentTimeMillis() - start));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
