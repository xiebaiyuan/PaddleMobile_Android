package com.baidu.paddle.modeloader

import android.graphics.Bitmap
import android.support.v7.widget.AppCompatImageView


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

interface IModelLoader {
    fun load()

    fun clear()

    fun getInputSize(): Int

    fun getScaledMatrix(bitmap: Bitmap, desWidth: Int, desHeight: Int): FloatArray

    fun predictImage(inputBuf: FloatArray): FloatArray?

    fun mixResult(showView: AppCompatImageView, predicted: Pair<FloatArray, Bitmap>)

    fun predictImage(bitmap: Bitmap): FloatArray?

    fun setThreadCount(mThreadCounts: Int)

}