package com.hunter.picturebackend;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com/hunter/picturebackend/mapper")
@Slf4j
@EnableAspectJAutoProxy(exposeProxy = true)  // 开启切面代理
@EnableAsync // 开启异步
public class PictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackendApplication.class, args);
        log.info("开发文档地址：http://localhost:8123/api/doc.html#/home");
    }

}
