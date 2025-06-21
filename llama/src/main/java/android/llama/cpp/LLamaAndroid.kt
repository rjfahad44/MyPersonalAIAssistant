package android.llama.cpp

import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.concurrent.thread


// TODO(Original LlamaAndroid Class)
//class LLamaAndroid {
//    @Volatile
//    private var cancelRequested = false
//    private val tag: String? = this::class.simpleName
//
//    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }
//
//    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
//        thread(start = false, name = "Llm-RunLoop") {
//            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")
//
//            // No-op if called more than once.
//            //System.loadLibrary("llama-android")
//            System.loadLibrary("MyPersonalAIAssistant")
//            // Set llama log handler to Android
//            log_to_android()
//            backend_init(false)
//
//            Log.d(tag, system_info())
//
//            it.run()
//        }.apply {
//            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
//                Log.e(tag, "Unhandled exception", exception)
//            }
//        }
//    }.asCoroutineDispatcher()
//
//    private val nlen: Int = 256
//
//    private external fun log_to_android()
//    private external fun load_model(filename: String): Long
//    private external fun free_model(model: Long)
//    private external fun new_context(model: Long): Long
//    private external fun free_context(context: Long)
//    private external fun backend_init(numa: Boolean)
//    private external fun backend_free()
//    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
//    private external fun free_batch(batch: Long)
//    private external fun new_sampler(): Long
//    private external fun free_sampler(sampler: Long)
//    private external fun bench_model(
//        context: Long,
//        model: Long,
//        batch: Long,
//        pp: Int,
//        tg: Int,
//        pl: Int,
//        nr: Int,
//    ): String
//
//    private external fun system_info(): String
//
//    private external fun completion_init(
//        context: Long,
//        batch: Long,
//        text: String,
//        formatChat: Boolean,
//        nLen: Int,
//    ): Int
//
//    private external fun completion_loop(
//        context: Long,
//        batch: Long,
//        sampler: Long,
//        nLen: Int,
//        ncur: IntVar,
//    ): String?
//
//    private external fun kv_cache_clear(context: Long)
//
//    suspend fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1): String {
//        return withContext(runLoop) {
//            when (val state = threadLocalState.get()) {
//                is State.Loaded -> {
//                    Log.d(tag, "bench(): $state")
//                    bench_model(state.context, state.model, state.batch, pp, tg, pl, nr)
//                }
//
//                else -> throw IllegalStateException("No model loaded")
//            }
//        }
//    }
//
//    suspend fun load(pathToModel: String) {
//        withContext(runLoop) {
//            when (threadLocalState.get()) {
//                is State.Idle -> {
//                    val model = load_model(pathToModel)
//                    if (model == 0L) throw IllegalStateException("load_model() failed")
//
//                    val context = new_context(model)
//                    if (context == 0L) throw IllegalStateException("new_context() failed")
//
//                    val batch = new_batch(512, 0, 1) //512
//                    if (batch == 0L) throw IllegalStateException("new_batch() failed")
//
//                    val sampler = new_sampler()
//                    if (sampler == 0L) throw IllegalStateException("new_sampler() failed")
//
//                    Log.i(tag, "Loaded model $pathToModel")
//                    threadLocalState.set(State.Loaded(model, context, batch, sampler))
//                }
//
//                else -> throw IllegalStateException("Model already loaded")
//            }
//        }
//    }
//
//    fun send(message: String, formatChat: Boolean = false): Flow<String> = flow {
//        cancelRequested = false
//        when (val state = threadLocalState.get()) {
//            is State.Loaded -> {
//                val ncur = IntVar(completion_init(state.context, state.batch, message, formatChat, nlen))
//                while (ncur.value <= nlen && !cancelRequested) {
//                    val str = completion_loop(state.context, state.batch, state.sampler, nlen, ncur)
//                    if (str.isNullOrEmpty()) break
//                    emit(str)
//                }
//                //kv_cache_clear(state.context)
//            }
//
//            else -> {
//                emit("❌ Model is not loaded. Please try again later.")
//            }
//        }
//    }.flowOn(runLoop)
//
//
//    fun stopThinking() {
//        cancelRequested = true
//        when (val state = threadLocalState.get()) {
//            is State.Loaded -> {
//                kv_cache_clear(state.context)
//                free_context(state.context)
//                free_model(state.model)
//                free_batch(state.batch)
//                free_sampler(state.sampler)
//                backend_free()
//            }
//            else -> {}
//        }
//    }
//
//
//    /**
//     * Unloads the model and frees resources.
//     *
//     * This is a no-op if there's no model loaded.
//     */
//    suspend fun unload() {
//        withContext(runLoop) {
//            when (val state = threadLocalState.get()) {
//                is State.Loaded -> {
//                    free_context(state.context)
//                    free_model(state.model)
//                    free_batch(state.batch)
//                    free_sampler(state.sampler)
//
//                    threadLocalState.set(State.Idle)
//                }
//
//                else -> {}
//            }
//        }
//    }
//
//    companion object {
//        private class IntVar(value: Int) {
//            @Volatile
//            var value: Int = value
//                private set
//
//            fun inc() {
//                synchronized(this) {
//                    value += 1
//                }
//            }
//        }
//
//        private sealed interface State {
//            data object Idle : State
//            data class Loaded(
//                val model: Long,
//                val context: Long,
//                val batch: Long,
//                val sampler: Long,
//            ) : State
//        }
//
//        // Enforce only one instance of Llm.
//        private val _instance: LLamaAndroid = LLamaAndroid()
//
//        fun instance(): LLamaAndroid = _instance
//    }
//}


