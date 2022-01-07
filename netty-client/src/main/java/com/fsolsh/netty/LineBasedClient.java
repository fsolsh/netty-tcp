package com.fsolsh.netty;

import com.fsolsh.netty.common.LineBasedFrameEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import static com.fsolsh.netty.common.LineBasedFrameEncoder.NEWLINE;

public class LineBasedClient {


    public static void main(String[] args) throws InterruptedException {
        new LineBasedClient().connect("127.0.0.1", 8080);
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

                            // 对服务端返回的消息通过换行符进行分隔，并且每次查找的最大为1024字节
                            ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            // 将分隔之后的字节数据转换为字符串
                            ch.pipeline().addLast(new StringDecoder());
                            // 对客户端发送的数据进行编码，这里主要是在客户端发送的数据最后添加分隔符
                            ch.pipeline().addLast(new LineBasedFrameEncoder());
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new LineBasedClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class LineBasedClientHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("client receives message: " + msg.trim());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush("hello server, " + NEWLINE + "this is a message from the client!");
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
