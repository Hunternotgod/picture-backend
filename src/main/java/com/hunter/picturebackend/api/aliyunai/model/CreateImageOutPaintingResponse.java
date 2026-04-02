package com.hunter.picturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 图像扩展任务提交响应
 */
@Data
public class CreateImageOutPaintingResponse {

    /**
     * 任务输出信息
     */
    @Alias("output")
    private Output output;

    /**
     * 请求唯一标识
     */
    @Alias("request_id")
    private String requestId;

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
         * PENDING：任务排队中
         * RUNNING：任务处理中
         * SUCCEEDED：任务执行成功
         * FAILED：任务执行失败
         * CANCELED：任务已取消
         * UNKNOWN：任务不存在或状态未知
         */
        @Alias("task_status")
        private String taskStatus;
    }
}