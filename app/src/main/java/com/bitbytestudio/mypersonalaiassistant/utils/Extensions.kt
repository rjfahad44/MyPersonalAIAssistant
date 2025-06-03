package com.bitbytestudio.mypersonalaiassistant.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun copyAssetToFile(context: Context, assetName: String, targetFileName: String): File {
    val outFile = File(context.filesDir, targetFileName)
    if (!outFile.exists()) {
        context.assets.open(assetName).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return outFile
}

suspend fun Context.downloadModelToFile(url: String, fileName: String): File {
    val file = File(cacheDir, fileName)
    URL(url).openStream().use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}