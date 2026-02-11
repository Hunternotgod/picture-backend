package com.hunter.picturebackend.controller;

import com.hunter.picturebackend.common.BaseResponse;
import com.hunter.picturebackend.common.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Api(tags = "检查接口")
public class MainController {

    /**
     * 健康检查
     *
     * @return
     */
    @GetMapping("/health")
    @ApiOperation("健康检查")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok!");
    }
}
