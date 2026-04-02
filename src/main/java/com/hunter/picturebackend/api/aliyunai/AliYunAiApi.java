package com.hunter.picturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.hunter.picturebackend.api.aliyunai.model.CreateImageOutPaintingResponse;
import com.hunter.picturebackend.api.aliyunai.model.GetImageOutPaintingResponse;
import com.hunter.picturebackend.api.aliyunai.model.ImageOutPaintingRequest;
import com.hunter.picturebackend.exception.BusinessException;
import com.hunter.picturebackend.exception.ErrorCode;
import com.hunter.picturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 阿里云图片扩展 API 调用类
 */
@Slf4j
@Service
public class AliYunAiApi {
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param imageOutPaintingRequest
     * @return
     */
    public CreateImageOutPaintingResponse createImageOutPainting(ImageOutPaintingRequest imageOutPaintingRequest) {
        ThrowUtils.throwIf(imageOutPaintingRequest == null, ErrorCode.PARAMS_ERROR, "扩图参数为空");
        // 发送请求
        // 发送POST请求
        HttpRequest request = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(imageOutPaintingRequest));

        // 处理响应
        try (HttpResponse response = request.execute()) {
            ThrowUtils.throwIf(!response.isOk(), ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            CreateImageOutPaintingResponse createImageOutPaintingResponse = JSONUtil.toBean(response.body(), CreateImageOutPaintingResponse.class);
            if (createImageOutPaintingResponse != null) {
                log.error("请求异常：{}", createImageOutPaintingResponse.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            return createImageOutPaintingResponse;
        }


    }


    /**
     * 查询创建的任务结果
     *
     * @param taskId
     * @return
     */
    public GetImageOutPaintingResponse getImageOutPaintingResponse(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务id不能为空！");
        // 处理响应
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        try (HttpResponse response = HttpRequest.get(url)
                .header("Authorization", "Bearer" + apiKey)
                .execute()) {
            ThrowUtils.throwIf(!response.isOk(), ErrorCode.OPERATION_ERROR, "获取任务结果失败");

            return JSONUtil.toBean(response.body(), GetImageOutPaintingResponse.class);
        }
    }


}
