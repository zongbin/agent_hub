package com.leon.agenthub.common.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务线程池配置。
 *
 * <p>文档索引（解析 → 分块 → embedding → 写向量库）是 IO 密集型长任务，
 * 独占 {@code documentIndexExecutor}，避免与后续 Agent 任务的异步执行互相挤占。
 *
 * <p>队列容量说明：队列中存放的是轻量任务对象（仅含文档 ID 与文件路径），
 * 真正占内存的解析文本只存在于运行中的线程内，因此队列可以适度放宽；
 * 但队列越长，排队文档的等待时间越长，故仍设上限并配合拒绝策略兜底。
 */
@Configuration
public class AsyncConfig {

    /** 文档索引线程池 Bean 名 */
    public static final String DOCUMENT_INDEX_EXECUTOR = "documentIndexExecutor";

    /** 常驻线程数 */
    private static final int CORE_POOL_SIZE = 4;

    /** 最大线程数 */
    private static final int MAX_POOL_SIZE = 8;

    /** 等待队列容量 */
    private static final int QUEUE_CAPACITY = 200;

    /** 停机时等待在途任务完成的时长（秒） */
    private static final int AWAIT_TERMINATION_SECONDS = 120;

    /**
     * 文档索引专用线程池。
     *
     * <p>关键配置：
     * <ul>
     *   <li>{@code AbortPolicy}：队列满时立即拒绝，由调用方标记文档为失败；
     *       不用 CallerRunsPolicy，避免拖垮上传接口的请求线程</li>
     *   <li>{@code waitForTasksToCompleteOnShutdown}：停机时等待在途索引完成，
     *       防止文档永久卡在「处理中」</li>
     * </ul>
     */
    @Bean(DOCUMENT_INDEX_EXECUTOR)
    public ThreadPoolTaskExecutor documentIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("doc-index-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}
