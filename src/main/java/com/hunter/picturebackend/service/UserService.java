package com.hunter.picturebackend.service;

import com.hunter.picturebackend.model.dto.UserLoginRequest;
import com.hunter.picturebackend.model.dto.UserRegisterRequest;
import com.hunter.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hunter.picturebackend.model.vo.LoginUserVo;

import javax.servlet.http.HttpServletRequest;

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
     * @param user
     * @return 脱敏后的用户信息
     */
    LoginUserVo getLoginUserVo(User user);

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

}
