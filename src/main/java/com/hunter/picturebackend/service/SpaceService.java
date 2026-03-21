package com.hunter.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hunter.picturebackend.model.dto.space.SpaceAddRequest;
import com.hunter.picturebackend.model.dto.space.SpaceQueryRequest;
import com.hunter.picturebackend.model.entity.Space;
import com.hunter.picturebackend.model.entity.User;
import com.hunter.picturebackend.model.vo.SpaceVo;

import javax.servlet.http.HttpServletRequest;


/**
 * @author hunternotgod
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-03-21 16:52:43
 */
public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return newSpaceId
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     *
     * @param space 需校验的空间
     * @param add   判断是否为创建空间（否为编辑空间）
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别自动填充限额数据
     *
     * @param space 需填充空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 获取单个空间封装
     *
     * @param space
     * @param request
     * @return spaceVo
     */
    SpaceVo getSpaceVo(Space space, HttpServletRequest request);

    /**
     * 分页获取空间封装
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVo> getSpaceVoPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


}
