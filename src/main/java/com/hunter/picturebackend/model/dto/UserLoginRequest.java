package com.hunter.picturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登陆请求
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 4157626732226337483L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

}
