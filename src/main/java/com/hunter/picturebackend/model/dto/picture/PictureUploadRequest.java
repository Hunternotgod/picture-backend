package com.hunter.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片id （用于修改）
     */
    private Long id;

    private static final long serialVersionUID = -4244527138033906L;
}
