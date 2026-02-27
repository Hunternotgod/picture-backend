package com.hunter.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunter.picturebackend.model.dto.picture.PictureQueryRequest;
import com.hunter.picturebackend.model.dto.picture.PictureReviewRequest;
import com.hunter.picturebackend.model.dto.picture.PictureUploadRequest;
import com.hunter.picturebackend.model.dto.user.UserQueryRequest;
import com.hunter.picturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hunter.picturebackend.model.entity.User;
import com.hunter.picturebackend.model.vo.PictureVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author hunternotgod
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2026-02-13 16:05:57
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVo uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片封装
     *
     * @param picture
     * @param request
     * @return pictureVo
     */
    PictureVo getPictureVo(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片数据校验
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);
}
