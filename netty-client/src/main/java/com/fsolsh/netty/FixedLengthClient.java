package com.fsolsh.netty;

import com.fsolsh.netty.common.FixedLengthFrameEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class FixedLengthClient {

    public static void main(String[] args) throws InterruptedException {
        new FixedLengthClient().connect("127.0.0.1", 8080);
    }

    public void connect(String host, int port) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 对服务端发送的消息进行粘包和拆包处理，由于服务端发送的消息已经进行了空格补全，
                            // 并且长度为2000，因而这里指定的长度也为2000
                            ch.pipeline().addLast(new FixedLengthFrameDecoder(2000));
                            // 将粘包和拆包处理得到的消息转换为字符串
                            ch.pipeline().addLast(new StringDecoder());
                            // 对客户端发送的消息进行空格补全，保证其长度为2000
                            ch.pipeline().addLast(new FixedLengthFrameEncoder(2000));
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new FixedLengthClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class FixedLengthClientHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("client receives message: " + msg.trim());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush("hello server, a message from the client!");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("client msg: channelInactive");
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("client msg: exceptionCaught");
    }
}
