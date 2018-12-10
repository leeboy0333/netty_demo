package com.netty.demo.discard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.log4j.lf5.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(DiscardServerHandler.class);

    @Override
    public  void  channelRead(ChannelHandlerContext  ctx, Object msg){
        ByteBuf in = (ByteBuf) msg;
        try {
//            ByteBuf toBeSend = in.retainedSlice();
            while (in.isReadable()) { // (1)
                char b = (char)in.readByte();
                System.out.print(b);
            }
            //在discardServerHandler中仅仅只是记录读取的消息，不作处理，直接下发到后续的Handler
//            ctx.fireChannelRead(toBeSend);
        } finally {
//            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public  void  exceptionCaught(ChannelHandlerContext ctx, Throwable  cause){ //(4)
        logger.error("exception ", cause);
        ctx.close();
    }
}
