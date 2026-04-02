package com.hunter.picturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 图像扩展任务查询响应
 */
@Data
public class GetImageOutPaintingResponse {

    /**
     * 请求唯一标识
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 输出的任务信息
     */
    @Alias("output")
    private Output output;

    /**
     * 图像统计信息
     */
    @Alias("usage")
    private Usage usage;

    /**
     * 请求失败的错误码
     */
    @Alias("code")
    private String code;

    /**
     * 请求失败的详细信息
     */
    @Alias("message")
    private String message;

    @Data
    public static class Output {

        /**
         * 任务ID，查询有效期24小时
         */
        @Alias("task_id")
        private String taskId;

        /**
         * 任务状态
         */
        @Alias("task_status")
        private String taskStatus;

        /**
         * 任务结果统计
         */
        @Alias("task_metrics")
        private TaskMetrics taskMetrics;

        /**
         * 任务提交时间
         */
        @Alias("submit_time")
        private String submitTime;

        /**
         * 任务调度时间
         */
        @Alias("scheduled_time")
        private String scheduledTime;

        /**
         * 任务完成时间
         */
        @Alias("end_time")
        private String endTime;

        /**
         * 输出图像URL地址
         */
        @Alias("output_image_url")
        private String outputImageUrl;
    }

    @Data
    public static class TaskMetrics {

        /**
         * 总的任务数
         */
        @Alias("TOTAL")
        private Integer total;

        /**
         * 任务状态为成功的任务数
         */
        @Alias("SUCCEEDED")
        private Integer succeeded;

        /**
         * 任务状态为失败的任务数
         */
        @Alias("FAILED")
        private Integer failed;
    }

    @Data
    public static class Usage {

        /**
         * 模型成功生成图片的数量
         */
        @Alias("image_count")
        private Integer imageCount;
    }
}