// TODO(Modified LlamaAndroid Class)
//class LLamaAndroid {
//    @Volatile
//    private var cancelRequested = false
//    private val tag: String? = this::class.simpleName
//
//    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }
//
//    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
//        thread(start = false, name = "Llm-RunLoop") {
//            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")
//            System.loadLibrary("MyPersonalAIAssistant")
//            log_to_android()
//            backend_init(false)
//            Log.d(tag, system_info())
//            it.run()
//        }.apply {
//            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
//                Log.e(tag, "Unhandled exception", exception)
//            }
//        }
//    }.asCoroutineDispatcher()
//
//    private val nlen: Int = 512
//
//    private external fun log_to_android()
//    private external fun load_model(filename: String): Long
//    private external fun free_model(model: Long)
//    private external fun new_context(model: Long): Long
//    private external fun free_context(context: Long)
//    private external fun backend_init(numa: Boolean)
//    private external fun backend_free()
//    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
//    private external fun free_batch(batch: Long)
//    private external fun new_sampler(): Long
//    private external fun free_sampler(sampler: Long)
//    private external fun bench_model(context: Long, model: Long, batch: Long, pp: Int, tg: Int, pl: Int, nr: Int): String
//    private external fun system_info(): String
//    private external fun completion_init(context: Long, batch: Long, text: String, formatChat: Boolean, nLen: Int): Int
//    private external fun completion_loop(context: Long, batch: Long, sampler: Long, nLen: Int, ncur: IntVar): String?
//    private external fun kv_cache_clear(context: Long)
//
//    suspend fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1): String = withContext(runLoop) {
//        when (val state = threadLocalState.get()) {
//            is State.Loaded -> bench_model(state.context, state.model, state.batch, pp, tg, pl, nr)
//            else -> throw IllegalStateException("No model loaded")
//        }
//    }
//
//    suspend fun load(pathToModel: String) {
//        withContext(runLoop) {
//            when (threadLocalState.get()) {
//                is State.Idle -> {
//                    val model = load_model(pathToModel)
//                    if (model == 0L) throw IllegalStateException("load_model() failed")
//                    val context = new_context(model)
//                    if (context == 0L) throw IllegalStateException("new_context() failed")
////                    val batch = new_batch(512, 0, 1) //512
////                    if (batch == 0L) throw IllegalStateException("new_batch() failed")
////                    val sampler = new_sampler()
////                    if (sampler == 0L) throw IllegalStateException("new_sampler() failed")
//                    Log.i(tag, "Loaded model $pathToModel")
//                    threadLocalState.set(State.Loaded(model, context))
//                }
//                else -> {
//                    throw IllegalStateException("Model already loaded")
//                }
//            }
//        }
//    }
//
//    fun send(message: String, formatChat: Boolean = false): Flow<String> = flow {
//        cancelRequested = false
//        val state = threadLocalState.get()
//        if (state !is State.Loaded) {
//            emit("❌ Model is not loaded. Please try again later.")
//            return@flow
//        }
//
//        val batch = new_batch(nlen, 0, 1)
//        if (batch == 0L) throw IllegalStateException("new_batch() failed")
//        val sampler = new_sampler()
//        if (sampler == 0L) {
//            free_batch(batch)
//            throw IllegalStateException("new_sampler() failed")
//        }
//
//        try {
//            val ncur = IntVar(completion_init(state.context, batch, message, formatChat, nlen))
//
//            while (!cancelRequested) {
//                val token = completion_loop(state.context, batch, sampler, nlen, ncur)
//                if (token.isNullOrEmpty()) {
//                    break // End of response
//                }
//                emit(token)
//            }
//
//        } finally {
//            kv_cache_clear(state.context)
//            free_batch(batch)
//            free_sampler(sampler)
//        }
//    }.flowOn(runLoop)
//
//    fun stopThinking() {
//        cancelRequested = true
//    }
//
//    suspend fun unload() {
//        withContext(runLoop) {
//            when (val state = threadLocalState.get()) {
//                is State.Loaded -> {
//                    free_context(state.context)
//                    free_model(state.model)
//                    threadLocalState.set(State.Idle)
//                }
//                else -> {}
//            }
//        }
//    }
//
//    companion object {
//        private class IntVar(value: Int) {
//            @Volatile
//            var value: Int = value
//                private set
//            fun inc() {
//                synchronized(this) { value += 1 }
//            }
//        }
//
//        private sealed interface State {
//            data object Idle : State
//            data class Loaded(val model: Long,
//                              val context: Long,
//                              val batch: Long = 512,
//                              val sampler: Long = 0,) : State
//        }
//
//        private val _instance: LLamaAndroid = LLamaAndroid()
//        fun instance(): LLamaAndroid = _instance
//    }
//}







