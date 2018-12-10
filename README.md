Netty简介
•Netty是NIO基础上，诞生的网络应用开发框架；
•分层良好——利于扩展，方便业务开发；
•性能良好——IO线程模型、IO内存零拷贝、优秀的锁使用实践；

低性能
•阻塞
IO读写阻塞、锁等待——导致cpu空闲，资源浪费

•争抢
线程调度、锁竞争——轻量级进程，上下文切换，系统开销增加，吞吐量（us/(sy + us)）下降




•gc


临时对象碎片内存——额外资源占用，吞吐量下降

IO模型演进——单线程



Thread






socket0	socket2	……	socketn



优势：
简单劣势：
所有的读写在一个线程里，通过轮训获得执行时间。没有并发，出现阻塞，存在可读写的连接，也要排队等待，性能低

IO模型演进——per thread / per connection




thread-0

thread-1

thread-2
……

thread-n






socket0


socket1


socket2

socketn




优势：
简单；
连接少的情况下，线程少，并发性能好； 劣势：
连接增加，性能降低；
线程随连接创建和销毁，线程的创建和销毁存在系统开线程数量多，造成争抢，上下文切换，吞吐量降低； 每个线程占用独立的线程栈，内存空间浪费严重；

物理内存：2G，-Xmx1G -Xms1G -Xss1M
请问，最多可以创建多少个线程？


IO模型演进——线程池


Threadpool max_worker:m







socket0	socket1

……	socketm-1





socketm	socketm+1

……	socketn


优势：
通过线程池，约束了线程创建，资源占用降低劣势：
抽屉原理，连接读写是在线程中，排队执行的；
当存在某一个连接阻塞的情况下，其阻塞的线程，不能做其他连接的读写，并发能力降低；

IO模型演进——NIO



thread




accept	read	write selector


channel0	channel1	channel2	……	channeln


优势：
单线程（java原生NIO是1个线程对应1024个连接的读写）； 只要有连接存在读写状态，线程一直工作，无资源浪费；
劣势：
单线程处理多个连接，降低了并发能力；
出现某一连接上业务处理时间长时，其他连接处理被阻塞；

IO模型演进——Netty


work group	child group



accept	read	write
selector	selector




channel0	channel1	channel2	……	channeln

优势：
使用线程池替代原生NIO的线程，并发能力提升，某一连接的业务阻塞，不会影响到所有连接的读写； 读写事件，与accept事件拆分给不同的线程池调度。accept用于创建连接，响应外部连接请求，不因业务
处理阻塞导致连接不响应。保证总有资源用于建立连接；
Netty进行异步调度时，channel与执行这个channel读写任务的executor进行了绑定，避免共享内存的竞争； 劣势：
阻塞child group的loop的业务处理，可能为：1、IO密集型处理；2、运算密集型处理； IO密集型，应进行异步，避免对读写线程的占用；
运算密集的，不应进行异步，此时通过加压，达到系统最大负载处理能力，以此为依据，进行分布式扩容；

IO模型演进——Netty	业务IO密集



work group	child group




accept	read	write
selector	selector



thread pool max_worker:m






channel0	channel1	channel2	……	channeln



业务的并发能力与threadpool的最大线程数相同

堆外内存Direct Memory
•Java 网络程序中使用堆外直接内存进行内容的读取和发送（从javaSocket获取接受数据和写入发送数据），可以避免了字节缓冲区的二次拷贝；

•如果使用传统的堆内存（Heap Memory，其实就是byte[]）进行Socket读写，JVM会将堆内存Buffer拷贝一份到堆外直接内存中， 然后才写入Socket中。反之，读取Socket数据，需要从堆外内存拷贝到JVM堆内创建的byte[]对象，才能进行业务操作；

池化内存管理



PoolByteBufferAllocator










ChunkList:

PoolArena
PoolChunk	PoolChunk

...






平台本地ByteBuffer池

建模


 	page	

ByteBuffer管理
•标记Page:
演示设置maxOrder为4
2


3	2


5	3	3	3



5	5	4	4	4	4	4	4


返回这个页签占用的返by回teB这uf个fe页r   签占用的byteBuffer
池的offset和length	池的offset和length



使用完全二叉树标记的算法，使得搜索可用的PageNode的复杂度从遍历的O(n)降为O(2*log2n)

内存碎片——尺寸分级/分段加锁


PoolArena
tinySubpagePools smallSubpagePools
•超过一个pageSize(>4096， 默认8192)的，进行page连接；

 	16~32	
 	32~64	





SubPage	SubPage	SubPage

一个page的大小至少4k，碎片内存通过bitmap[]数组标记使用状况，避免碎片浪费内存池空间


•超过一个chunkSize 的，不进行池化缓存；

512~1024	SubPage	SubPage	SubPage






分段加锁：
synchronized(head){
……
}

其他内存管理优化
•申请读缓冲
自适应
2^n字节对齐(利用移位运算加速) chunk使用率队列排序

•线程级缓存
PoolThreadLocalCache

高性能的线程安全操作



•无锁化
•volatile
•ThreadLocal
•线程绑定 - NioEventLoop
•CAS
•AtomicXXX 原子类

•减小锁粒度
•分段加锁
•读写锁

无锁化——volatile
•变量修饰符
“可见性”。不同的线程，由不同的cpu调度；每个cpu从缓存行读取数据进行运算；一旦变量有写操作，写入主存，通过内存栅栏的Lock指令，将其他cpu缓存行置无效，其他cpu读请求时，会从主存同步一次到私有内存；

•内存栅栏——Memory Barrier


