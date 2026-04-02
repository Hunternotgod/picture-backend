package com.hunter.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hunter.picturebackend.api.aliyunai.AliYunAiApi;
import com.hunter.picturebackend.api.aliyunai.model.CreateImageOutPaintingResponse;
import com.hunter.picturebackend.api.aliyunai.model.ImageOutPaintingRequest;
import com.hunter.picturebackend.exception.BusinessException;
import com.hunter.picturebackend.exception.ErrorCode;
import com.hunter.picturebackend.exception.ThrowUtils;
import com.hunter.picturebackend.manager.CosManager;
import com.hunter.picturebackend.manager.cache.LocalCacheManager;
import com.hunter.picturebackend.manager.upload.FilePictureUpload;
import com.hunter.picturebackend.manager.upload.PictureUploadTemplate;
import com.hunter.picturebackend.manager.upload.UrlPictureUpload;
import com.hunter.picturebackend.model.dto.file.UploadPictureResult;
import com.hunter.picturebackend.model.dto.picture.*;
import com.hunter.picturebackend.model.entity.Picture;
import com.hunter.picturebackend.model.entity.Space;
import com.hunter.picturebackend.model.entity.User;
import com.hunter.picturebackend.model.enums.PictureReviewStatusEnum;
import com.hunter.picturebackend.model.vo.PictureVo;
import com.hunter.picturebackend.model.vo.UserVo;
import com.hunter.picturebackend.service.PictureService;
import com.hunter.picturebackend.mapper.PictureMapper;
import com.hunter.picturebackend.service.SpaceService;
import com.hunter.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hunternotgod
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-02-13 16:05:57
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private LocalCacheManager localCacheManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    /**
     * 上传图片
     *
     * @param inputSource          文件输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间权限和额度
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 校验是否有空间的权限，仅空间管理员才能上传
            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            // 校验额度
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间条数不足");
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间条数不足");
        }
        // 判断更新还是新增
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");

            //            boolean exists = this.lambdaQuery()
//                    .eq(Picture::getId, pictureId)
//                    .exists();
//            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可编辑图片
            ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
            // 校验空间是否一致
            // 没传spaceId，则复用原有图片的spaceId（也兼容了公共空间）
            if (spaceId == null) {
                spaceId = oldPicture.getSpaceId();
            } else {
                // 如果传了spaceId，判断spaceId与原图片spaceId是否一致
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id不一致");
            }
        }

        // 上传图片，得到图片信息
        // 按照用户 id 划分目录 ==> 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId()); // 图片路径前缀
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId); // 图片路径前缀
        }


        // 根据inputSource类型，区分上传方式（使用了模版方法）
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId); // 指定空间 id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());


        // 补充审核参数
        this.fillReviewParams(picture, loginUser);

        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和 编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            //插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize +" + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });

        // 更新空间额度
        return PictureVo.objToVo(picture);
    }

    /**
     * 构造 QueryWrapper 对象
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage); // 新增
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId); // 新增
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus); // 新增
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片封装
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVo getPictureVo(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVo pictureVo = PictureVo.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVo = userService.getUserVo(user);
            pictureVo.setUser(userVo);
        }
        return pictureVo;
    }

    /**
     * 分页获取图片封装
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVo> pictureVoPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVoPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVo> pictureVoList = pictureList.stream().map(PictureVo::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVoList.forEach(pictureVo -> {
            Long userId = pictureVo.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVo.setUser(userService.getUserVo(user));
        });
        pictureVoPage.setRecords(pictureVoList);
        return pictureVoPage;
    }

    /**
     * 图片数据校验
     *
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwIf(id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum), ErrorCode.PARAMS_ERROR);

        // 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验审核状态是否重复，修改之前和要修改成的状态相同
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");

        // 数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 管理员，无论是编辑还是创建，默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());

        }

    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 创建成功的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多抓取和创建 30 条");
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败！", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "获取页面失败！");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isEmpty(div), ErrorCode.PARAMS_ERROR, "获取页面失败！");
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片地址，防止转义和对象存储的冲突问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));

            try {
                PictureVo pictureVo = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功，id：{}", pictureVo.getId());
                uploadCount++;
            } catch (Exception e) {
                log.info("图片上传失败", e);
                continue;
            }
            if (uploadCount > count) {
                break;
            }
        }

        return uploadCount;
    }

    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    @Async // 异步执行
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断改图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {

        Long id = pictureId;
        Picture oldPicture = getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验权限
        checkPictureAuth(loginUser, oldPicture);

        // 开启事务
        transactionTemplate.execute(status -> {
            boolean result = removeById(id);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                    .setSql("totalSize = totalSize -" + oldPicture.getPicSize())
                    .setSql("totalCount = totalCount - 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            return true;
        });


        // 清理图片资源
        clearPictureFile(oldPicture);
    }

    /**
     * 批量删除图片
     *
     * @param pictureIdList
     * @param loginUser
     */
    @Override
    public void deletePictures(List<Long> pictureIdList, User loginUser) {

        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList) && loginUser == null, ErrorCode.PARAMS_ERROR);

        List<Picture> pictures = listByIds(pictureIdList);

        for (Picture picture : pictures) {
            ThrowUtils.throwIf(pictures == null, ErrorCode.NOT_FOUND_ERROR);
            // 校验权限
            checkPictureAuth(loginUser, picture);
        }

        // 按空间分组计算额度变更
        Map<Long, Long> spaceSizeDelta = new HashMap<>(); //  需减少的总大小
        Map<Long, Long> spaceCountDelta = new HashMap<>(); //  需减少的总数量

        for (Picture picture : pictures) {
            Long spaceId = picture.getSpaceId();
            spaceSizeDelta.put(spaceId, spaceSizeDelta.getOrDefault(spaceId, 0L) + picture.getPicSize());
            spaceCountDelta.put(spaceId, spaceCountDelta.getOrDefault(spaceId, 0L) + 1);
        }


        // 开启事务
        transactionTemplate.execute(status -> {
            // 数据库删除
            boolean result = removeBatchByIds(pictureIdList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
            for (Map.Entry<Long, Long> entry : spaceSizeDelta.entrySet()) {
                Long spaceId = entry.getKey();
                Long sizeReduce = entry.getValue(); // 额度变化
                Long countReduce = spaceCountDelta.get(spaceId); // 数量变化
                if (spaceId != null) {
                    // 额度更新
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, spaceId)
                            .setSql("totalSize = totalSize -" + sizeReduce)
                            .setSql("totalCount = totalCount - " + countReduce)
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }
                clearPictureListCache(spaceId);
            }
            return true;
        });

        for (Picture picture : pictures) {
            try {
                // 清理图片资源
                clearPictureFile(picture);
            } catch (Exception e) {
                log.error("图片资源清理失败 pictureId: {}", picture.getId());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片清理失败");
            }
        }
    }

    /**
     * 更新图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        validPicture(picture);
        // 补充审核参数
        fillReviewParams(picture, loginUser);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 操作数据库
        boolean result = updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            ThrowUtils.throwIf(!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅本人或管理员可操作");
        } else {
            // 私有空间仅空间管理员可操作
            ThrowUtils.throwIf(!picture.getUserId().equals(loginUserId), ErrorCode.NO_AUTH_ERROR, "仅空间管理员可操作");
        }
    }

    /**
     * 清理图片列表缓存
     *
     * @param spaceId 空间id
     *                - null: 清理公共空间缓存
     *                - 具体值: 清理指定私有空间缓存
     */
    @Override
    public void clearPictureListCache(Long spaceId) {

        try {
            // 策略：清理本地缓存 + 按空间类型清理Redis缓存
            // 由于缓存key是MD5计算的，采用前缀匹配清理

            // 1. 清理本地缓存（Caffeine不支持遍历，直接清理全部）
            localCacheManager.invalidateAll();

            // 2. 构建Redis清理模式
            // 公共空间: 查询条件包含 "nullSpaceId":true
            // 私有空间: 查询条件包含 "spaceId":xxx
            String redisPattern = "picture:listPictureVoByPage:*";
            Set<String> keys = stringRedisTemplate.keys(redisPattern);

            if (!CollectionUtils.isEmpty(keys)) {
                // 如果需要按空间精确清理，可以反序列化key对应的value进行判断
                // 但性能开销大，这里采用简单策略：清理所有列表缓存
                stringRedisTemplate.delete(keys);
                log.info("已清理 {} 条Redis缓存，空间: {}", keys.size(),
                        spaceId == null ? "公共空间" : "空间" + spaceId);
            }
        } catch (Exception e) {
            log.error("清理缓存失败, spaceId={}", spaceId, e);
        }
    }

    /**
     * 创建扩图任务
     *
     * @param pictureImageOutPaintingRequest
     * @param loginUser
     */
    @Override
    public CreateImageOutPaintingResponse createPictureImageOutPaintingTask(PictureImageOutPaintingRequest pictureImageOutPaintingRequest, User loginUser) {
        ThrowUtils.throwIf(pictureImageOutPaintingRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = pictureImageOutPaintingRequest.getPictureId();
        Picture picture = Optional.ofNullable(getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 权限校验
        checkPictureAuth(loginUser, picture);
        // 创建扩图任务
        ImageOutPaintingRequest imageOutPaintingRequest = new ImageOutPaintingRequest();
        ImageOutPaintingRequest.Input input = new ImageOutPaintingRequest.Input();
        input.setImageUrl(picture.getUrl());
        imageOutPaintingRequest.setInput(input);
        imageOutPaintingRequest.setParameters(pictureImageOutPaintingRequest.getParameters());
        // 创建任务
        return aliYunAiApi.createImageOutPainting(imageOutPaintingRequest);

    }


}










