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
import io.reactivex.schedulers.Schedulers;


@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint({"SetTextI18n", "CheckResult"})
public class MainActivity extends Activity {
    public static final int TAKE_PHOTO_REQUEST_CODE = 1001;
    public static final int PERMISSION_REQUEST_CODE = 1002;
    private ModelLoader loader = new MobileNetModelLoaderImpl();

    boolean isloaded = false;
    boolean isModelCopyed = false;
    private ImageView imageView;
    private TextView tvSpeed;

    static {
        try {
            System.loadLibrary("paddle-mobile");
        } catch (SecurityException | UnsatisfiedLinkError | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private TextView infos;
    private TextView predictInfos;
    private String mCurrentPath;


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
        Button button = findViewById(R.id.button);
        Button btnBanana = findViewById(R.id.predict_banada);
        infos = findViewById(R.id.tv_infos);
        predictInfos = findViewById(R.id.tv_preinfos);
        btnBanana.setOnClickListener(view -> scaleImageAndPredictImage(getBanana().getPath()));
        button.setOnClickListener(view -> {
            if (!isHasSdCard()) {
                Toast.makeText(MainActivity.this, R.string.sdcard_not_available,
                        Toast.LENGTH_LONG).show();
                return;
            }
            takePicFromCamera();

        });
        Button bt_load = findViewById(R.id.bt_load);
        bt_load.setOnClickListener(view -> {
            isloaded = true;
            loader.load();
        });
        Button bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setOnClickListener(view -> {
            isloaded = false;
            loader.clear();
        });


    }

    private void takePicFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String path = getTempImage().getPath();

        mCurrentPath = path;
        Uri mOriginUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mOriginUri = FileProvider.getUriForFile(MainActivity.this.getApplication(), MainActivity.this.getApplication().getPackageName() + ".FileProvider",
                    new File(path));
        } else {
            mOriginUri = Uri.fromFile(new File(path));
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mOriginUri);

        MainActivity.this.startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
    }

    @SuppressLint("CheckResult")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
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
                    scaleImageAndPredictImage(mCurrentPath);
                }
                break;
            default:
                break;
        }
    }


    /**
     * check whether sdcard is mounted
     */
    public boolean isHasSdCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    boolean isbusy = false;
    /**
     * 缩放然后predict这张图片
     */
    private void scaleImageAndPredictImage(String path) {
        tvSpeed.setText("");
        if (path == null) {
            Toast.makeText(this, "图片lost", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isbusy){
            Toast.makeText(this, "处于前一次操作中", Toast.LENGTH_SHORT).show();
            return;
        }

        Observable
                .just(path)
                .map(s -> {
                    if (!isloaded) {
                        isloaded = true;
                        loader.load();
                    }
                    return getScaleBitmap(
                            MainActivity.this,
                            path
                    );
                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bitmap -> imageView.setImageBitmap(bitmap))
//                .observeOn(Schedulers.io())
                .map(loader::predictImage)
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<float[]>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        isbusy = true;
                    }

                    @Override
                    public void onNext(float[] result) {
                        float max = Float.MIN_VALUE;
                        int maxi = -1;
                        float sum = 0;
                        if (result != null) {
                            Log.d("pml", "result.length: " + result.length);

                            for (int i = 0; i < result.length; ++i) {
                                Log.d("detail", " index: " + i + " value: " + result[i]);
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
                    }

                    @Override
                    public void onError(Throwable e) {
                        isbusy = false;
                    }

                    @Override
                    public void onComplete() {
                        isbusy = false;
                    }
                });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("pml", "pml clear");
        // clear pml
        isloaded = false;
        loader.clear();
    }
}
