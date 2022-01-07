package com.fsolsh.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class LineBasedFrameEncoder extends MessageToByteEncoder<String> {

    public static final String NEWLINE = System.lineSeparator();

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) {
        // 在响应的数据后面添加换行符
        ctx.writeAndFlush(Unpooled.wrappedBuffer((msg + NEWLINE).getBytes()));
    }
}