@Keep
class LLamaAndroid {
    @Volatile
    private var cancelRequested = false
    private val tag = this::class.simpleName
    private val modelMaxTokens = 4096
    private var modelName: String = "DEFAULT"

    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }

    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")
            System.loadLibrary("MyPersonalAIAssistant")
            log_to_android()
            backend_init(false)
            Log.d(tag, system_info())
            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception ->
                Log.e(tag, "Unhandled exception", exception)
            }
        }
    }.asCoroutineDispatcher()

    // Native functions
    private external fun log_to_android()
    private external fun load_model(filename: String): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)
    private external fun new_sampler(): Long
    private external fun free_sampler(sampler: Long)
    private external fun bench_model(context: Long, model: Long, batch: Long, pp: Int, tg: Int, pl: Int, nr: Int): String
    private external fun system_info(): String
    private external fun completion_init(context: Long, batch: Long, text: String, formatChat: Boolean, nLen: Int): Int
    private external fun completion_loop(context: Long, batch: Long, sampler: Long, nLen: Int, ncur: IntVar): String?
    private external fun kv_cache_clear(context: Long)

    private val chatHistory = mutableListOf<ChatMessage>().apply {
        add(ChatMessage(Role.SYSTEM, "You are a helpful AI assistant."))
    }

    fun setSystemPrompt(prompt: String) {
        chatHistory.removeAll { it.role == Role.SYSTEM }
        chatHistory.add(0, ChatMessage(Role.SYSTEM, prompt))
    }

    fun clearHistory() {
        chatHistory.retainAll { it.role == Role.SYSTEM }
    }

    private fun trimOldMessages(maxTokens: Int = 2048) {
        while (estimateTokenCount(formatChatHistory()) > maxTokens && chatHistory.size > 2) {
            chatHistory.removeAt(1)
        }
    }

    private fun formatChatHistory(): String {
        return when (modelName) {
            "QWEN", "DEEPSEEK" -> {
                buildString {
                    chatHistory.forEach { msg ->
                        when (msg.role) {
                            Role.SYSTEM -> {
                                append("<|im_start|>system\n")
                                append(msg.content.trim())
                                append("\n<|im_end|>\n")
                            }
                            Role.USER -> {
                                append("<|im_start|>user\n")
                                append(msg.content.trim())
                                append("\n<|im_end|>\n")
                            }
                            Role.ASSISTANT -> {
                                append("<|im_start|>assistant\n")
                                append(msg.content.trim())
                                append("\n<|im_end|>\n")
                            }
                        }
                    }
                    append("<|im_start|>user\n")
                }
            }

            "LLAMA" -> {
                buildString {
                    chatHistory.forEach { msg ->
                        when (msg.role) {
                            Role.SYSTEM -> {
                                append("[INST] <<SYS>>\n${msg.content.trim()}\n<</SYS>>\n\n")
                            }
                            Role.USER -> {
                                append("[INST] ${msg.content.trim()} [/INST]\n")
                            }
                            Role.ASSISTANT -> {
                                append("${msg.content.trim()}\n")
                            }
                        }
                    }
                    append("[INST] ")
                }
            }

            "GEMMA" -> {
                buildString {
                    chatHistory.forEach { msg ->
                        when (msg.role) {
                            Role.SYSTEM, Role.USER -> {
                                append("User: ${msg.content.trim()}\n")
                                append("Assistant:\n")
                            }
                            Role.ASSISTANT -> {
                                append("${msg.content.trim()}\n")
                            }
                        }
                    }
                    append("User: ")
                }
            }

            else -> {
                buildString {
                    chatHistory.forEach { msg ->
                        val rolePrefix = when (msg.role) {
                            Role.SYSTEM -> "System"
                            Role.USER -> "User"
                            Role.ASSISTANT -> "Assistant"
                        }
                        append("$rolePrefix: ${msg.content.trim()}\n")
                    }
                    append("User: ")
                }
            }
        }
    }



    private fun estimateTokenCount(text: String): Int {
        val wordCount = text.trim().split("\\s+".toRegex()).size
        return (wordCount / 0.75).toInt().coerceAtLeast(1)
    }

    suspend fun load(pathToModel: String) {
        val name = File(pathToModel).nameWithoutExtension
        modelName = when {
            name.contains("qwen", ignoreCase = true) -> "QWEN"
            name.contains("deepseek", ignoreCase = true) -> "DEEPSEEK"
            name.contains("llama", ignoreCase = true) -> "LLAMA"
            name.contains("gemma", ignoreCase = true) -> "GEMMA"
            else -> "DEFAULT"
        }

        Log.i(tag, "Loaded model fullName: $name")
        Log.i(tag, "Loaded model name Key: $modelName")

        withContext(runLoop) {
            if (threadLocalState.get() != State.Idle) throw IllegalStateException("Model already loaded")

            val model = load_model(pathToModel)
            require(model != 0L) { "load_model() failed" }

            val context = new_context(model)
            require(context != 0L) { "new_context() failed" }

            threadLocalState.set(State.Loaded(model, context))
            Log.i(tag, "Loaded model from $pathToModel")
        }
    }

    suspend fun unload() {
        withContext(runLoop) {
            (threadLocalState.get() as? State.Loaded)?.let {
                free_context(it.context)
                free_model(it.model)
                threadLocalState.set(State.Idle)
                Log.i(tag, "Model unloaded")
            }
        }
    }

    suspend fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1): String = withContext(runLoop) {
        val state = threadLocalState.get() as? State.Loaded
            ?: throw IllegalStateException("No model loaded")
        bench_model(state.context, state.model, new_batch(256, 0, 1), pp, tg, pl, nr)
    }

    fun send(message: String, formatChat: Boolean = true): Flow<String> = flow {
        cancelRequested = false
        val state = threadLocalState.get()
        if (state !is State.Loaded) {
            emit("❌ Model is not loaded.")
            return@flow
        }

        chatHistory.add(ChatMessage(Role.USER, message))
        trimOldMessages()
        val prompt = formatChatHistory()
        val promptTokens = estimateTokenCount(prompt)
        val nlen = (modelMaxTokens - promptTokens).coerceIn(64, 1024)

        val batch = new_batch(nlen, 0, 1)
        require(batch != 0L) { "new_batch() failed" }

        val sampler = new_sampler()
        if (sampler == 0L) {
            free_batch(batch)
            throw IllegalStateException("new_sampler() failed")
        }

        try {
            val ncur = IntVar(completion_init(state.context, batch, prompt, formatChat, nlen))
            val reply = StringBuilder()

            while (!cancelRequested) {
                val token = completion_loop(state.context, batch, sampler, nlen, ncur)
                if (token.isNullOrEmpty()) break
                reply.append(token)
                emit(token)
            }

            chatHistory.add(ChatMessage(Role.ASSISTANT, reply.toString()))
        } finally {
            kv_cache_clear(state.context)
            free_batch(batch)
            free_sampler(sampler)
        }
    }.flowOn(runLoop)

    fun stopThinking() {
        cancelRequested = true
    }

    private class IntVar(value: Int) {
        @Volatile var value: Int = value
            private set
        fun inc() = synchronized(this) { value += 1 }
    }

    @Keep
    private sealed interface State {
        data object Idle : State
        data class Loaded(val model: Long, val context: Long) : State
    }

    @Keep
    data class ChatMessage(val role: Role, val content: String)
    enum class Role { SYSTEM, USER, ASSISTANT }

    companion object {
        private val _instance = LLamaAndroid()
        fun instance(): LLamaAndroid = _instance
    }
}

