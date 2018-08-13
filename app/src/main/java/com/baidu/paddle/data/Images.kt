package com.baidu.paddle.data

import android.os.Environment
import java.io.File
import java.io.IOException

val banana: File
    get() {
        val assetPath = "pml_demo"
        val imagePath = (Environment.getExternalStorageDirectory().toString()
                + File.separator + assetPath)
        val tempFile = File(imagePath, "banana.jpeg")
        try {
            tempFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return tempFile
    }

val hand: File
    get() {
        val assetPath = "pml_demo"
        val imagePath = (Environment.getExternalStorageDirectory().toString()
                + File.separator + assetPath)
        val tempFile = File(imagePath, "hand.jpg")
        try {
            tempFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return tempFile
    }

val hand2: File
    get() {
        val assetPath = "pml_demo"
        val imagePath = (Environment.getExternalStorageDirectory().toString()
                + File.separator + assetPath)
        val tempFile = File(imagePath, "hand2.jpg")
        try {
            tempFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return tempFile
    }

val apple: File
    get() {
        val assetPath = "pml_demo"
        val imagePath = (Environment.getExternalStorageDirectory().toString()
                + File.separator + assetPath)
        val tempFile = File(imagePath, "apple.jpg")
        try {
            tempFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return tempFile
    }

val tempImage: File
    get() {
        val tempFile = File(Environment.getExternalStorageDirectory(), "temp.jpg")
        try {
            tempFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return tempFile
    }

