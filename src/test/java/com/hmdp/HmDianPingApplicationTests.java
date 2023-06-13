/*
package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.Adler32;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testSaveShop() {
       //shopService.saveShop2Redis(1L,10L);
    }

    */
/*@Test
    void loadShopData(){
        // 查询店铺信息
        List<Shop> list = shopService.list();
        // 把店铺分组 按照typeId
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 分批写入redis
        for(Map.Entry<Long,List<Shop>> entry : map.entrySet()){
            // 获取类型id
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;
            // 获取同类型的店铺集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 写入redis geoadd 经度 纬度 member
            for(Shop shop : value){
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(),shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key,locations);
        }
    }

    @Test
    void testHyperLogLog(){
        String[] values = new String[1000];
        int j = 0;
        for(int i = 0; i< 1000000; i++){
            j = i % 1000;
            values[j] = "user_" + i;
            if(j == 999){
                stringRedisTemplate.opsForHyperLogLog().add("hl2",values);
            }
        }
        // 统计数量
        Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
        System.out.println("count = "+count);
    }*//*

}
*/
