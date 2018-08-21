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

class GEnetModelLoaderCombinedImpl : ModelLoader() {

    private var type = ModelType.genet_combine
    // mobile net is bgr
    private val means = floatArrayOf(128f, 128f, 128f)
    private val ddims = intArrayOf(1, 3, 128, 128)
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
                else -> (rs[i - bs.size - gs.size] - means[2]) * scale
            }
        }

        return dataBuf
    }


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
  //      val modelPath = sdcardPath + File.separator + "genetmodel.mlm"
        val paramsPath = sdcardPath + File.separator + "params"

        Log.d("pml", "loadpath : $sdcardPath")
        Log.d("pml", "modelPath : $modelPath")
        Log.d("pml", "paramsPath : $paramsPath")

        val key =  "#w\$`-3>Yyzue5f%3a3zY@_)wYZ1&c5Nlh#lUmy+K+;O7uqMRra";
        // PML.loadCombinedEncrypt(modelPath, paramsPath,key)
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
        return predictImage(getScaledMatrix(bitmap, getInputSize(), getInputSize()))
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
        // l r t b
        val l: Float = floats[0] * w
        val r: Float = floats[1] * w

        val t: Float = floats[2] * h
        val b: Float = floats[3] * h
        info {
            "l= $l r= $r t= $t b= $b "
        }
        info {
            " bitmap.width = ${newb.width}  " +
                    " bitmap.height = ${newb.height} " +
                    " showView.width = ${showView.width} " +
                    " showView.height = ${showView.height}  "
        }

        cv.drawRect(l, t, r, b, paint)

        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG)//保存
        //store
        cv.restore()//存储

        showView.setImageBitmap(newb)

//        try {
//            val bitmap = predicted.second
//            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//
//
//            val floats = predicted.first
//            val paint = Paint()
//            paint.color = Color.RED
//            paint.style = Paint.Style.STROKE
//            paint.strokeWidth = 3.0f
//           val canvas = Canvas(mutableBitmap)
////            canvas.drawBitmap(mutableBitmap,0f,0f,paint)
//            info {
//                " bitmap.width = ${bitmap.width}  " +
//                        " bitmap.height = ${bitmap.height} " +
//                        " showView.width = ${showView.width} " +
//                        " showView.height = ${showView.height}  "
//
//
//            }
//            val x1: Float = floats[0] * showView.width
//            val x2: Float = floats[2] * showView.width
//            val y1: Float = floats[1] * showView.height
//            val y2: Float = floats[3] * showView.height
//            info {
//                "x1= $x1 "+
//                "x2= $x2 "+
//                "y1= $y1 "+
//                "y2= $y2 "
//
//            }
//            canvas.drawRect(x1, y1, x2, y2, paint)
//
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }
//    override fun mixResult(canvas: AppCompatImageView, predicted: Pair<FloatArray, Bitmap>, viewWidth: Int, viewHeight: Int) {
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
//    }

}