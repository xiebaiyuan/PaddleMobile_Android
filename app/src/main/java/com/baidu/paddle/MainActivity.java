/*
 * Copyright (c) 2016 Baidu, Inc. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.baidu.paddle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.paddle.modeloader.MobileNetModelLoaderImpl;
import com.baidu.paddle.modeloader.ModelLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class MainActivity extends Activity {
    public static final int TAKE_PHOTO_REQUEST_CODE = 1001;
    public static final int PERMISSION_REQUEST_CODE = 1002;

    boolean isloaded = false;
    private ModelLoader loader = new MobileNetModelLoaderImpl();
    boolean isModelCopyed = false;

    private Button btnBanana;

    //    private ModelType type = ModelType.googlenet;
    private ImageView imageView;
    private TextView tvSpeed;
    private Button button;
    private Bitmap bmp;

    static {
        try {
            System.loadLibrary("paddle-mobile");

        } catch (SecurityException e) {
            e.printStackTrace();

        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();

        } catch (NullPointerException e) {
            e.printStackTrace();

        }

    }

    private Uri mOriginUri;
    private TextView infos;
    private TextView predictInfos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        init();

        if (!isGotNeededPermissions()) {
            doRequestPermission();
        } else {
            copyModels();
        }

    }

    private void doRequestPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            this.requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void init() {
        imageView = findViewById(R.id.imageView);
        tvSpeed = findViewById(R.id.tv_speed);
        button = findViewById(R.id.button);
        btnBanana = findViewById(R.id.predict_banada);
        infos = findViewById(R.id.tv_infos);
        predictInfos = findViewById(R.id.tv_preinfos);
        btnBanana.setOnClickListener(view -> scaleImageAndPredictImage(getBanana().getPath()));
        button.setOnClickListener(view -> {
            if (!isHasSdCard()) {
                Toast.makeText(MainActivity.this, R.string.sdcard_not_available,
                        Toast.LENGTH_LONG).show();
                return;
            }
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                // save pic in sdcard
//                Uri imageUri = Uri.fromFile(getTempImage());
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//                startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);

            takePicFromCamera();

        });
        Button bt_load = (Button) findViewById(R.id.bt_load);
        bt_load.setOnClickListener(view -> {
            isloaded = true;
            loader.load();
        });
        Button bt_clear = (Button) findViewById(R.id.bt_clear);
        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isloaded = false;

                loader.clear();
            }
        });


    }

    private void takePicFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mOriginUri = FileProvider.getUriForFile(MainActivity.this.getApplication(), MainActivity.this.getApplication().getPackageName() + ".FileProvider",
                    new File(getTempImage().getPath()));
        } else {
            mOriginUri = Uri.fromFile(new File(getTempImage().getPath()));
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mOriginUri);

        MainActivity.this.startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
    }

    @SuppressLint("CheckResult")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
//            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (isGotNeededPermissions()) {
                copyModels();
            } else {
                doRequestPermission();
            }
        }
    }

    @SuppressLint({"CheckResult", "SetTextI18n"})
    private void copyModels() {
        if (isModelCopyed) {
            return;
        }
        infos.setText("拷贝模型中....");

        Observable.create((ObservableEmitter<String> emitter) -> {
            String assetPath = "pml_demo";
            String sdcardPath = Environment.getExternalStorageDirectory()
                    + File.separator + assetPath;
            copyFilesFromAssets(MainActivity.this, assetPath, sdcardPath);
            emitter.onNext(sdcardPath);

        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread(), true)
                .subscribe(path -> {
                            isModelCopyed = true;
                            infos.setText("模型已拷贝至" + path);
                          //  Toast.makeText(MainActivity.this, "模型已拷贝至" + path, Toast.LENGTH_SHORT).show();
                        }
                );


    }

    private boolean isGotNeededPermissions() {
        return PermissionUtils.INSTANCE.checkPermissions(this, Manifest.permission.CAMERA)
                &&
                PermissionUtils.INSTANCE.checkPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private File getBanana() {
        String assetPath = "pml_demo";
        String imagePath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        File tempFile = new File(imagePath, "banana.jpeg");
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }


    private File getHand() {
        String assetPath = "pml_demo";
        String imagePath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        File tempFile = new File(imagePath, "hand.jpg");
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

    private File getHand2() {
        String assetPath = "pml_demo";
        String imagePath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        File tempFile = new File(imagePath, "hand2.jpg");
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

    private File getApple() {
        String assetPath = "pml_demo";
        String imagePath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        File tempFile = new File(imagePath, "apple.jpg");
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

    private static final String TAG = "pml";

    /**
     * scale bitmap in case of OOM
     *
     * @param ctx
     * @param filePath
     * @return
     */
    public Bitmap getScaleBitmap(Context ctx, String filePath) {
        Log.d(TAG, "getScaleBitmap: filePath: " + filePath);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opt);

        int bmpWidth = opt.outWidth;
        int bmpHeight = opt.outHeight;

        int maxSize = loader.getInputSize() * 2;

        opt.inSampleSize = 1;
        while (true) {
            if (bmpWidth / opt.inSampleSize <= maxSize || bmpHeight / opt.inSampleSize <= maxSize) {
                break;
            }
            opt.inSampleSize *= 2;
        }
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, opt);
    }

    /**
     * scale bitmap in case of OOM
     *
     * @param ctx
     * @param filePath
     * @return
     */
    public Bitmap getBitmap(Context ctx, String filePath) {
        Log.d(TAG, "getScaleBitmap: filePath: " + filePath);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, opt);
    }

    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                file.mkdirs();
                // copy recursivelyC
                for (String fileName : fileNames) {
                    Log.d(TAG, "copyFilesFromAssets fileName: " + fileName);
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public File getTempImage() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File tempFile = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return tempFile;
        }
        return null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
