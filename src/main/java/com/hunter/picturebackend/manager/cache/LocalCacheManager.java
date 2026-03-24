package com.hunter.picturebackend.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地缓存管理器
 * 基于 Caffeine 实现的本地缓存，支持图片列表等数据的缓存
 */
@Slf4j
@Component
public class LocalCacheManager {

    /**
     * 图片列表本地缓存
     */
    private Cache<String, String> pictureListCache;

    @PostConstruct
    public void init() {
        pictureListCache = Caffeine.newBuilder()
                .initialCapacity(1024)
                .maximumSize(10_000L) // 最大一万条数据
                // 缓存5min后移除
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats() // 开启统计
                .build();
        log.info("本地缓存管理器初始化完成");
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存key
     * @return 缓存值，不存在返回null
     */
    public String get(String key) {
        return pictureListCache.getIfPresent(key);
    }

    /**
     * 设置缓存
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    public void put(String key, String value) {
        pictureListCache.put(key, value);
    }

    /**
     * 删除指定缓存
     *
     * @param key 缓存key
     */
    public void invalidate(String key) {
        pictureListCache.invalidate(key);
    }

    /**
     * 清理所有缓存
     */
    public void invalidateAll() {
        pictureListCache.invalidateAll();
        log.info("本地缓存已全部清理");
    }

    /**
     * 根据前缀清理缓存
     * 注意：Caffeine 不支持通配符，此方法会遍历所有key进行匹配
     *
     * @param prefix key前缀
     */
    public void invalidateByPrefix(String prefix) {
        ConcurrentMap<String, String> map = pictureListCache.asMap();
        Set<String> keysToRemove = map.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(java.util.stream.Collectors.toSet());

        keysToRemove.forEach(pictureListCache::invalidate);
        log.info("根据前缀 [{}] 清理了 {} 条本地缓存", prefix, keysToRemove.size());
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getStats() {
        return pictureListCache.stats();
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存条目数
     */
    public long getSize() {
        return pictureListCache.estimatedSize();
    }
}