package com.leon.agenthub.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AsyncConfig} 单元测试 — 校验文档索引线程池的关键配置。
 */
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void documentIndexExecutorHasExpectedPoolSettings() {
        ThreadPoolTaskExecutor executor = config.documentIndexExecutor();

        assertEquals(4, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals(200, executor.getQueueCapacity());
    }

    @Test
    void rejectsWithAbortPolicyWhenQueueIsFull() {
        ThreadPoolTaskExecutor executor = config.documentIndexExecutor();

        assertInstanceOf(ThreadPoolExecutor.AbortPolicy.class,
                executor.getThreadPoolExecutor().getRejectedExecutionHandler());
    }

    @Test
    void runsTasksOnDocumentIndexThread() throws Exception {
        ThreadPoolTaskExecutor executor = config.documentIndexExecutor();
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在超时前执行完毕");
        assertTrue(threadName.get().startsWith("doc-index-"),
                "线程名应以 doc-index- 开头，实际为: " + threadName.get());
        executor.shutdown();
    }

    @Test
    void waitsForInFlightTasksOnShutdown() {
        ThreadPoolTaskExecutor executor = config.documentIndexExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(latch::countDown);
        // waitForTasksToCompleteOnShutdown=true 时，停机会等待在途任务完成
        executor.shutdown();

        assertEquals(0, latch.getCount(), "停机时应等待在途索引任务完成，避免文档卡在处理中");
    }
}
