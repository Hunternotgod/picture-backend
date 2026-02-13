package com.hunter.picturebackend.controller;

import com.hunter.picturebackend.annotation.AuthCheck;
import com.hunter.picturebackend.common.BaseResponse;
import com.hunter.picturebackend.common.ResultUtils;
import com.hunter.picturebackend.constant.UserConstant;
import com.hunter.picturebackend.exception.BusinessException;
import com.hunter.picturebackend.exception.ErrorCode;
import com.hunter.picturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/test/upload")
    @ApiOperation("测试文件上传")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);

        File file = null;
        try {
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath, file);
            // 返回可访问的地址
            log.info("upload is success! filePath:{}", filePath);
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error,filePath：{}", filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error,filePath：{}", filePath);
                }
            }
        }
    }


    /**
     * 测试文件下载
     *
     * @param filePath 文件路径
     * @param response 响应对象
     */
    @GetMapping("/test/download")
    @ApiOperation("测试文件下载")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
            cosObjectInput = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 关闭流
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }

    }


}
