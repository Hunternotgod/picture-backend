package com.hunter.picturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hunter.picturebackend.model.entity.Picture;
import com.hunter.picturebackend.service.PictureService;
import com.hunter.picturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * @author hunternotgod
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-02-13 16:05:57
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

}




