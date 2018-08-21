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
package com.baidu.paddle

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.baidu.paddle.data.MobileNetClassfiedData
import com.baidu.paddle.data.banana
import com.baidu.paddle.data.pathList
import com.baidu.paddle.data.tempImage
import com.baidu.paddle.modeloader.LoaderFactory
import com.baidu.paddle.modeloader.ModelLoader
import com.baidu.paddle.modeloader.ModelType
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.main_activity.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import java.util.concurrent.TimeUnit


@SuppressLint("SetTextI18n")
class MainActivity : Activity(), AnkoLogger {

    private lateinit var mModelLoader: ModelLoader

    private var mCurrentType = ModelType.genet_combine
    private var mThreadCounts = 1
    val modelList: ArrayList<ModelType> by lazy {
        val list = ArrayList<ModelType>()
        list.add(ModelType.mobilenet)
        list.add(ModelType.googlenet)
        list.add(ModelType.mobilenet_combined)
        list.add(ModelType.mobilenet_combined_qualified)
        list.add(ModelType.mobilenet_ssd_gesture)
        list.add(ModelType.genet_combine)
        list
    }

    val threadCountList: ArrayList<Int> by lazy {
        Runtime.getRuntime().availableProcessors()
        val list = ArrayList<Int>()
//        for (i in (1..Runtime.getRuntime().availableProcessors())) {
//            list.add(i)
//        }
        list.add(1)
        list.add(2)
        list.add(4)
        list
    }


    private var isloaded = false
    private var isModelCopyed = false

    private var mCurrentPath: String? = banana.absolutePath

