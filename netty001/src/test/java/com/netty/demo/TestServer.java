package com.netty.demo;

import com.netty.demo.entry.NettyServer;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.junit.Test;

import java.util.Scanner;

public class TestServer {
    @Test
    public void test001(){
        InternalLogger logger = InternalLoggerFactory.getInstance(ResourceLeakDetector.class);

        int port = 8080;

        NettyServer server = new NettyServer(port);

        new Thread(()->{
            try{
                server.run();
            }
            catch (Throwable e){
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
        System.out.println("exit - close server;\ngc - print memory allocator info.");
        String cmd = null;
        while(!"exit".equalsIgnoreCase(cmd)){
            cmd = scanner.nextLine();
            if("gc".equalsIgnoreCase(cmd)){
                server.dumpNettyBuffer();
            }
        }
        scanner.close();
        server.close();
    }
}
