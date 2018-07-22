package com.baidu.paddle.modeloader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import com.baidu.paddle.PML
import java.io.File


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

class GoogleNetModelLoaderImpl : ModelLoader() {


    private var type = ModelType.googlenet
    private val means = floatArrayOf(148.0f, 148.0f, 148.0f)
    private val ddims = intArrayOf(1, 3, 224, 224)


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

    override fun getScaledMatrix(bitmap: Bitmap, desWidth: Int, desHeight: Int): FloatArray {
        val dataBuf = FloatArray(3 * desWidth * desHeight)
        var rIndex: Int
        var gIndex: Int
        var bIndex: Int
        val pixels = IntArray(desWidth * desHeight)
        val bm = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false)
        bm.getPixels(pixels, 0, desWidth, 0, 0, desWidth, desHeight)
        var j = 0
        var k = 0
        for (i in pixels.indices) {
            val clr = pixels[i]
            j = i / desHeight
            k = i % desWidth
            rIndex = j * desWidth + k
            gIndex = rIndex + desHeight * desWidth
            bIndex = gIndex + desHeight * desWidth
            dataBuf[rIndex] = (clr and 0x00ff0000 shr 16).toFloat() - means[0]
            dataBuf[gIndex] = (clr and 0x0000ff00 shr 8).toFloat() - means[1]
            dataBuf[bIndex] = (clr and 0x000000ff).toFloat() - means[2]

        }
        if (bm.isRecycled) {
            bm.recycle()
        }
        return dataBuf
    }


    override fun predictImage(buf: FloatArray): FloatArray? {
        var result: FloatArray? = null
        try {
            val start = System.currentTimeMillis()
            result = PML.predictImage(buf, ddims)
            //result = PML.predictImage(inputData, ddims);
            val end = System.currentTimeMillis()
            predictImageTime = end - start

        } catch (e: Exception) {
            e.printStackTrace()
        }


        return result
    }

    override fun drawRect(canvas: Canvas, predicted: FloatArray, viewWidth: Int, viewHeight: Int) {
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.0f
        var x1 = 0f
        var x2 = 0f
        var y1 = 0f
        var y2 = 0f

        // the googlenet result sequence is (left top right top bottom)
        x1 = predicted[0] * viewWidth / 224
        y1 = predicted[1] * viewHeight / 224
        x2 = predicted[2] * viewWidth / 224
        y2 = predicted[3] * viewHeight / 224


        canvas.drawRect(x1, y1, x2, y2, paint)
    }


    override fun predictImage(bitmap: Bitmap): FloatArray?{
        val buf = getScaledMatrix(bitmap, getInputSize(), getInputSize())
        return predictImage(buf)
    }


}