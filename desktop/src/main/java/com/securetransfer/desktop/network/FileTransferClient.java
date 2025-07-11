package com.securetransfer.desktop.network;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class FileTransferClient {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferClient.class);
    
    private final OkHttpClient client;
    
    public FileTransferClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS) // No timeout for file uploads
                .readTimeout(0, TimeUnit.SECONDS)  // No timeout for file downloads
                .build();
    }
    
    public CompletableFuture<String> sendFile(String targetHost, int targetPort, File file, 
                                            BiConsumer<Long, Long> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Sending file {} to {}:{}", file.getName(), targetHost, targetPort);
                
                RequestBody requestBody = RequestBody.create(file, MediaType.get("application/octet-stream"));
                ProgressRequestBody progressRequestBody = new ProgressRequestBody(requestBody, progressCallback);
                
                Request request = new Request.Builder()
                        .url("http://" + targetHost + ":" + targetPort + "/upload")
                        .post(progressRequestBody)
                        .addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        logger.info("File sent successfully: {}", file.getName());
                        return "File sent successfully";
                    } else {
                        String error = "Transfer failed: " + response.code() + " " + response.message();
                        logger.error(error);
                        throw new RuntimeException(error);
                    }
                }
            } catch (Exception e) {
                logger.error("Error sending file", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    public CompletableFuture<String> sendText(String targetHost, int targetPort, String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Sending text to {}:{}", targetHost, targetPort);
                
                RequestBody requestBody = RequestBody.create(text, MediaType.get("text/plain"));
                
                Request request = new Request.Builder()
                        .url("http://" + targetHost + ":" + targetPort + "/text")
                        .post(requestBody)
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        logger.info("Text sent successfully");
                        return "Text sent successfully";
                    } else {
                        String error = "Transfer failed: " + response.code() + " " + response.message();
                        logger.error(error);
                        throw new RuntimeException(error);
                    }
                }
            } catch (Exception e) {
                logger.error("Error sending text", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    public CompletableFuture<Boolean> pingDevice(String host, int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url("http://" + host + ":" + port + "/ping")
                        .get()
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful() && "pong".equals(response.body().string());
                }
            } catch (Exception e) {
                logger.debug("Ping failed for {}:{}", host, port, e);
                return false;
            }
        });
    }
    
    private static class ProgressRequestBody extends RequestBody {
        private final RequestBody delegate;
        private final BiConsumer<Long, Long> progressListener;
        
        public ProgressRequestBody(RequestBody delegate, BiConsumer<Long, Long> progressListener) {
            this.delegate = delegate;
            this.progressListener = progressListener;
        }
        
        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }
        
        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }
        
        @Override
        public void writeTo(okio.BufferedSink sink) throws IOException {
            ProgressSink progressSink = new ProgressSink(sink, contentLength(), progressListener);
            okio.BufferedSink bufferedSink = okio.Okio.buffer(progressSink);
            delegate.writeTo(bufferedSink);
            bufferedSink.flush();
        }
    }
    
    private static class ProgressSink extends okio.ForwardingSink {
        private final long contentLength;
        private final BiConsumer<Long, Long> progressListener;
        private long bytesWritten = 0L;
        
        public ProgressSink(okio.Sink delegate, long contentLength, BiConsumer<Long, Long> progressListener) {
            super(delegate);
            this.contentLength = contentLength;
            this.progressListener = progressListener;
        }
        
        @Override
        public void write(okio.Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            progressListener.accept(bytesWritten, contentLength);
        }
    }
}
