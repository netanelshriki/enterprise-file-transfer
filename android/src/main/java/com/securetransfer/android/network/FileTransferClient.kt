package com.securetransfer.android.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class FileTransferClient(private val context: Context) {
    private val logger = LoggerFactory.getLogger(FileTransferClient::class.java)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS) // No timeout for file uploads
        .readTimeout(0, TimeUnit.SECONDS)  // No timeout for file downloads
        .build()
    
    private val _transferProgress = MutableStateFlow<TransferProgress?>(null)
    val transferProgress: StateFlow<TransferProgress?> = _transferProgress
    
    suspend fun sendFile(
        targetHost: String,
        targetPort: Int,
        fileUri: Uri,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            logger.info("Sending file $fileName to $targetHost:$targetPort")
            
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))
            
            val tempFile = File(context.cacheDir, fileName)
            tempFile.outputStream().use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            
            val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaType())
            val progressRequestBody = ProgressRequestBody(requestBody) { bytesWritten, contentLength ->
                _transferProgress.value = TransferProgress(
                    fileName = fileName,
                    bytesTransferred = bytesWritten,
                    totalBytes = contentLength,
                    isComplete = bytesWritten >= contentLength
                )
            }
            
            val request = Request.Builder()
                .url("http://$targetHost:$targetPort/upload")
                .post(progressRequestBody)
                .addHeader("Content-Disposition", "attachment; filename=\"$fileName\"")
                .build()
            
            val response = client.newCall(request).execute()
            
            tempFile.delete() // Clean up temp file
            
            if (response.isSuccessful) {
                logger.info("File sent successfully: $fileName")
                Result.success("File sent successfully")
            } else {
                logger.error("File send failed: ${response.code} ${response.message}")
                Result.failure(Exception("Transfer failed: ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            logger.error("Error sending file", e)
            Result.failure(e)
        }
    }
    
    suspend fun sendText(
        targetHost: String,
        targetPort: Int,
        text: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            logger.info("Sending text to $targetHost:$targetPort")
            
            val requestBody = RequestBody.create("text/plain".toMediaType(), text)
            
            val request = Request.Builder()
                .url("http://$targetHost:$targetPort/text")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                logger.info("Text sent successfully")
                Result.success("Text sent successfully")
            } else {
                logger.error("Text send failed: ${response.code} ${response.message}")
                Result.failure(Exception("Transfer failed: ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            logger.error("Error sending text", e)
            Result.failure(e)
        }
    }
    
    suspend fun pingDevice(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://$host:$port/ping")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful && response.body?.string() == "pong"
        } catch (e: Exception) {
            logger.debug("Ping failed for $host:$port", e)
            false
        }
    }
    
    data class TransferProgress(
        val fileName: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val isComplete: Boolean
    ) {
        val progressPercentage: Float
            get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()) * 100f else 0f
    }
    
    private class ProgressRequestBody(
        private val delegate: RequestBody,
        private val progressListener: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : RequestBody() {
        
        override fun contentType(): MediaType? = delegate.contentType()
        
        override fun contentLength(): Long = delegate.contentLength()
        
        override fun writeTo(sink: okio.BufferedSink) {
            val progressSink = ProgressSink(sink, contentLength(), progressListener)
            val bufferedSink = okio.buffer(progressSink)
            delegate.writeTo(bufferedSink)
            bufferedSink.flush()
        }
    }
    
    private class ProgressSink(
        private val delegate: okio.Sink,
        private val contentLength: Long,
        private val progressListener: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : okio.ForwardingSink(delegate) {
        
        private var bytesWritten = 0L
        
        override fun write(source: okio.Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            progressListener(bytesWritten, contentLength)
        }
    }
}
