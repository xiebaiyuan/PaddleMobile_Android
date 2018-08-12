package com.baidu.paddle.modeloader

import android.graphics.Bitmap
import android.graphics.Canvas


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

interface IModelLoader {
    fun load()

    fun clear()

    fun getInputSize(): Int

    fun getScaledMatrix(bitmap: Bitmap, desWidth: Int, desHeight: Int): FloatArray

    fun predictImage(inputBuf: FloatArray): FloatArray?

    fun drawRect(canvas: Canvas, predicted: FloatArray, viewWidth: Int, viewHeight: Int)

    fun predictImage(bitmap: Bitmap): FloatArray?

    fun setThreadCount(mThreadCounts: Int)

}