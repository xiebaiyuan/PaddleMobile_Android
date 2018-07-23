package com.baidu.paddle.modeloader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import com.baidu.paddle.PML
import java.io.File


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

class MobileNetModelLoaderImpl : ModelLoader() {

    private var type = ModelType.mobilenet
    // mobile net is bgr
    private val means = floatArrayOf(103.94f, 116.78f, 123.68f)
    private val ddims = intArrayOf(1, 3, 224, 224)
    private val scale = 0.017f
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
        val dataBuf = FloatArray(3 * desWidth * desHeight)
        var firtChannel: Int
        var secountChannel: Int
        var thirdChannel: Int
        val pixels = IntArray(desWidth * desHeight)
        val bm = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false)
        bm.getPixels(pixels, 0, desWidth, 0, 0, desWidth, desHeight)
        var j = 0
        var k = 0
        for (i in pixels.indices) {
            val clr = pixels[i]
//            Log.d("plm","clr= "+String.format("%#x\n",clr))

            j = i / desHeight
            k = i % desWidth
            firtChannel = j * desWidth + k
            secountChannel = firtChannel + desHeight * desWidth
            thirdChannel = secountChannel + desHeight * desWidth

            val r = (((clr and 0x00ff0000) shr 16).toFloat() - means[2]) * scale
            val g = (((clr and 0x0000ff00) shr 8).toFloat() - means[1]) * scale
            val b = (((clr and 0x000000ff)).toFloat() - means[0]) * scale

            //  mobilenet  is bgr
            dataBuf[firtChannel] = b
            dataBuf[secountChannel] = g
            dataBuf[thirdChannel] = r

        }
        if (bm.isRecycled) {
            bm.recycle()
        }
        return dataBuf
    }

    override fun getInputSize(): Int {
        return 224
    }


    override fun clear() {
        PML.clear()
    }

    override fun load() {
        val assetPath = "pml_demo"
        val sdcardPath = (Environment.getExternalStorageDirectory().toString()
                + File.separator + assetPath + File.separator + type)
        PML.load(sdcardPath)
    }

    override fun predictImage(buf: FloatArray): FloatArray? {
        var predictImage: FloatArray? = null
        try {
            val start = System.currentTimeMillis()
            predictImage = PML.predictImage(buf, ddims)
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