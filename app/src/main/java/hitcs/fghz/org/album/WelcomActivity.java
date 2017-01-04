package hitcs.fghz.org.album;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowImageClassifier;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import hitcs.fghz.org.album.dao.MyDatabaseHelper;
import static hitcs.fghz.org.album.utils.ImagesScaner.getMediaImageInfo;

/**
 * i plan use this activity as the first activity
 * when open the app, you can see this page
 * and the app can deal somethings at background
 * Created by me on 17-1-1.
 */

public class WelcomActivity extends AppCompatActivity {

    private TextView textView = null;
    private int i = 0;
    private  List<Classifier.Recognition> results;
    private int size = 0;
    private Handler myHandler = new Handler()
    {
        @Override
        //重写handleMessage方法,根据msg中what的值判断是否执行后续操作
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case 0x24:
                    startActivity(new Intent(getApplication(),  MainActivity.class));
                    WelcomActivity.this.finish();
                case 0x123:
                    i++;
                    if (textView != null)
                    textView.setText("\n正在处理图片 " + i + "/" + size);
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,  Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
        } catch (Exception e) {
            Log.d("didn't", "perssion");
        }
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_welcome);
        textView = (TextView) findViewById(R.id.work_process);

        // init tensorflow
        if (Config.classifier == null) {
            // get permission

            Config.classifier = new TensorFlowImageClassifier();
            try {
                Config.classifier.initializeTensorFlow(
                        getAssets(), Config.MODEL_FILE, Config.LABEL_FILE, Config.NUM_CLASSES, Config.INPUT_SIZE, Config.IMAGE_MEAN, Config.IMAGE_STD,
                        Config.INPUT_NAME, Config.OUTPUT_NAME);
            } catch (final IOException e) {
                ;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                prepareForApplication(WelcomActivity.this);
                Looper.loop();
            }
        }).start();

    }
    // get all image information in db

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public  void prepareForApplication(Context ctx) {
        Config.dbHelper = new MyDatabaseHelper(ctx, "Album.db", null, Config.dbversion);
        SQLiteDatabase db = Config.dbHelper.getWritableDatabase();
        List<Map> tmp = getMediaImageInfo(ctx);
        size = tmp.size();
        Log.d("ALBUM DATA", String.valueOf(tmp));
        String url = null;
        Cursor cursor = null;
        Cursor cursor_album = null;
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_4444;
        for (Map<String, String> map : tmp) {
            myHandler.sendEmptyMessage(0x123);
            url = map.get("_data");
            cursor = db.query("AlbumPhotos", null, "url = '" + url + "'", null, null, null, null);
            Log.d("Detail of photo", url);

            if (!cursor.moveToFirst()) {
                Log.d("Detail of photo", "not in album");
                ContentValues values = new ContentValues();
                values.put("url", url);

                if (Config.classifier != null) {
                    // get bitmap
                    bitmap = BitmapFactory.decodeFile(url, options);
                    do_tensorflow(bitmap);

                    values.put("album_name", results.get(0).getTitle());
                    Log.d("ADD INTO ALBUM", url + "  " + results.get(0).getTitle());
                    cursor_album = db.query("Album", null, "album_name ='" +  results.get(0).getTitle() + "'", null, null, null, null);
                    if (!cursor_album.moveToFirst()) {
                        ContentValues values_ablum = new ContentValues();
                        values_ablum.put("album_name", results.get(0).getTitle());
                        values_ablum.put("show_image", url);
                        db.insert("Album", null, values_ablum);
                    } else {
                        String album = cursor_album.getString(cursor_album.getColumnIndex("album_name"));
                        String url_album = cursor_album.getString(cursor_album.getColumnIndex("show_image"));

                        Log.d("ALBUM have album", album + " " + url_album);
                    }
                    cursor_album.close();
                }
                db.insert("AlbumPhotos", null, values);
                values.clear();
            }
            else {
                String name = cursor.getString(cursor.getColumnIndex("album_name"));
                String url_new = cursor.getString(cursor.getColumnIndex("url"));

                Log.d("ALBUM_PHOTO have photos", name + " " + url_new);
            }
            cursor.close();

        }
        db.close();
        Config.init = true;
        myHandler.sendEmptyMessage(0x24);
    }
    // use tf to deal image
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void do_tensorflow(Bitmap bitmap) {
        // resize
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) Config.INPUT_SIZE) / width;
        float scaleHeight = ((float) Config.INPUT_SIZE) / height;
        Matrix matrix = new Matrix();

        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbm = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        // get results
        results = Config.classifier.recognizeImage(newbm);
        Log.d("Result", String.valueOf(results));
    }

}
