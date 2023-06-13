package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * ClassName: CacheClient
 * Package: com.hmdp.utils
 * Description:
 *
 * @Author xzh
 * @Create 2023/4/24 15:37
 */
@Component
@Slf4j
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 1 将任意Java对象序列化成json/并存储到string类型的key中，并且可以设置TTL过期时间
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),time,unit);
    }
    // 2 将任意Java对象序列化成json/并存储到string类型的key中，并且可以设置逻辑过期时间,用于处理缓存击穿问题
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }
    // 3 根据指定的key查询缓存，并且反序列化为指定类型 利用缓存空值的方法解决缓存穿透问题
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback
    ,Long time,TimeUnit unit){
        String key = keyPrefix + id;
        // 1 从redis里面查商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2 判断是否存在
        if(StrUtil.isNotBlank(json)){
            // 3 存在 直接返回
            return JSONUtil.toBean(json,type);
        }
        // 判断命中的是否是空置
        if(json != null){
            return null;
        }
        // 4 不存在 根据id查询数据库
        R r = dbFallback.apply(id);
        // 5 不存在返回错误
        if(r == null){
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6 存在 写入redis
        this.set(key,r,time,unit);
        // 7 返回
        return r;
    }

    // 4 根据指定的key查询缓存，并且反序列化为指定类型 利用逻辑过期解决缓存击穿问题
    private  static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <ID,R> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback
            ,Long time,TimeUnit unit){
        String key = keyPrefix + id;
        // 1 从redis里面查商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2 判断是否存在
        if(StrUtil.isBlank(json)){
            // 3 存在 直接返回
            return null;
        }
        // 4 命中 需要把json反序列成对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 5.1 未过期 直接返回店铺信息
            return r;
        }
        // 5.2 已经过期 需要缓存重建
        // 6 缓存重建
        // 6.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2 判断是否成功获取锁
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    // 6.3 成功 开启独立线程 实现缓存重建
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key,r1,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }

        // 6.4 返回过期的商铺信息
        return r;
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
