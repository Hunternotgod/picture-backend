package com.hunter.picturebackend.constant;

/**
 * 图像扩展任务状态
 */
public final class ImageTaskStatus {

    /**
     * 任务排队中
     */
    public static final String PENDING = "PENDING";

    /**
     * 任务处理中
     */
    public static final String RUNNING = "RUNNING";

    /**
     * 任务执行成功
     */
    public static final String SUCCEEDED = "SUCCEEDED";

    /**
     * 任务执行失败
     */
    public static final String FAILED = "FAILED";

    /**
     * 任务已取消
     */
    public static final String CANCELED = "CANCELED";

    /**
     * 任务不存在或状态未知
     */
    public static final String UNKNOWN = "UNKNOWN";

    private ImageTaskStatus() {
    }
}