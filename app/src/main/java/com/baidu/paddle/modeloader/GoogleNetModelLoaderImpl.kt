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
    private val scale = 1


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
        // r g b
        val rsGsBs = getRsGsBs(bitmap, desWidth, desHeight)

        val bs = rsGsBs.third
        val gs = rsGsBs.second
        val rs = rsGsBs.first

        val dataBuf = FloatArray(3 * desWidth * desHeight)

        if (bs.size + gs.size + rs.size != dataBuf.size) {
            throw IllegalArgumentException("不可能吧老铁?")
        }

        // bbbb... gggg.... rrrr...
        for (i in dataBuf.indices) {
            dataBuf[i] = when {
                i < rs.size -> (rs[i] - means[0]) * scale
                i < rs.size + gs.size -> (gs[i - rs.size] - means[1]) * scale
                else -> (bs[i - rs.size - bs.size] - means[2]) * scale
            }
        }
        return dataBuf
    }


    override fun predictImage(inputBuf: FloatArray): FloatArray? {
        var result: FloatArray? = null
        try {
            val start = System.currentTimeMillis()
            result = PML.predictImage(inputBuf, ddims)
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
        val x1: Float = predicted[0] * viewWidth / 224
        val x2: Float = predicted[2] * viewWidth / 224
        val y1: Float = predicted[1] * viewHeight / 224
        val y2: Float = predicted[3] * viewHeight / 224
        canvas.drawRect(x1, y1, x2, y2, paint)
    }


    override fun predictImage(bitmap: Bitmap): FloatArray?{
        val buf = getScaledMatrix(bitmap, getInputSize(), getInputSize())
        return predictImage(buf)
    }


}