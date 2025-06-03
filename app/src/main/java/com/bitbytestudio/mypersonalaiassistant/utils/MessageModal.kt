package com.bitbytestudio.mypersonalaiassistant.utils

data class MessageModal(
    var message: String,
    var sender: String,
    var imageUri: String? = null,
    var ttft: Long = 0L,
    var tps: Double = 0.0,
    var decodingSpeed: Double = 0.0,
    var totalTokens: Int = 0
)
