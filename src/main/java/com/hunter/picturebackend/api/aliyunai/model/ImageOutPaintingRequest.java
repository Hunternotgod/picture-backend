package com.hunter.picturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 图像扩展（OutPainting）请求
 */
@Data
public class ImageOutPaintingRequest implements Serializable {

    /**
     * 模型名称，固定值：image-out-painting
     */
    @Alias("model")
    private String model = "image-out-painting";

    /**
     * 输入图像信息
     */
    @Alias("input")
    private Input input;

    /**
     * 输出参数
     */
    @Alias("parameters")
    private Parameters parameters;

    @Data
    public static class Input {
        /**
         * 图像URL地址或者图像base64数据
         */
        @Alias("image_url")
        private String imageUrl;
    }

    @Data
    public static class Parameters {
        /**
         * 逆时针旋转角度，默认0，范围[0, 359]
         */
        @Alias("angle")
        private Integer angle = 0;

        /**
         * 图像宽高比，可选："", "1:1", "3:4", "4:3", "9:16", "16:9"
         */
        @Alias("output_ratio")
        private String outputRatio = "";

        /**
         * 水平方向扩展比例，默认1.0，范围[1.0, 3.0]
         */
        @Alias("x_scale")
        @JsonProperty("xScale")
        private Float xScale = 1.0f;

        /**
         * 垂直方向扩展比例，默认1.0，范围[1.0, 3.0]
         */
        @Alias("y_scale")
        @JsonProperty("yScale")
        private Float yScale = 1.0f;

        /**
         * 上方添加像素，默认0
         */
        @Alias("top_offset")
        private Integer topOffset = 0;

        /**
         * 下方添加像素，默认0
         */
        @Alias("bottom_offset")
        private Integer bottomOffset = 0;

        /**
         * 左侧添加像素，默认0
         */
        @Alias("left_offset")
        private Integer leftOffset = 0;

        /**
         * 右侧添加像素，默认0
         */
        @Alias("right_offset")
        private Integer rightOffset = 0;

        /**
         * 开启图像最佳质量模式，默认false
         */
        @Alias("best_quality")
        private Boolean bestQuality = false;

        /**
         * 限制模型生成的图像文件大小，默认true
         */
        @Alias("limit_image_size")
        private Boolean limitImageSize = true;

        /**
         * 添加水印，默认true
         */
        @Alias("add_watermark")
        private Boolean addWatermark = true;
    }
}