package io.merculet.base.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义线程池
 */
@Slf4j
@Component
public class ThreadConfig {
    /**
     * 参数初始化
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * 核心线程数量大小
     */
    private static final int corePoolSize = Math.max(8, Math.min(CPU_COUNT*2 - 1, 8));
    /**
     * 线程池最大容纳线程数
     */
    private static final int maximumPoolSize = CPU_COUNT * 4 + 1;
    /**
     * 线程空闲后的存活时长
     */
    private static final int keepAliveTime = 30;

    /**
     * 线程队列长度
     */
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(200,true);


    /**
     * 线程的创建工厂
     */
    ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "thread #" + mCount.getAndIncrement());
            thread.setUncaughtExceptionHandler( (t,e) -> {
                log.error("thread exception,thread={} msg={}",thread.getName() , e.getMessage());
            });
            return thread;
        }
    };

    /**
     * 线程池任务满载后采取的任务拒绝策略
     */
    RejectedExecutionHandler rejectHandler = new ThreadPoolExecutor.DiscardOldestPolicy();

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        /**
         * 线程池对象，创建线程
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue, threadFactory, rejectHandler);
        return threadPoolExecutor;
    }

    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor(){
        /**
         * 线程池对象，创建线程
         */
        ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(maximumPoolSize,threadFactory);
        return threadPoolExecutor;
    }

}
