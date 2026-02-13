package com.hunter.picturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.hunter.picturebackend.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureVo implements Serializable {

    /**
     * id  
     */
    private Long id;

    /**
     * 图片 url  
     */
    private String url;

    /**
     * 图片名称  
     */
    private String name;

    /**
     * 简介  
     */
    private String introduction;

    /**
     * 标签  
     */
    private List<String> tags;

    /**
     * 分类  
     */
    private String category;

    /**
     * 文件体积  
     */
    private Long picSize;

    /**
     * 图片宽度  
     */
    private Integer picWidth;

    /**
     * 图片高度  
     */
    private Integer picHeight;

    /**
     * 图片比例  
     */
    private Double picScale;

    /**
     * 图片格式  
     */
    private String picFormat;

    /**
     * 用户 id  
     */
    private Long userId;

    /**
     * 创建时间  
     */
    private Date createTime;

    /**
     * 编辑时间  
     */
    private Date editTime;

    /**
     * 更新时间  
     */
    private Date updateTime;

    /**
     * 创建用户信息  
     */
    private UserVo user;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象  
     */
    public static Picture voToObj(PictureVo PictureVo) {
        if (PictureVo == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(PictureVo, picture);
        // 类型不同，需要转换  
        picture.setTags(JSONUtil.toJsonStr(PictureVo.getTags()));
        return picture;
    }

    /**
     * 对象转封装类  
     */
    public static PictureVo objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVo PictureVo = new PictureVo();
        BeanUtils.copyProperties(picture, PictureVo);
        // 类型不同，需要转换  
        PictureVo.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return PictureVo;
    }
}

