package com.fsolsh.netty.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class JsonEncoder extends MessageToByteEncoder<JSONObject> {

    @Override
    protected void encode(ChannelHandlerContext ctx, JSONObject jsonObject, ByteBuf buf) {
        String json = JSON.toJSONString(jsonObject);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(json.getBytes()));
    }
}