Lock是处理器缓存同步指令锁定缓存行， 需要处理器支持

thread0

cpu0
缓存行

thread1

cpu1
缓存行


缓存以行为单位同步，行的大小一般为
64bytes。
同一个缓存行存储了多个volatile变量，其中一个失效，会导致这个缓存行的所有变量所在的缓存行锁定、同步？



主存

无锁化——ThreadLocal
•ThreadLocalMap - 永远是”current thread”的成员变量，不存在多线程共享操作；
•MapEntry数组操作，通过hashCode&len操作，获得随机访问的索引。由于前一个条件保证是在同一个线程上下文，因此不会存在数组增减时的同步开销；
无锁化——NioEventLoop
•将NioEventLoop与Executor绑定，run操作在绑定线程中执行；与
ThraedLocal有异曲同工之妙；

ThreadLocal解读






Thread t = Thread.currentThread(); ThreadLocalMap map = getMap(t);

if (map != null) {
ThreadLocalMap.Entry e = map.getEntry(this); if (e != null) {
T result = (T)e.value; return result;

Thread
threadLocals	ThreadLocalMap getEntry(ThreadLocal)

}
}	•	hash方式存取数据

return setInitialValue();
•
被同一线程操作，无需同步代码

无锁化——CAS——AtomicXXX原子操作类




•Compare And Swap 模型
set(y) x=currentVal
N

1、compareAndSwap原子操作前，值可能被修改，因此compare动作可能失败，失败的情况下，x需重新获取最新的值；

2、compareAndSwap操作声明为native的，由native本地实现原子操作；

3、原子类的value字段声明为volatile；

currentVal==x

Y

compareAndSwap



end

减小锁粒度
•减少锁占用时间
copy on write——写时复制技术，同步块变为引用赋值； 尽量减少同步块中的运算代码；

•将大的共享对象切片，对切片进行加锁
分段加锁；

•读写锁
读锁共享，写锁互斥；

Netty实用注意点
•SocketOptions：
•SO_RCVBUF/SO_SNDBUF
socket的读/写缓冲区的大小。不一定生效，实际值还受操作系统限制。以getOption操作读出的值为准。

•SO_TIMEOUT
服务端accept阻塞超时时间。超过设定值，没有客户端连接，accept接口抛出超时异常。

•TCP_NODELAY
启用TCP Negal算法，会提升带宽利用率，但是会影响实时性，导致延迟。

Netty实用注意点
•SO_BACKLOG
设置accept queue长度，受系统somaxconn限制，最终为两者的较小值。服务器最大连接数=阻塞接口accept()返回的所有连接+ MIN(SO_BACKLOG,somaxconn).

Netty实用注意点
•SO_LINGE相R关系统设置：
设置影tc响p_TmIMaEx_WorApIhTa时ns长/tc。p_不fin设_t置ime则按照o系ut统默认2msl。

•SO_REUSEADDR
设置允许绑定不同ip的同一端口， 允许TIME_WAITING状态的连接， 被新创建的连接使用相同的本地 ip和端口。












fin_wait_1







fin_wait_2









FIN

FIN
ACK FIN

FIN FIN
















close_wait




last_ack



so_linger时长，默认：2msl


time_wait

FIN ACK





closed



closed

Netty实用注意点
•ChannelConfig.setAutoRead() 与Channel.isWritable()
默认的，AUTO_READ设置为true，Netty自动从socket的读缓冲读取数据到DirectMemory。
TCP层面感知不到缓冲区满的压力，不进行窗口“收缩”流控，从而造成发送端一直向写通道“塞”数据，如果达到DirectMemory的最大设置，造成OutOfMemoryException.
如果将AUTO_READ关闭之后，内核收到对端的FIN，并回复ACK，进入CLOSE_WAIT状态， 但是应用层关闭了AUTO_READ，收不到读事件，不进行close()调用，那么四次挥手进行了半， 导致大量的CLOSE_WAIT的连接占用系统资源。
isWritable正是与AUTO_READ相对应的。假设对端设置了AUTO_READ为false，并且窗口收
缩到了0，此时应用层不进行Writable判断，仍然进行write，那么TCP的写缓冲会被填满，同时， 由于DirectMemory不断“塞”入待发送数据，达到DirectMemory的最大设置，造成OutOfMemoryException


 	应用层	
 	DirectMemory	
内核Write/Read Buffer





TCP连接

 	应用层	
 	DirectMemory	
内核Write/Read Buffer

Netty实用注意点
•ByteBuff 对象释放原则
引用计数对象的最终使用者，负责销毁对象。

Netty实用注意点
•堆内存溢出监控
netty的直接堆内存最小配置：-Dio.netty.allocator.pageSize=4096 -Dio.netty.allocator.maxOrder=5 -
Dio.netty.maxDirectMemory=786432
在内存足够的情况下，避免使用最小配置。能够保证不同的线程使用不同的PooledArena实例，降低线程同步触发的几率，提升并发效率。

•开启直接堆内存溢出监控
加上jvm启动参数：-Dio.netty.leakDetection.level=PARANOID，触发一次gc，就可以在日志中看到检测的堆外内存泄露信息。利用finallize()在gc时至少触发一次，记录gc的对象，引用计数不为0。
-Dio.netty.leakDetection.targetRecords=1024，如果应用层的内存泄露被阈值忽略，调测时，适当放大这个阈值，可以看到更利于定位的异常日志信息。
默认情况下，是simple级别，日志中提示出采样的1%的ByteBuff是否发生内存泄露；设置为
paranoid，提示出所有的ByteBuff发生泄露的地方，会影响性能测试。

















Thanks！
