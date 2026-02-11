package com.hunter.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.hunter.picturebackend.model.dto.user.UserLoginRequest;
import com.hunter.picturebackend.model.dto.user.UserQueryRequest;
import com.hunter.picturebackend.model.dto.user.UserRegisterRequest;
import com.hunter.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hunter.picturebackend.model.vo.LoginUserVo;
import com.hunter.picturebackend.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author hunternotgod
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2026-02-11 14:14:01
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return 新用户id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登陆
     *
     * @param userLoginRequest
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVo userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获得脱敏后的登陆用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return 脱敏后的用户信息
     */
    UserVo getUserVo(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList
     * @return 脱敏后的用户信息列表
     */
    List<UserVo> getUserVoList(List<User> userList);

    /**
     * 获取当前登陆用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     */
    void userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
