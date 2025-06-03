package com.bitbytestudio.mypersonalaiassistant.utils

import android.content.Context
import android.util.Log
import com.nexa.NexaOmniVlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class LlamaBridge(
    private val context: Context,
    private val messageHandler: MessageHandler
) {

    companion object {
        private const val TAG = "LlamaBridge"
        private const val DEFAULT_TEMPERATURE = 1.0f
        private const val DEFAULT_MAX_TOKENS = 64
        private const val DEFAULT_TOP_K = 50
        private const val DEFAULT_TOP_P = 0.9f
    }

    private val modelManager = VlmModelManager(context)
    private val imagePathHelper = ImagePathHelper(context)
    private val flowHelper = KotlinFlowHelper()

    private var nexaVlmInference: NexaOmniVlmInference? = null
    private var isModelLoaded: Boolean = false

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    interface InferenceCallback {
        fun onStart()
        fun onToken(token: String)
        fun onComplete(fullResponse: String)
        fun onError(error: String)
    }

    fun areModelsAvailable(): Boolean {
        return modelManager.areModelsAvailable()
    }

    fun loadModel() {
        coroutineScope.launch {
            try {
                if (!modelManager.areModelsAvailable()) {
                    throw IOException("Required model files are not available")
                }

                val modelPath = modelManager.textModelPath
                val projectorPath = modelManager.mmProjModelPath

                Log.d(TAG, "Loading model from: $modelPath")
                Log.d(TAG, "Loading projector from: $projectorPath")

                nexaVlmInference = NexaOmniVlmInference(
                    modelPath = modelPath.toString(),
                    projectorPath = projectorPath.toString(),
                    imagePath = "",
                    stopWords = arrayListOf("</s>"),
                    temperature = DEFAULT_TEMPERATURE,
                    maxNewTokens = DEFAULT_MAX_TOKENS,
                    topK = DEFAULT_TOP_K,
                    topP = DEFAULT_TOP_P
                ).apply {
                    loadModel()
                }

                isModelLoaded = true
                Log.d(TAG, "Model loaded successfully.")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model", e)
                withContext(Dispatchers.Main) {
                    messageHandler.addMessage(
                        MessageModal("Error loading model: ${e.message}", "assistant", null)
                    )
                }
            }
        }
    }

    fun processMessage(message: String, imageUri: String, callback: InferenceCallback) {
        if (!isModelLoaded) {
            callback.onError("Model not loaded yet")
            return
        }

        coroutineScope.launch {
            val imagePath = try {
                imagePathHelper.copyUriToPrivateFile(imageUri)
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    callback.onError("Failed to process image: ${e.message}")
                }
                return@launch
            }

            val assistantMessage = MessageModal("", "bot", null)
            withContext(Dispatchers.Main) {
                messageHandler.addMessage(assistantMessage)
                callback.onStart()
            }

            try {
                val startTime = System.currentTimeMillis()
                var firstTokenTime = 0L
                var tokenCount = 0
                val fullResponse = StringBuilder()

                val flow = nexaVlmInference?.createCompletionStream(
                    message,
                    imagePath,
                    arrayListOf("</s>"),
                    DEFAULT_TEMPERATURE,
                    DEFAULT_MAX_TOKENS,
                    DEFAULT_TOP_K,
                    DEFAULT_TOP_P
                ) ?: run {
                    withContext(Dispatchers.Main) {
                        callback.onError("Failed to create completion stream")
                    }
                    return@launch
                }

                flow.collect { token ->
                    if (tokenCount == 0) {
                        firstTokenTime = System.currentTimeMillis() - startTime
                    }
                    tokenCount++
                    fullResponse.append(token)

                    withContext(Dispatchers.Main) {
                        callback.onToken(token)
                    }
                }

                val totalTime = System.currentTimeMillis() - startTime
                val tokensPerSecond = tokenCount / (totalTime / 1000.0)
                val decodingTime = totalTime - firstTokenTime
                val decodingSpeed = if (decodingTime > 0) (tokenCount - 1) / (decodingTime / 1000.0) else 0.0

                assistantMessage.ttft = firstTokenTime
                assistantMessage.tps = tokensPerSecond
                assistantMessage.decodingSpeed = decodingSpeed
                assistantMessage.totalTokens = tokenCount

                withContext(Dispatchers.Main) {
                    callback.onComplete(fullResponse.toString())
                }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Stream collection failed", e)
                    withContext(Dispatchers.Main) {
                        callback.onError("Stream collection failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun cleanup() {
        flowHelper.cancel()
    }

    fun shutdown() {
        coroutineScope.launch {
            try {
                nexaVlmInference?.dispose()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing inference", e)
            }
            nexaVlmInference = null
            isModelLoaded = false
        }
        coroutineScope.cancel()
    }
}
