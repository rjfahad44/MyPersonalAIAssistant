package com.bitbytestudio.mypersonalaiassistant.utils

import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageHandler(
    private val messageList: SnapshotStateList<MessageModal>
) {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun addMessage(message: MessageModal) {
        runOnMain {
            messageList.add(message)
        }
    }

    fun updateLastBotMessage(newToken: String) {
        runOnMain {
            val lastIndex = messageList.lastIndex
            if (lastIndex >= 0) {
                val lastMessage = messageList[lastIndex]
                if (lastMessage.sender == "bot") {
                    messageList[lastIndex] = lastMessage.copy(
                        message = lastMessage.message + newToken
                    )
                } else {
                    messageList.add(MessageModal(newToken, "bot"))
                }
            }
        }
    }

    fun finalizeLastBotMessage(completeMessage: String) {
        runOnMain {
            val lastIndex = messageList.lastIndex
            if (lastIndex >= 0) {
                val lastMessage = messageList[lastIndex]
                if (lastMessage.sender == "bot") {
                    messageList[lastIndex] = lastMessage.copy(
                        message = completeMessage
                    )
                } else {
                    messageList.add(MessageModal(completeMessage, "bot"))
                }
            }
        }
    }

    fun clearMessages() {
        runOnMain {
            messageList.clear()
        }
    }

    fun getLastMessage(): MessageModal? = messageList.lastOrNull()

    fun isLastMessageFromBot(): Boolean = getLastMessage()?.sender == "bot"

    private fun runOnMain(action: () -> Unit) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            action()
        } else {
            mainScope.launch {
                action()
            }
        }
    }
}
