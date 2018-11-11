package netty.demo.discardserver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class DiscardServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public  void  channelRead(ChannelHandlerContext  ctx, Object msg){
        ByteBuf in = (ByteBuf) msg;
        try {
            ByteBuf toBeSend = in.retainedSlice();
            while (in.isReadable()) { // (1)
                char b = (char)in.readByte();
                System.out.print(b);
            }
            //在discardServerHandler中仅仅只是记录读取的消息，不作处理，直接下发到后续的Handler
            ctx.fireChannelRead(toBeSend);
        } finally {
            ReferenceCountUtil.release(msg); // (2)
        }
    }

    @Override
    public  void  exceptionCaught(ChannelHandlerContext ctx,Throwable  cause){ //(4)
        cause.printStackTrace();
        ctx.close();
    }
}
