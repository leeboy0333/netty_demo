package netty.demo.discardserver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public  void  channelRead(ChannelHandlerContext  ctx, Object msg){
        ((ByteBuf)msg).release(); //(3)
    }

    @Override
    public  void  exceptionCaught(ChannelHandlerContext ctx,Throwable  cause){ //(4)
        cause.printStackTrace();
        ctx.close();
    }
}
