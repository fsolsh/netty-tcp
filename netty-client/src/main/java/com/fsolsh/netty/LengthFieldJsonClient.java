package com.fsolsh.netty;

import com.alibaba.fastjson.JSONObject;
import com.fsolsh.netty.common.JsonDecoder;
import com.fsolsh.netty.common.JsonEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class LengthFieldJsonClient {

    public static void main(String[] args) throws InterruptedException {
        new LengthFieldJsonClient().connect("127.0.0.1", 8080);
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
                            // LengthFieldPrepender是一个编码器，主要是在响应字节数据前面添加字节长度字段
                            ch.pipeline().addLast(new LengthFieldPrepender(2));
                            // 对经过粘包和拆包处理之后的数据进行json反序列化，从而得到JsonObject对象
                            ch.pipeline().addLast(new JsonDecoder());
                            // 对响应数据进行编码，主要是将JsonObject对象序列化为字节数组
                            ch.pipeline().addLast(new JsonEncoder());
                            // 最终的消息数据的处理
                            ch.pipeline().addLast(new LengthFieldJsonClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class LengthFieldJsonClientHandler extends SimpleChannelInboundHandler<JSONObject> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JSONObject jsonObject) {
        System.out.println("client receives message: " + jsonObject.toJSONString());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        JSONObject data = new JSONObject();
        data.put("name", "zhang san");
        data.put("age", 20);
        data.put("email", "zhangsan@qq.com");
        data.put("msg", "hello server!");
        //take care of this method
        ctx.write(data);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("client msg: channelInactive");
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("client msg: exceptionCaught" + cause.getMessage());
    }
}
