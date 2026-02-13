package com.hunter.picturebackend.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunter.picturebackend.annotation.AuthCheck;
import com.hunter.picturebackend.common.BaseResponse;
import com.hunter.picturebackend.common.DeleteRequest;
import com.hunter.picturebackend.common.ResultUtils;

import com.hunter.picturebackend.constant.UserConstant;
import com.hunter.picturebackend.exception.BusinessException;
import com.hunter.picturebackend.exception.ErrorCode;
import com.hunter.picturebackend.exception.ThrowUtils;
import com.hunter.picturebackend.model.dto.user.*;
import com.hunter.picturebackend.model.entity.User;
import com.hunter.picturebackend.model.enums.UserRoleEnum;
import com.hunter.picturebackend.model.vo.LoginUserVo;
import com.hunter.picturebackend.model.vo.UserVo;
import com.hunter.picturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
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

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("创建用户")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户 （管理员）未脱敏
     *
     * @param id
     * @return 未脱敏用户信息
     */
    @GetMapping("/get")
    @ApiOperation("根据id获取用户（管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestBody Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        log.info("get user：{},by id：{}", user, id);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取包装类
     *
     * @param id
     * @return 脱敏用户信息
     */
    @GetMapping("/get/vo")
    @ApiOperation("根据id获取包装类")
    public BaseResponse<UserVo> getUserVoById(@RequestBody Long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        UserVo userVo = userService.getUserVo(user);
        return ResultUtils.success(userVo);
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("删除用户")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("更新用户")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询
     *
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVo>> listUserVoByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();

        // 查询结果（未脱敏）
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));

        // 脱敏
        Page<UserVo> userVoPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVo> userVoList = userService.getUserVoList(userPage.getRecords());
        userVoPage.setRecords(userVoList);

        return ResultUtils.success(userVoPage);
    }
}
