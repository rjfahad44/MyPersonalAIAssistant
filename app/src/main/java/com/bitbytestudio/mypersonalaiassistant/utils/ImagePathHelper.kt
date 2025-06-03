package com.bitbytestudio.mypersonalaiassistant.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.*
import androidx.core.net.toUri

class ImagePathHelper(private val context: Context) {

    companion object {
        private const val TAG = "MessageProcessor"
    }

    fun getPathFromUri(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)

            when (uri.scheme) {
                "content" -> {
                    if (DocumentsContract.isDocumentUri(context, uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)

                        // MediaStore documents
                        if (uri.authority == "com.android.providers.media.documents") {
                            val split = docId.split(":")
                            val type = split[0]
                            val id = split[1]
                            val contentUri: Uri? = when (type) {
                                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }

                            val selection = "_id=?"
                            val selectionArgs = arrayOf(id)

                            return getDataColumn(contentUri, selection, selectionArgs)
                        }
                    }

                    // MediaStore (general case)
                    getDataColumn(uri, null, null)
                }

                "file" -> uri.path

                else -> {
                    val file = File(uriString)
                    if (file.exists()) uriString else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting path from URI: $uriString", e)
            null
        }
    }

    @Throws(IOException::class)
    fun copyUriToPrivateFile(uriString: String): String {
        val uri = uriString.toUri()
        val privateDir = context.getExternalFilesDir("images")
            ?: throw IOException("Private directory not available")

        val destFile = File(privateDir, "temp_image_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        } ?: throw IOException("Failed to open URI input stream")

        return destFile.absolutePath
    }

    private fun getDataColumn(
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        try {
            context.contentResolver.query(uri ?: return null, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        return cursor.getString(columnIndex)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting data column", e)
        }

        return null
    }
}
