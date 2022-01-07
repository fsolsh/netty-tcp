package com.fsolsh.netty;

import com.fsolsh.netty.common.FixedLengthFrameEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class FixedLengthServer {

    public static void main(String[] args) throws InterruptedException {
        new FixedLengthServer().bind(8080);
    }

    public void bind(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 这里将FixedLengthFrameDecoder添加到pipeline中，指定长度为2000
                            ch.pipeline().addLast(new FixedLengthFrameDecoder(2000));
                            // 将前一步解码得到的数据转码为字符串
                            ch.pipeline().addLast(new StringDecoder());
                            // 这里FixedLengthFrameEncoder是我们自定义的，用于将长度不足2000的消息进行补全空格
                            ch.pipeline().addLast(new FixedLengthFrameEncoder(2000));
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new FixedLengthServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}

class FixedLengthServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("server receives message: " + msg.trim());
        ctx.writeAndFlush("hello client! I have received your message.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("server msg: channelInactive");
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("server msg: exceptionCaught");
    }
}
