package com.fsolsh.netty;

import com.alibaba.fastjson.JSONObject;
import com.fsolsh.netty.common.JsonDecoder;
import com.fsolsh.netty.common.JsonEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class LengthFieldJsonServer {

    public static void main(String[] args) throws InterruptedException {
        new LengthFieldJsonServer().bind(8080);
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
                            // 这里将LengthFieldBasedFrameDecoder添加到pipeline的首位，因为其需要对接收到的数据
                            // 进行长度字段解码，这里也会对数据进行粘包和拆包处理
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 2, 0, 2));
                            // LengthFieldPrepender是一个编码器，主要是在响应字节数据前面添加字节长度字段
                            ch.pipeline().addLast(new LengthFieldPrepender(2));
                            // 对经过粘包和拆包处理之后的数据进行json反序列化，从而得到JsonObject对象
                            ch.pipeline().addLast(new JsonDecoder());
                            // 对响应数据进行编码，主要是将JsonObject对象序列化为字节数组
                            ch.pipeline().addLast(new JsonEncoder());
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new LengthFieldJsonServerHandler());
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

class LengthFieldJsonServerHandler extends SimpleChannelInboundHandler<JSONObject> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JSONObject jsonObject) {
        System.out.println("server receives message: " + jsonObject.toJSONString());
        JSONObject data = new JSONObject();
        data.put("name", "li si");
        data.put("age", 20);
        data.put("email", "lisi@qq.com");
        data.put("msg", "hello client!");
        //take care of this method
        ctx.write(data);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("server msg: channelInactive");
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("server msg: exceptionCaught" + cause.getMessage());
    }
}
