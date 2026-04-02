package com.hunter.picturebackend.model.dto.picture;

import com.hunter.picturebackend.api.aliyunai.model.ImageOutPaintingRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建扩图任务请求封装类
 */
@Data
public class PictureImageOutPaintingRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private ImageOutPaintingRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;


}
