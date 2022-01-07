package com.fsolsh.netty;

import com.fsolsh.netty.common.DelimiterBasedFrameEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import static com.fsolsh.netty.common.DelimiterBasedFrameEncoder.DEFAULT_DELIMITER;

public class DelimiterBasedServer {

    public static void main(String[] args) throws InterruptedException {
        new DelimiterBasedServer().bind(8080);
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
                            // 将delimiter设置到DelimiterBasedFrameDecoder中，经过该解码一器进行处理之后，源数据将会
                            // 被按照_$进行分隔，这里1024指的是分隔的最大长度，即当读取到1024个字节的数据之后，若还是未
                            // 读取到分隔符，则舍弃当前数据段，因为其很有可能是由于码流紊乱造成的
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer(DEFAULT_DELIMITER.getBytes())));
                            // 将分隔之后的字节数据转换为字符串数据
                            ch.pipeline().addLast(new StringDecoder());
                            // 这是我们自定义的一个编码器，主要作用是在返回的响应数据最后添加分隔符
                            ch.pipeline().addLast(new DelimiterBasedFrameEncoder(DEFAULT_DELIMITER));
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new DelimiterBasedServerHandler());
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

class DelimiterBasedServerHandler extends SimpleChannelInboundHandler<String> {

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
