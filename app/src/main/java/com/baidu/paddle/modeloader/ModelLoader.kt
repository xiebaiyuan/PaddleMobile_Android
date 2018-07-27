package com.baidu.paddle.modeloader

import android.graphics.Bitmap


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

abstract class ModelLoader : IModelLoader {
    var predictImageTime: Long = -1


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
        if (bm.isRecycled) {
            bm.recycle()
        }
        return Triple(rs, gs, bs)
    }

}