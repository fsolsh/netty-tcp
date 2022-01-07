package com.fsolsh.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class LengthFieldStringClient {

    public static void main(String[] args) throws InterruptedException {
        new LengthFieldStringClient().connect("127.0.0.1", 8080);
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
                            // 这里将LengthFieldBasedFrameDecoder添加到pipeline的首位，因为其需要对接收到的数据
                            // 进行长度字段解码，这里也会对数据进行粘包和拆包处理
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 2, 0, 2));
                            // 将接收到的字节数据转换为字符串数据
                            ch.pipeline().addLast(new StringDecoder());
                            // LengthFieldPrepender是一个编码器，主要是在响应字节数据前面添加字节长度字段
                            ch.pipeline().addLast(new LengthFieldPrepender(2));
                            // 将发送之前的字节数据转换为字符串数据
                            ch.pipeline().addLast(new StringEncoder());
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new LengthFieldStringClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class LengthFieldStringClientHandler extends SimpleChannelInboundHandler<String> {

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

