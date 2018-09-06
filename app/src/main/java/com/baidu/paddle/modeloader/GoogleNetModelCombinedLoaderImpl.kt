package com.baidu.paddle.modeloader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import android.support.v7.widget.AppCompatImageView
import android.util.Log
import com.baidu.paddle.PML
import org.jetbrains.anko.info
import java.io.File


/**
 * Created by xiebaiyuan on 2018/7/18.
 */

class GoogleNetModelCombinedLoaderImpl : ModelLoader() {


    private var type = ModelType.googlenet_combine
    private val means = floatArrayOf(148.0f, 148.0f, 148.0f)
    private val ddims = intArrayOf(1, 3, 224, 224)
    private val scale = 1


    override fun getInputSize(): Int {
        return ddims[2]
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
                else -> (bs[i - rs.size - gs.size] - means[2]) * scale
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
    override fun mixResult(showView: AppCompatImageView, predicted: Pair<FloatArray, Bitmap>) {


        val src = predicted.second
        val floats = predicted.first

        val w = showView.width
        val h = showView.height

        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.0f

        //create the new blank bitmap
        val newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)//创建一个新的和SRC长度宽度一样的位图
        val cv = Canvas(newb)
        //draw src into
        cv.drawBitmap(src, 0f, 0f, null)//在 0，0坐标开始画入src
        // l t r b
//        val l: Float = floats[0]*w
//        val t: Float = floats[1]*h
//
//        val r: Float = floats[2]*w
//        val b: Float = floats[3]*h
//

        var x1 = 0f
        var x2 = 0f
        var y1 = 0f
        var y2 = 0f

        // the googlenet result sequence is (left top right top bottom)
        x1 = floats[0] * showView.width / 224
        y1 = floats[1] * showView.height / 224
        x2 = floats[2] * showView.width / 224
        y2 = floats[3] * showView.height / 224




//
//        info {
//            "l= $l r= $r t= $t b= $b "
//        }
        info {
            " bitmap.width = ${newb.width}  " +
                    " bitmap.height = ${newb.height} " +
                    " showView.width = ${showView.width} " +
                    " showView.height = ${showView.height}  "
        }
        cv.drawRect(x1, y1, x2, y2, paint)

//        cv.drawRect(l, t, r, b, paint)

        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG)//保存
        //store
        cv.restore()//存储

        showView.setImageBitmap(newb)
        super.mixResult(showView, predicted)
    }

//    override fun mixResult(canvas: AppCompatImageView, predicted: Pair<FloatArray, Bitmap>, viewWidth: Int, viewHeight: Int) {
//        val paint = Paint()
//        paint.color = Color.RED
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = 3.0f
//        val x1: Float = predicted[0] * viewWidth / 224
//        val x2: Float = predicted[2] * viewWidth / 224
//        val y1: Float = predicted[1] * viewHeight / 224
//        val y2: Float = predicted[3] * viewHeight / 224
//        canvas.mixResult(x1, y1, x2, y2, paint)
//    }


    override fun predictImage(bitmap: Bitmap): FloatArray? {
        val buf = getScaledMatrix(bitmap, getInputSize(), getInputSize())
        return predictImage(buf)
    }


}