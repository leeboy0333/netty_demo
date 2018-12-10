package com.netty.demo.entry;

import com.netty.demo.discard.DiscardServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.NettyDumpHelper;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class NettyServer {
    private Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private ChannelFuture f;

    private int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup(5);
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //                            .addLast(new LineBasedFrameDecoder(1024))
                                    //                            .addLast(new StringDecoder())
                                    .addLast(new DiscardServerHandler());
//                                    .addLast(new EchoServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    //                    .childOption(ChannelOption.AUTO_READ, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            f = b.bind(port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void dumpNettyBuffer() {
        PooledByteBufAllocator allocator = (PooledByteBufAllocator) f.channel().config().getAllocator();

        logger.error(allocator.dumpStats());
    }

    public void memLeakInfo(){
        PooledByteBufAllocator allocator = (PooledByteBufAllocator) f.channel().config().getAllocator();

        StringBuffer sb = new StringBuffer();
        allocator.directArenas().stream().map(arena->{
                        StringBuilder arenaSb = new StringBuilder("\n-------- arena : " + Integer.toHexString(System.identityHashCode(arena)) + " --------\n");
                        NettyDumpHelper.appendArena(arenaSb, arena);
                        arenaSb.append(StringUtil.NEWLINE)
                                .append(
                                "allocations:        "+arena.numAllocations() + ", active:        " + arena.numActiveAllocations() +", deallocate:        " + arena.numDeallocations() + "\n" +
                                "tinyAllocations:    "+arena.numTinyAllocations() + ", active:        " + arena.numActiveTinyAllocations() +", deallocate:        " + arena.numTinyDeallocations() + "\n" +
                                "smallAllocations:   "+arena.numSmallAllocations() + ", active:        " + arena.numActiveSmallAllocations() +", deallocate:        " + arena.numSmallDeallocations() + "\n" +
                                "normal:             "+arena.numNormalAllocations() + ", active:        " + arena.numActiveNormalAllocations() +", deallocate:        " + arena.numNormalDeallocations() + "\n" +
                                "huge:               "+arena.numHugeAllocations() + ", active:        " + arena.numActiveHugeAllocations() +", deallocate:        " + arena.numHugeDeallocations() + "\n");
                        return arenaSb.toString();
                    }
                ).forEach(arenaInfo -> sb.append(arenaInfo));
        logger.error(sb.toString());
    }


    public void close() {
        try {
            f.channel().close();
        } catch (Throwable e) {
            logger.error("channel close error.", e);
        }
    }


    public static void main(String[] args) {
        InternalLogger logger = InternalLoggerFactory.getInstance(ResourceLeakDetector.class);

        int port = 8080;

        NettyServer server = new NettyServer(port);

        new Thread(() -> {
            try {
                server.run();
            } catch (Throwable e) {
                logger.error("err happen.", e);
            }
        }).start();

        Scanner scanner = null;
        try {
            scanner = new Scanner(System.in);
        } catch (Throwable e) {
            logger.error("file error", e);
            server.close();
            return;
        }
        System.out.println("help：\n\texit - close server;\n\tgc - do garbage collection;\n\tdump - print memory allocator info;\n\tleak - print memory allocator details.");
        String cmd = null;
        while (!"exit".equalsIgnoreCase(cmd)) {
            cmd = scanner.nextLine();
            if ("gc".equalsIgnoreCase(cmd)) {
                logger.info("system gc");
                System.gc();
            }
            else if ("dump".equalsIgnoreCase(cmd)){
                server.dumpNettyBuffer();
            }
            else if ("leak".equalsIgnoreCase(cmd)){
                server.memLeakInfo();
            }
        }
        scanner.close();
        server.close();
    }
}