//                    DetectionTask detectionTask = new DetectionTask();
//                    detectionTask.execute(getTempImage().getPath());

                    scaleImageAndPredictImage(getTempImage().getPath());

                }
                break;
            default:
                break;
        }
    }

//    /**
//     * draw rect on imageView
//     *
//     * @param bitmap
//     * @param predicted
//     * @param viewWidth
//     * @param viewHeight
//     */
//    private void drawRect(Bitmap bitmap, float[] predicted, int viewWidth, int viewHeight) {
//
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawBitmap(bitmap, 0, 0, null);
//
//        loader.drawRect(canvas, predicted, viewWidth, viewHeight);
//
//        imageView.setImageBitmap(bitmap);
//
//    }
//
//    private void drawImage(Bitmap bitmap) {
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawBitmap(bitmap, 0, 0, null);
//        imageView.setImageBitmap(bitmap);
//    }
//
//    float getMaxIndex(float[] predicted) {
//        float max = 0;
//        int index = 0;
//        for (int i = 0; i < predicted.length; i++) {
//            if (predicted[i] > max) {
//                max = predicted[i];
//                index = i;
//            }
//        }
//        return index;
//    }

//    public float[] getScaledMatrix(Bitmap bitmap, int desWidth,
//                                   int desHeight) {
//        float[] dataBuf = new float[3 * desWidth * desHeight];
//        int rIndex;
//        int gIndex;
//        int bIndex;
//        int[] pixels = new int[desWidth * desHeight];
//        Bitmap bm = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false);
//        bm.getPixels(pixels, 0, desWidth, 0, 0, desWidth, desHeight);
//        int j = 0;
//        int k = 0;
//        for (int i = 0; i < pixels.length; i++) {
//            int clr = pixels[i];
//            j = i / desHeight;
//            k = i % desWidth;
//            rIndex = j * desWidth + k;
//            gIndex = rIndex + desHeight * desWidth;
//            bIndex = gIndex + desHeight * desWidth;
//            dataBuf[rIndex] = (float) ((clr & 0x00ff0000) >> 16) - 148;
//            dataBuf[gIndex] = (float) ((clr & 0x0000ff00) >> 8) - 148;
//            dataBuf[bIndex] = (float) ((clr & 0x000000ff)) - 148;
//
//        }
//        if (bm.isRecycled()) {
//            bm.recycle();
//        }
//        return dataBuf;
//
//
//    }

    /**
     * check whether sdcard is mounted
     *
     * @return
     */
    public boolean isHasSdCard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 缩放然后predict这张图片
     */
    @SuppressLint("SetTextI18n")
    private void scaleImageAndPredictImage(String path) {
        tvSpeed.setText("");
        if (!isloaded) {
            isloaded = true;
            loader.load();
        }
//        if (path == null) {
//            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
//        }
        Bitmap scaleBitmap = getScaleBitmap(
                MainActivity.this,
                path
        );

        imageView.setImageBitmap(scaleBitmap);
        float[] result = loader.predictImage(
                scaleBitmap
        );

        float max = Float.MIN_VALUE;
        int maxi = -1;

        float sum = 0;
        if (result != null) {
            Log.d("pml", "result.length: " + result.length);

            for (int i = 0; i < result.length; ++i) {
                Log.d("pml: ", " index: " + i + " value: " + result[i]);
                sum += result[i];
                if (result[i] > max) {
                    max = result[i];
                    maxi = i;
                }
            }
        }

        Log.d("pml", "maxindex: " + maxi);
        Log.d("pml", "max: " + max);
        Log.d("pml", "sum: " + sum);
        tvSpeed.setText("detection cost：" + loader.getPredictImageTime() + "ms");

        predictInfos.setText(
                "结果是: " + MobileNetClassfiedData.INSTANCE.getDataList().get(maxi) +
                        "\n耗时:" + loader.getPredictImageTime() + "ms");

        // Toast.makeText(this, "maxindex: " + maxi + " max: " + max, Toast.LENGTH_SHORT).show();
    }
//    public void dumpData(float[] results, String filename) {
//        try {
//            File writename = new File(filename);
//            writename.createNewFile();
//            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
//            for (float result : results) {
//                out.write(result + " ");
//            }
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("pml", "pml clear");
        // clear pml
        isloaded = false;

        loader.clear();

    }

//    class DetectionTask extends AsyncTask<String, Void, float[]> {
//        private long time;
//
//        public DetectionTask() {
//            super();
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//
//
//        }
//
//        @Override
//        protected float[] doInBackground(String... strings) {
//            bmp = getScaleBitmap(mContext, strings[0]);
//
//            float[] result = null;
//            try {
//                result = loader.predictImage(bmp);
//                time = loader.getPredictImageTime();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            if (result != null) {
//                for (int i = 0; i < result.length; ++i) {
//                    Log.d("pml: ", " index: " + i + " value: " + result[i]);
//                }
//            }
//
//            return result;
//        }
//
//        @Override
//        protected void onPostExecute(float[] result) {
//            super.onPostExecute(result);
//            try {
//                Bitmap src = Bitmap.createScaledBitmap(bmp, imageView.getWidth(),
//                        imageView.getHeight(), false);
//                drawRect(src, result, imageView.getWidth(), imageView.getHeight());
//                tvSpeed.setText("detection cost：" + time + "ms");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        protected void onProgressUpdate(Void... values) {
//            super.onProgressUpdate(values);
//        }
//
//        @Override
//        protected void onCancelled() {
//            super.onCancelled();
//        }
//
//
//    }
}
