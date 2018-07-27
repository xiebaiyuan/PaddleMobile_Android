package com.baidu.paddle.modeloader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.util.Log
import com.baidu.paddle.PML
import java.io.File


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

class MobileNetSSDModelLoaderImpl : ModelLoader() {

    private var type = ModelType.mobilenet_ssd_new
    // mobile net is bgr
    private val means = floatArrayOf(127.5f, 127.5f, 127.5f)
    private val ddims = intArrayOf(1, 3, 300, 300)
    private val scale = 0.007843f
//   b g r
    //   #    mean_value: [103.94,116.78,123.68]

//    #  transform_param {
//        #    scale: 0.017
//        #    mirror: false
//        #    crop_size: 224
//        #    mean_value: [103.94,116.78,123.68]
//        #  }
//    input_dim: 1
//    input_dim: 3
//    input_dim: 224
//    input_dim: 224

    override fun getScaledMatrix(bitmap: Bitmap, desWidth: Int, desHeight: Int): FloatArray {
        val rsGsBs = getRsGsBs(bitmap, desWidth, desHeight)

        val rs = rsGsBs.first
        val gs = rsGsBs.second
        val bs = rsGsBs.third

        val dataBuf = FloatArray(3 * desWidth * desHeight)

        if (rs.size + gs.size + bs.size != dataBuf.size) {
            throw IllegalArgumentException("不可能吧老铁?")
        }

        // bbbb... gggg.... rrrr...
        for (i in dataBuf.indices) {
            dataBuf[i] = when {
                i < bs.size -> (bs[i] - means[0]) * scale
                i < bs.size + gs.size -> (gs[i - bs.size] - means[1]) * scale
                else -> (rs[i - bs.size - rs.size] - means[2]) * scale
            }
        }

        return dataBuf
    }

    override fun getInputSize(): Int {
        return 300
    }


    override fun clear() {
        PML.clear()
    }

    override fun load() {
        val assetPath = "pml_demo"
        val sdcardPath = (Environment.getExternalStorageDirectory().toString()
                + File.separator + assetPath + File.separator + type)

        val modelPath = sdcardPath + File.separator + "model"
        val paramsPath = sdcardPath + File.separator + "params"
        Log.d("pml", "loadpath : $sdcardPath")
        Log.d("pml", "modelPath : $modelPath")
        Log.d("pml", "paramsPath : $paramsPath")


        PML.loadCombined(modelPath, paramsPath)
    }

    override fun predictImage(inputBuf: FloatArray): FloatArray? {
        var predictImage: FloatArray? = null
        try {
            val start = System.currentTimeMillis()
            predictImage = PML.predictImage(inputBuf, ddims)
            val end = System.currentTimeMillis()
            predictImageTime = end - start
        } catch (e: Exception) {
        }
        return predictImage
    }

    override fun predictImage(bitmap: Bitmap): FloatArray? {
        val buf = getScaledMatrix(bitmap, getInputSize(), getInputSize())
        return predictImage(buf)
    }


    override fun drawRect(canvas: Canvas, predicted: FloatArray, viewWidth: Int, viewHeight: Int) {
//        val paint = Paint()
//        paint.color = Color.RED
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = 3.0f
//        var x1 = 0f
//        var x2 = 0f
//        var y1 = 0f
//        var y2 = 0f
//
//        // the googlenet result sequence is (left top right top bottom)
//        x1 = predicted[0] * viewWidth / 224
//        y1 = predicted[1] * viewHeight / 224
//        x2 = predicted[2] * viewWidth / 224
//        y2 = predicted[3] * viewHeight / 224
//
//
//        canvas.drawRect(x1, y1, x2, y2, paint)
    }

}