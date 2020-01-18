package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SkuService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private RedisUtil redisUtil;
    /**
     * 查询spu图片集合
     * @param spuImage
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }
    /**
     * sku中加载销售属性下拉列表
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        List<SpuSaleAttr> saleAttrList =spuSaleAttrMapper.selectSpuSaleAttrBySpuId(spuId);
        return saleAttrList;
    }
    /**
     * 保存Sku
     * @param skuInfo
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insertSelective(skuInfo);
        //判断是否有SkuImage,如果有则保存
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList!=null && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
        //判断是否有SkuAttrValue,如果有则保存
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if(skuAttrValueList != null && skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        //判断是否有SkuSaleAttrValue,如果有则保存
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(skuAttrValueList != null && skuAttrValueList.size()>0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }
    /**
     * 根据skuId查询skuInfo与skuImage
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //SkuInfo skuInfoByRedisSetLock = getSkuInfoByRedisSetLock(skuId);
        return getSkuInfoByRedisson(skuId);
    }

    /**
     * redissonf分布式锁获取rediss中的数据
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedisson(String skuId) {
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        RLock lock = null;
        try {
            //调用redisUtil工具类,获取jedis,连接redis
            jedis = redisUtil.getJedis();
            //定义key,    sku:skuId:info
            String redisKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            //从redis缓中获取数据
            String skuInfoJson = jedis.get(redisKey);
            //判断获取到的数据是否为空
            if(skuInfoJson == null){
                //上锁,防止缓存击穿,此处用的分布式锁是redisson
                System.out.println("没有命中缓存");
                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.168.131:6379");
                RedissonClient redisson = Redisson.create(config);
                lock = redisson.getLock("myLock");
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                if (res) {
                    //从数据库中获取数据,然后存入redis中
                    skuInfo = this.getSkuInfoDB(skuId);
                    //如果数据库中的值为null，则下载判断redis中的值时也为null,便会发生缓存穿透,所以此处赋值为""
                    if(skuInfo == null){
                        jedis.setex(redisKey,ManageConst.SKUKEY_TIMEOUT,"");
                        return skuInfo;
                    }
                    //将skuInfo对象转成Json字符串
                    String skuInfoToJson = JSON.toJSONString(skuInfo);
                    //存入redis
                    jedis.setex(redisKey,ManageConst.SKUKEY_TIMEOUT,skuInfoToJson);
                    return skuInfo;
                }else{
                    //如果锁返回的信息不是true，则说明已经上锁,等待1秒后,重新查询
                    System.out.println("等待！");
                    // 等待
                    Thread.sleep(1000);
                    //重新查询
                    return this.getSkuInfo(skuId);
                }
            }else{
                //这里表示redis中有此数据,转换为对象后直接返回
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                jedis.close();
            }
            if(lock!=null){
                lock.unlock();
            }
        }
        return getSkuInfoDB(skuId);
    }

    /**
     * 带set方式的分布式锁的整合了redis的skuInfo查询
     */
    private SkuInfo getSkuInfoByRedisSetLock(String skuId) {
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        try {
            //调用redisUtil工具类,获取jedis,连接redis
            jedis = redisUtil.getJedis();
            //定义key,    sku:skuId:info
            String redisKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            //从redis缓中获取数据
            String skuInfoJson = jedis.get(redisKey);
            //判断获取到的数据是否为空
            if(skuInfoJson == null){
                //上锁,防止缓存击穿
                System.out.println("没有命中缓存");
                //设置随机值,保证锁的key时随机的
                String token = UUID.randomUUID().toString().replace("-","");
                // 定义锁的key user:userId:lock
                String skuLockKey=ManageConst.SKUKEY_PREFIX+token+ManageConst.SKULOCK_SUFFIX;
                //上锁,设置过期时间为10秒钟
                String lockKey = jedis.set(skuLockKey, "ok", "nx", "px", ManageConst.SKULOCK_EXPIRE_PX);
                if("OK".equals(lockKey)){
                    //从数据库获取数据,之后存入redis
                    skuInfo = this.getSkuInfoDB(skuId);
                    //将数据对象转换为Json字符串
                    String skuInfoToJson = JSON.toJSONString(skuInfo);
                    //存入redis,设置过期时间为一个星期
                    jedis.setex(redisKey,ManageConst.SKUKEY_TIMEOUT,skuInfoToJson);
                    //使用lua脚本解锁
                    String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(token));
                    return skuInfo;
                }else {
                    //如果锁返回的信息不是"OK"，则说明已经上锁,等待1秒后,重新查询
                    System.out.println("等待！");
                    // 等待
                    Thread.sleep(1000);
                    //重新查询
                    return this.getSkuInfo(skuId);
                }
            }else{
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    /**
     * 根据skuId从数据库中获取skuInfo商品详情页的基本信息
     * 说明: 因为加入redis,所以将这个功能从getSkuInfo中提取了出来,之前是在
     * getSkuInfo方法中的
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        //根据skuId查询平台属性值,并保存至skuInfo中的setSkuAttrValueList中
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        skuInfo.setSkuAttrValueList(skuAttrValueMapper.select(skuAttrValue));
        return skuInfo;
    }

    /**
     * 加载页面时显示销售属性与销售属性值
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        List<SpuSaleAttr> saleAttList = spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());

        return saleAttList;
    }
    /**
     * 选择不同销售属性值进行跳转
     * @return
     */
    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(SkuInfo skuInfo) {
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        return skuSaleAttrValueList;
    }

}
