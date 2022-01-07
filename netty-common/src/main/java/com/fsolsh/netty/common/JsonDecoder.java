package com.fsolsh.netty.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        JSONObject jsonObject = JSON.parseObject(new String(bytes, CharsetUtil.UTF_8));
        out.add(jsonObject);
    }
}