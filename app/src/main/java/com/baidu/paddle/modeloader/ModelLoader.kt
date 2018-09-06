package com.baidu.paddle.modeloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.widget.AppCompatImageView
import android.util.Log
import com.baidu.paddle.PML
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

abstract class ModelLoader : IModelLoader, AnkoLogger {
    var predictImageTime: Long = -1
    var isbusy = false
    var timeList: MutableList<Long> = ArrayList()
    var timeInfo: String = ""
    var mTimes: Long = 1
    var showAllresult =false
    /**
     * 将图片按照指定尺寸压好,将像素按照rs gs bs排列
     */
    fun getRsGsBs(
            bitmap: Bitmap,
            desWidth: Int,
            desHeight: Int
    ): Triple<ArrayList<Float>, ArrayList<Float>, ArrayList<Float>> {

        val pixels = IntArray(desWidth * desHeight)
        val bm = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false)
        bm.getPixels(pixels, 0, desWidth, 0, 0, desWidth, desHeight)

        val rs = ArrayList<Float>()
        val gs = ArrayList<Float>()
        val bs = ArrayList<Float>()

        for (i in pixels.indices) {
            val clr = pixels[i]
            val r = ((clr and 0x00ff0000) shr 16).toFloat()
            val g = ((clr and 0x0000ff00) shr 8).toFloat()
            val b = ((clr and 0x000000ff)).toFloat()
            rs.add(r)
            gs.add(g)
            bs.add(b)
        }
        if (!bm.isRecycled) {
            bm.recycle()
        }
        return Triple(rs, gs, bs)
    }

    /**
     * scale bitmap in case of OOM
     */
    fun getScaleBitmap(ctx: Context, filePath: String?): Bitmap {
        Log.d("pml", "getScaleBitmap: filePath: $filePath")
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, opt)

        val bmpWidth = opt.outWidth
        val bmpHeight = opt.outHeight

        val maxSize = getInputSize() * 2

        opt.inSampleSize = 1
        while (true) {
            if (bmpWidth / opt.inSampleSize <= maxSize || bmpHeight / opt.inSampleSize <= maxSize) {
                break
            }
            opt.inSampleSize *= 2
        }
        opt.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, opt)
    }

    override fun setThreadCount(mThreadCounts: Int) {
        PML.setThread(mThreadCounts)
    }

    override fun mixResult(showView: AppCompatImageView, predicted: Pair<FloatArray, Bitmap>) {
        // empty impl
    }

    override fun processInfo(result: FloatArray): String? {
        info { "result.length: " + result.size }

        getTimeInfo()

        var max = java.lang.Float.MIN_VALUE
        var maxi = -1
        var sum = 0f

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


        val resultInfos = StringBuilder()
        for (i in 0..Math.min(1000, result.size - 1)) {
            info { " index: " + i + " value: " + result[i] }
            if (showAllresult) {
                resultInfos.appendln(" index: $i value: ${result[i]}")
            }
            sum += result[i]
            if (result[i] > max) {
                max = result[i]
                maxi = i
            }
        }

        info { "maxindex: $maxi" }
        info { "max: $max" }
        info { "sum: $sum" }


        return "$timeInfo\n" + "result.size: ${result.size}\n" + "${resultInfos}\n"
    }

    private fun getTimeInfo() {
//        timeList.subList(10,timeList.size)

        timeInfo = "运算$mTimes 次平均时间: ${timeList.average()}ms\n最小时间: ${timeList.min()}ms \n最大时间: ${timeList.max()}ms \n所有时间信息:${timeList}"
        info { timeInfo }
        timeList.clear()
    }

    fun clearTimeList() {
        timeList.clear()
    }

    fun predictTimes(times: Long) {
        mTimes = times;
    }
}