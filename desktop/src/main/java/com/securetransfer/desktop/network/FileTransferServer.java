package com.securetransfer.desktop.network;

import com.securetransfer.desktop.model.TransferSession;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileTransferServer {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferServer.class);
    private static final int PORT = 8080;
    
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, TransferSession> activeSessions = new ConcurrentHashMap<>();
    
    public void start() throws InterruptedException {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting file transfer server on port {}", PORT);
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new FileTransferHandler(activeSessions));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture future = bootstrap.bind(PORT).sync();
            logger.info("File transfer server started successfully");
            
            future.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                logger.info("File transfer server stopped");
                running.set(false);
            });
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping file transfer server");
            
            activeSessions.values().forEach(TransferSession::cancel);
            activeSessions.clear();
            
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public void addTransferSession(String sessionId, TransferSession session) {
        activeSessions.put(sessionId, session);
    }
    
    public TransferSession getTransferSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    public void removeTransferSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
}
