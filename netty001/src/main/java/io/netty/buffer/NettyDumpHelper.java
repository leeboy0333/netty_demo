package io.netty.buffer;

import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class NettyDumpHelper {
    private static final Logger logger = LoggerFactory.getLogger(NettyDumpHelper.class);

    private static void appendPoolSubPages(StringBuilder buf, PoolSubpage[] subpages) {
        buf.append(StringUtil.NEWLINE);
        for(int i = 0; i < subpages.length; ++i) {

            PoolSubpage<?> head = subpages[i];
            if (head.next != head) {
                PoolSubpage s = head.next;
                buf.append(StringUtil.NEWLINE).append(i).append(": ");
                do {
                    buf.append("->");
                    appendSubPage(buf, s);
                    s = s.next;
                } while(s != head);
                buf.append(StringUtil.NEWLINE);
            }
        }
    }

    private static <M, T> T getObjectField(M obj, String fieldName, Class<T> clazz){
        if(obj == null || StringUtil.isNullOrEmpty(fieldName)){
//            logger.warn("obj is {}, fieldName is {} \n{}", String.valueOf(obj), fieldName, Thread.currentThread().getStackTrace());
            return null;
        }
        Field field = null;
        try {
            Class objClass = obj.getClass();
            while (objClass != null){
                try{
                    field = objClass.getDeclaredField(fieldName);
                }catch (NoSuchFieldException e){
                    ;
                }
                objClass = objClass.getSuperclass();
            }
            if(field != null){
                field.setAccessible(true);
                return (T)field.get(obj);
            }
        } catch (IllegalAccessException e) {
            logger.error("failed get obj." + fieldName, e);
        }
        return null;
    }

    public static void appendArena(StringBuilder buf, PoolArenaMetric arena) {
        appendArena(buf, (PoolArena)arena);
    }

    public static void appendArena(StringBuilder buf, PoolArena arena){
        buf.append("Chunk(s) at 0~25%:").append(StringUtil.NEWLINE)
                .append(getObjectField(arena, "qInit", PoolChunkList.class))
                .append(StringUtil.NEWLINE).append("Chunk(s) at 0~50%:")
                .append(StringUtil.NEWLINE).append(getObjectField(arena, "q000", PoolChunkList.class))
                .append(StringUtil.NEWLINE).append("Chunk(s) at 25~75%:")
                .append(StringUtil.NEWLINE).append(getObjectField(arena, "q025", PoolChunkList.class))
                .append(StringUtil.NEWLINE).append("Chunk(s) at 50~100%:")
                .append(StringUtil.NEWLINE).append(getObjectField(arena, "q050", PoolChunkList.class))
                .append(StringUtil.NEWLINE).append("Chunk(s) at 75~100%:")
                .append(StringUtil.NEWLINE).append(getObjectField(arena, "q075", PoolChunkList.class))
                .append(StringUtil.NEWLINE).append("Chunk(s) at 100%:")
                .append(StringUtil.NEWLINE).append(getObjectField(arena, "q100", PoolChunkList.class))
                .append(StringUtil.NEWLINE).append("tiny subpages:");
        appendPoolSubPages(buf, getObjectField(arena, "tinySubpagePools", PoolSubpage[].class));
        buf.append(StringUtil.NEWLINE).append("small subpages:");
        appendPoolSubPages(buf, getObjectField(arena, "smallSubpagePools", PoolSubpage[].class));
        buf.append(StringUtil.NEWLINE);
    }


    private static void appendSubPage(StringBuilder buf, PoolSubpage page){
        PoolArena arena = page.chunk == null ? null : page.chunk.arena;
        if(arena == null){
            return;
        }
        boolean doNotDestroy;
        int maxNumElems;
        int numAvail;
        int elemSize;


        synchronized(arena) {
            if (!getObjectField(page, "doNotDestroy", boolean.class)) {
                doNotDestroy = false;
                elemSize = -1;
                numAvail = -1;
                maxNumElems = -1;
            } else {
                doNotDestroy = true;
                maxNumElems = getObjectField(page, "maxNumElems", int.class);
                numAvail = getObjectField(page, "numAvail", int.class);
                elemSize = getObjectField(page, "elemSize", int.class);
            }
        }
        int memoryMapIdx = getObjectField(page, "memoryMapIdx", int.class);
        int runOffset = getObjectField(page, "runOffset", int.class);
        int pageSize = getObjectField(page, "pageSize", int.class);

        buf.append(Integer.toHexString(System.identityHashCode(page.chunk)));
        buf.append(!doNotDestroy ? "(" + memoryMapIdx + ": not in use)" : "(" + memoryMapIdx + ": " + (maxNumElems - numAvail) + '/' + maxNumElems + ", offset: " + runOffset + ", length: " + pageSize + ", elemSize: " + elemSize + ')');
    }
}
