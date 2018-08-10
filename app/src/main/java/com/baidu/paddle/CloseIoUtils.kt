package com.baidu.paddle

import java.io.Closeable
import java.io.IOException

/**
 * 关闭io 工具类
 */
object CloseIoUtils {

    /**
     * 关闭IO

     * @param closeables closeables
     */
    fun closeIO(vararg closeables: Closeable?) {
        closeables
                .filterNotNull()
                .forEach {
                    try {
                        it.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
    }

}