    private val isGotNeededPermissions: Boolean
        get() = PermissionUtils.checkPermissions(this, Manifest.permission.CAMERA) && PermissionUtils.checkPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)


    /**
     * check whether sdcard is mounted
     */
    val isHasSdCard: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    internal var isbusy = false
    internal var timeList: MutableList<Long> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        init()
        if (!isGotNeededPermissions) {
            doRequestPermission()
        } else {
            copyModels()
        }

    }

    private fun doRequestPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            this.requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun init() {
        updateCurrentModel()
        mModelLoader.setThreadCount(mThreadCounts)
        thread_counts.text = "$mThreadCounts"
        clearInfos()
        mCurrentPath = banana.absolutePath
        predict_banada.setOnClickListener {


            scaleImageAndPredictImageTen(mCurrentPath)

        }
        btn_takephoto.setOnClickListener {
            if (!isHasSdCard) {
                Toast.makeText(this@MainActivity, R.string.sdcard_not_available,
                        Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            takePicFromCamera()

        }
        bt_load.setOnClickListener {
            isloaded = true
            mModelLoader.load()
        }

        bt_clear.setOnClickListener {
            isloaded = false
            mModelLoader.clear()
            clearInfos()
        }
        ll_model.setOnClickListener {
            MaterialDialog.Builder(this)
                    .title("选择模型")
                    .items(modelList)
                    .itemsCallbackSingleChoice(modelList.indexOf(mCurrentType))
                    { _, _, which, text ->

                        info { "which=$which" }
                        info { "text=$text" }
                        mCurrentType = modelList[which]
                        updateCurrentModel()
                        reloadModel()
                        clearInfos()
                        true
                    }
                    .positiveText("确定")
                    .show()
        }

        ll_threadcount.setOnClickListener {
            MaterialDialog.Builder(this)
                    .title("设置线程数量")
                    .items(threadCountList)
                    .itemsCallbackSingleChoice(threadCountList.indexOf(mThreadCounts))
                    { _, _, which, _ ->

                        mThreadCounts = threadCountList[which]
                        info { "mThreadCounts=$mThreadCounts" }
                        mModelLoader.setThreadCount(mThreadCounts)
                        reloadModel()
                        thread_counts.text = "$mThreadCounts"
                        clearInfos()
                        true
                    }
                    .positiveText("确定")
                    .show()
        }
    }

    private fun reloadModel() {
        mModelLoader.clear()
        mModelLoader.load()
        isloaded = true
    }

    private fun updateCurrentModel() {
        tv_modetext.text = mCurrentType.name
        mModelLoader = LoaderFactory.buildLoader(mCurrentType)
    }

    private fun takePicFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val path = tempImage.path

        mCurrentPath = path
        val mOriginUri: Uri
        mOriginUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this@MainActivity.application, this@MainActivity.application.packageName + ".FileProvider",
                    File(path))
        } else {
            Uri.fromFile(File(path))
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mOriginUri)

        this@MainActivity.startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE)
    }

    @SuppressLint("CheckResult")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (isGotNeededPermissions) {
                copyModels()
            } else {
                doRequestPermission()
            }
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun copyModels() {
        if (isModelCopyed) {
            return
        }
        tv_infos?.text = "拷贝模型中...."

        val dialog = MaterialDialog.Builder(this)
                .title("模型拷贝中")
                .content("请稍等..")
                .progress(true, 0)
                .show()

        Observable.create { emitter: ObservableEmitter<String> ->
            val assetPath = "pml_demo"
            val sdcardPath = (Environment.getExternalStorageDirectory().toString() + File.separator + assetPath)
            FileUtils.delDir(sdcardPath)
            FileUtils.copyFilesFromAssets(this@MainActivity, assetPath, sdcardPath)
            emitter.onNext(sdcardPath)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread(), true)
                .subscribe { path ->
                    isModelCopyed = true
                    tv_infos.text = "模型已拷贝至$path"
                    scaleAndShowBitmap(banana.absolutePath)
                    dialog.dismiss()
                }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TAKE_PHOTO_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                // scaleImageAndPredictImage(mCurrentPath);
//                scaleImageAndPredictImageTen(mCurrentPath)

                scaleAndShowBitmap(mCurrentPath)

            }
            else -> {
            }
        }
    }

    private fun scaleAndShowBitmap(path: String?) {
        Observable
                .just(path)
                .map {
                    mModelLoader.getScaleBitmap(
                            this@MainActivity,
                            this.mCurrentPath
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { bitmap -> show_image.setImageBitmap(bitmap) }
                .subscribe()
    }

    /**
     * 缩放然后predict这张图片
     */
    private fun scaleImageAndPredictImage(path: String?) {
        if (path == null) {
            Toast.makeText(this, "图片lost", Toast.LENGTH_SHORT).show()
            return
        }
        if (isbusy) {
            Toast.makeText(this, "处于前一次操作中", Toast.LENGTH_SHORT).show()
            return
        }



        Observable
                .just(path)
                .map {
                    if (!isloaded) {
                        isloaded = true
                        mModelLoader.load()
                    }
                    mModelLoader.getScaleBitmap(
                            this@MainActivity,
                            path
                    )
                }
                //                .subscribeOn(Schedulers.io())
                //                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { bitmap -> show_image.setImageBitmap(bitmap) }
                //                .observeOn(Schedulers.io())
                .map<FloatArray>(mModelLoader::predictImage)
                //                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<FloatArray> {
                    override fun onSubscribe(d: Disposable) {
                        isbusy = true
                    }

                    override fun onNext(result: FloatArray) {
                        var max = java.lang.Float.MIN_VALUE
                        var maxi = -1
                        var sum = 0f
                        info { "result.length: " + result.size }

                        for (i in result.indices) {
                            info { " index: " + i + " value: " + result[i] }
                            sum += result[i]
                            if (result[i] > max) {
                                max = result[i]
                                maxi = i
                            }
                        }
                        info { "maxindex: $maxi" }
                        info { "max: $max" }
                        info { "sum: $sum" }

                        tv_preinfos.text = "结果是: ${MobileNetClassfiedData.dataList[maxi]}\n耗时:${mModelLoader.predictImageTime}ms"
                    }

                    override fun onError(e: Throwable) {
                        isbusy = false
                    }

                    override fun onComplete() {
                        isbusy = false
                    }
                })
    }

    /**
     * 缩放然后predict这张图片
     */
    private fun scaleImageAndPredictImageTen(path: String?) {

        if (path == null) {
            Toast.makeText(this, "图片lost", Toast.LENGTH_SHORT).show()
            return
        }
        if (isbusy) {
            Toast.makeText(this, "处于前一次操作中", Toast.LENGTH_SHORT).show()
            return
        }
        timeList.clear()
        tv_infos.text = "运算中..."
//        val dialog = MaterialDialog.Builder(this)
//                .title("运算中")
//                .content("请稍等")
//                .progress(true, 0)
//                .show()


        Observable
                .just(path)
                .map {
                    if (!isloaded) {
                        isloaded = true
                        mModelLoader.setThreadCount(mThreadCounts)
                        mModelLoader.load()
                    }
                    mModelLoader.getScaleBitmap(
                            this@MainActivity,
                            path
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { bitmap -> show_image.setImageBitmap(bitmap) }
                //  .observeOn(Schedulers.io())

                .map { bitmap ->
                    var floatsTen: FloatArray? = null
                    for (i in 0..10) {
                        val floats = mModelLoader.predictImage(bitmap)
                        val predictImageTime = mModelLoader.predictImageTime
                        timeList.add(predictImageTime)

                        if (i == 10) {
                            floatsTen = floats
                        }
                    }
                    Pair(floatsTen!!, bitmap)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .map({ floatArrayBitmapPair ->

                    mModelLoader.mixResult(show_image, floatArrayBitmapPair)

                    floatArrayBitmapPair.second
                    floatArrayBitmapPair.first
                })/*.repeat(1000)*/
                .subscribe(object : Observer<FloatArray> {
                    override fun onSubscribe(d: Disposable) {
                        isbusy = true
                    }

                    override fun onNext(result: FloatArray) {
                        var max = java.lang.Float.MIN_VALUE
                        var maxi = -1
                        var sum = 0f
                        info { "result.length: " + result.size }

//                        for (i in result.indices) {
//                            info { " index: " + i + " value: " + result[i] }
//                            sum += result[i]
//                            if (result[i] > max) {
//                                max = result[i]
//                                maxi = i
//                            }
//                        }
//                        info { "maxindex: $maxi" }
//                        info { "max: $max" }
//                        info { "sum: $sum" }
//                        var sumTime: Long = 0
//                        for (i in 1 until timeList.size) {
//                            sumTime += timeList[i]
//                        }
//
////                        val resultInfo = "结果是: ${MobileNetClassfiedData.dataList[maxi]}\n"
//                        val timeInfo = "平均耗时:${sumTime / 10}ms"
//
//                        tv_preinfos.text = "" + timeInfo

                        //           dialog.dismiss()

//                        val resultInfos = StringBuilder()
//                        for (i in result.indices) {
//                           info { " index: " + i + " value: " + result[i] }
//                           resultInfos.appendln(" index: $i value: ${result[i]}")
//                            sum += result[i]
//                            if (result[i] > max) {
//                                max = result[i]
//                                maxi = i
//                            }
//                        }
//                        info { "maxindex: $maxi" }
//                        info { "max: $max" }
//                        info { "sum: $sum" }
////                        var sumTime: Long = 0
//                        for (i in 1 until timeList.size) {
//                            sumTime += timeList[i]
//                        }
                        timeList.removeAt(0)
//                        val resultInfo = "结果是: ${MobileNetClassfiedData.dataList[maxi]}\n"
                        val timeInfo = "运行10次平均耗时:${timeList.average()}ms"

                        //   tv_preinfos.text = resultInfo + timeInfo
                        tv_preinfos.text = "$timeInfo\n点击查看结果"
                        tv_preinfos.setOnClickListener {
                            MaterialDialog.Builder(this@MainActivity)
                                    .title("结果:")
                                    .content(timeInfo + "\n  result.size: " + result.size)
                                    .show()
                        }

                    }

                    override fun onError(e: Throwable) {
                        isbusy = false
                    }

                    override fun onComplete() {
                        isbusy = false
                        tv_infos.text = ""
                    }
                })
    }

    /**
     * 缩放然后predict这张图片
     */
    private fun scaleImageAndPredictImageleaktest(path: String?) {

//        if (path == null) {
//            Toast.makeText(this, "图片lost", Toast.LENGTH_SHORT).show()
//            return
//        }
//        if (isbusy) {
//            Toast.makeText(this, "处于前一次操作中", Toast.LENGTH_SHORT).show()
//            return
//        }
        timeList.clear()
        tv_infos.text = "运算中..."
//        val dialog = MaterialDialog.Builder(this)
//                .title("运算中")
//                .content("请稍等")
//                .progress(true, 0)
//                .show()
        pathList
                .toObservable()
                .delay(10, TimeUnit.MILLISECONDS)
                .map {
                    info { "path  = :$it" }
                    if (!isloaded) {
                        isloaded = true
                        mModelLoader.setThreadCount(mThreadCounts)
                        mModelLoader.load()
                    }
                    mModelLoader.getScaleBitmap(
                            this@MainActivity,
                            it
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
//                .doOnNext { bitmap -> show_image.setImageBitmap(bitmap) }
                //  .observeOn(Schedulers.io())

                .map { bitmap ->
                    var floatsTen: FloatArray? = null
//                    for (i in 0..10) {
//                        val floats = mModelLoader.predictImage(bitmap)
//                        val predictImageTime = mModelLoader.predictImageTime
//                        timeList.add(predictImageTime)
//
//                        if (i == 10) {
//                            floatsTen = floats
//                        }
//                    }
                    val floats = mModelLoader.predictImage(bitmap)
                    floatsTen = floats
                    val predictImageTime = mModelLoader.predictImageTime
                    timeList.add(predictImageTime)
                    Pair(floatsTen!!, bitmap)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .map({ floatArrayBitmapPair ->

                    mModelLoader.mixResult(show_image, floatArrayBitmapPair)

                    floatArrayBitmapPair.second
                    floatArrayBitmapPair.first
                }).repeat(10)
                .subscribe(object : Observer<FloatArray> {
                    override fun onSubscribe(d: Disposable) {
                        isbusy = true
                    }

                    override fun onNext(result: FloatArray) {
                        var max = java.lang.Float.MIN_VALUE
                        var maxi = -1
                        var sum = 0f
                        info { "result.length: " + result.size }

//                        for (i in result.indices) {
//                            info { " index: " + i + " value: " + result[i] }
//                            sum += result[i]
//                            if (result[i] > max) {
//                                max = result[i]
//                                maxi = i
//                            }
//                        }
//                        info { "maxindex: $maxi" }
//                        info { "max: $max" }
//                        info { "sum: $sum" }
//                        var sumTime: Long = 0
//                        for (i in 1 until timeList.size) {
//                            sumTime += timeList[i]
//                        }
                        timeList.toLongArray().average()
//
////                        val resultInfo = "结果是: ${MobileNetClassfiedData.dataList[maxi]}\n"
                        val timeInfo = "平均耗时:${timeList.toLongArray().average()}ms"
//
                        tv_preinfos.text = "" + timeInfo

                        //           dialog.dismiss()

//                        val resultInfos = StringBuilder()
//                        for (i in result.indices) {
//                            info { " index: " + i + " value: " + result[i] }
//                            resultInfos.appendln(" index: $i value: ${result[i]}")
//                            sum += result[i]
//                            if (result[i] > max) {
//                                max = result[i]
//                                maxi = i
//                            }
//                        }
//                        info { "maxindex: $maxi" }
//                        info { "max: $max" }
//                        info { "sum: $sum" }
//                        var sumTime: Long = 0
//                        for (i in 1 until timeList.size) {
//                            sumTime += timeList[i]
//                        }

//                        val resultInfo = "结果是: ${MobileNetClassfiedData.dataList[maxi]}\n"
//                        val timeInfo = "运行10次平均耗时:${sumTime / 100L}ms"

                        //   tv_preinfos.text = resultInfo + timeInfo
//                        tv_preinfos.text = "$timeInfo\n点击查看结果"
//                        tv_preinfos.setOnClickListener {
//                            val dialog = MaterialDialog.Builder(this@MainActivity)
//                                    .title("结果:")
//                                    .content(timeInfo + "\n" + resultInfos.toString())
//                                    .show()
//                        }

                    }

                    override fun onError(e: Throwable) {
                        isbusy = false
                    }

                    override fun onComplete() {
                        isbusy = false
                        tv_infos.text = ""
                    }
                })
    }


    fun clearInfos() {
        tv_infos.text = ""
        tv_preinfos.text = ""
    }

    override fun onBackPressed() {
        super.onBackPressed()

        info { "pml clear" }
        // clear pml
        isloaded = false
        mModelLoader.clear()
    }

    companion object {
        internal const val TAG = "pml"
        const val TAKE_PHOTO_REQUEST_CODE = 1001
        const val PERMISSION_REQUEST_CODE = 1002

        init {
            try {
                System.loadLibrary("paddle-mobile")
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

        }

    }
}
