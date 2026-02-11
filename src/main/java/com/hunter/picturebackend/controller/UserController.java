package com.hunter.picturebackend.controller;


import com.hunter.picturebackend.annotation.AuthCheck;
import com.hunter.picturebackend.common.BaseResponse;
import com.hunter.picturebackend.common.ResultUtils;

import com.hunter.picturebackend.constant.UserConstant;
import com.hunter.picturebackend.exception.ErrorCode;
import com.hunter.picturebackend.exception.ThrowUtils;
import com.hunter.picturebackend.model.dto.UserLoginRequest;
import com.hunter.picturebackend.model.dto.UserRegisterRequest;
import com.hunter.picturebackend.model.entity.User;
import com.hunter.picturebackend.model.vo.LoginUserVo;
import com.hunter.picturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@Api(tags = "用户接口")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return 新用户id
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        log.info("user register：{}", userRegisterRequest);
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户登陆
     *
     * @param userLoginRequest
     * @param request
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    @ApiOperation("用户登陆")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        log.info("user login：{},{}", userLoginRequest, request);
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVo loginUserVo = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 获取当前登陆用户
     *
     * @param request
     * @return
     */
    @GetMapping("/login")
    @ApiOperation("获取当前登陆用户")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LoginUserVo loginUserVo = userService.getLoginUserVo(loginUser);
        log.info("get loginUser：{}", loginUserVo);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @PostMapping("/logout")
    @ApiOperation("用户注销")
    public BaseResponse<String> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        log.info("user logout：{}", request);
        userService.userLogout(request);
        return ResultUtils.success("ok");
    }
}
