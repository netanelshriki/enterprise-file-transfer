package com.securetransfer.desktop.network;

import com.securetransfer.desktop.model.TransferSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class FileTransferHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferHandler.class);
    
    private final ConcurrentHashMap<String, TransferSession> activeSessions;
    
    public FileTransferHandler(ConcurrentHashMap<String, TransferSession> activeSessions) {
        this.activeSessions = activeSessions;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        
        if (request.method() != HttpMethod.GET && request.method() != HttpMethod.POST) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        
        String uri = request.uri();
        logger.debug("Received request: {} {}", request.method(), uri);
        
        if (uri.startsWith("/download/")) {
            handleDownload(ctx, request, uri);
        } else if (uri.startsWith("/upload/")) {
            handleUpload(ctx, request, uri);
        } else if (uri.equals("/ping")) {
            handlePing(ctx);
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
        }
    }
    
    private void handleDownload(ChannelHandlerContext ctx, FullHttpRequest request, String uri) {
        String sessionId = uri.substring("/download/".length());
        TransferSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }
        
        try {
            File file = session.getFile();
            if (!file.exists() || !file.isFile()) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpUtil.setContentLength(response, fileLength);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, 
                                 "attachment; filename=\"" + file.getName() + "\"");
            
            if (HttpUtil.isKeepAlive(request)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            
            ctx.write(response);
            
            ChannelFuture sendFileFuture = ctx.write(new ChunkedFile(raf, 0, fileLength, 8192),
                                                   ctx.newProgressivePromise());
            
            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                    if (total < 0) {
                        logger.debug("Transfer progress: {}", progress);
                    } else {
                        logger.debug("Transfer progress: {}/{}", progress, total);
                    }
                    session.updateProgress(progress, total);
                }
                
                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    logger.info("Transfer complete for session: {}", sessionId);
                    session.complete();
                }
            });
            
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            
            if (!HttpUtil.isKeepAlive(request)) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
            
        } catch (Exception e) {
            logger.error("Error handling download", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private void handleUpload(ChannelHandlerContext ctx, FullHttpRequest request, String uri) {
        sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }
    
    private void handlePing(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, 
                                                              Unpooled.copiedBuffer("pong", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                                                              Unpooled.copiedBuffer("Error: " + status, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in file transfer handler", cause);
        ctx.close();
    }
}
