package com.bitbytestudio.mypersonalaiassistant.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

class VlmModelManager(private val context: Context) {

    companion object {
        private const val TAG = "LlamaBridge"
        private const val MODELS_DIR = "models"
        private const val MODEL_TEXT_FILENAME = "model-q8_0.gguf"
        private const val MODEL_MMPROJ_FILENAME = "projector-fp16.gguf"
    }

    var textModelPath: File? = null
    var mmProjModelPath: File? = null

    private val externalModelDir: File
        get() = File(
            Environment.getExternalStorageDirectory(),
            "Android/data/${context.packageName}/files"
        )

    private fun findExistingModel(filename: String): String? {
        val possibleLocations = listOf(
            File(externalModelDir, filename),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename),
            File(context.getExternalFilesDir(null), "$MODELS_DIR/$filename"),
            File(context.filesDir, "$MODELS_DIR/$filename")
        )

        for (location in possibleLocations) {
            if (location.exists() && location.canRead()) {
                Log.d(TAG, "Found model at: ${location.absolutePath}")
                return location.absolutePath
            }
        }

        return null
    }

    @Throws(IOException::class)
    fun getTextModelPath(): String {
        textModelPath?.takeIf { it.exists() && it.canRead() }?.let {
            return it.absolutePath
        }

        return findExistingModel(MODEL_TEXT_FILENAME)?.also {
            textModelPath = File(it)
        } ?: throw IOException("Text model not found")
    }

    @Throws(IOException::class)
    fun getMmProjModelPath(): String {
        mmProjModelPath?.takeIf { it.exists() && it.canRead() }?.let {
            return it.absolutePath
        }

        return findExistingModel(MODEL_MMPROJ_FILENAME)?.also {
            mmProjModelPath = File(it)
        } ?: throw IOException("MMProj model not found")
    }

    fun areModelsAvailable(): Boolean {
        return try {
            getTextModelPath()
            getMmProjModelPath()
            true
        } catch (e: IOException) {
            Log.w(TAG, "Models not available: ${e.message}")
            false
        }
    }

    fun getModelsDirectory(): File? {
        return try {
            File(getTextModelPath()).parentFile
        } catch (e: IOException) {
            Log.w(TAG, "Could not get models directory: ${e.message}")
            null
        }
    }
}
