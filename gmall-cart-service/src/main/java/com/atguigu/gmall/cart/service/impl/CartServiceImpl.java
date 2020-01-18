package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private SkuService skuService;
    @Autowired
    private RedisUtil redisUtil;
    /**
     * 添加购物车
     * redis + mysql
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        // 定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //如果redis中没有此cartKey的值,则更新缓存,同步数据库
        if(!jedis.exists(cartKey)){
            loadCartCache(userId);
        }
        //先判断数据库的购物车表中是否有此商品
        //select * from cart_info where skuId=? and userId=?
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId",skuId).andEqualTo("userId",userId);
        //从数据库中查询
        CartInfo cartInfo = cartInfoMapper.selectOneByExample(example);
        //如果数据库查询结果不为空
        if(cartInfo != null){
            //说明数据库购物车中有此商品，将商品数量加上页面传过来的购买数量
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            //初始化cartInfo总的skuPrice实时价格
            cartInfo.setSkuPrice(cartInfo.getCartPrice());
            //修改数据库值
            cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
            //更新redis中的数据
        }else{
            //从商品详情中获取数据,然后保存到购物车数据中
            SkuInfo skuInfo = skuService.getSkuInfo(skuId);
            //没有此商品 直接插入
            CartInfo cartInfo1 = new CartInfo();
            //保存到购物车数据中
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            //讲购物车信息插入到数据库中
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfo = cartInfo1;
        }
        //更新缓存
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfo));
        //设置过期时间
        setCartkeyExpireTime(userId, jedis, cartKey);
    }
    //设置过期时间：
    private void setCartkeyExpireTime(String userId, Jedis jedis, String cartKey) {
        // 根据user得过期时间设置
        // 获取用户的过期时间 user:userId:info
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        // 用户key 存在，登录。
        Long expireTime = null;
        if (jedis.exists(userKey)){
            // 获取过期时间
            expireTime = jedis.ttl(userKey);
            // 给购物车的key 设置
            jedis.expire(cartKey,expireTime.intValue());
        }else {
            // 给购物车的key 设置
            jedis.expire(cartKey,7*24*3600);
        }
    }

    /**
     * 获取购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId) {
        //先从redis中查询
        // 定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //hvals获取redis中key为cartKey的数据,返回结果是一个集合List
        List<String> cartInfoListJson = jedis.hvals(cartKey);
        //此集合中存储从redis中传回来的商品数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        //如果redis中有购物车商品信息
        if(cartInfoListJson != null && cartInfoListJson.size()>0){
            for (String cartInfo : cartInfoListJson) {
                //将redis中的商品数据转换为对象存储到集合
                cartInfoList.add(JSON.parseObject(cartInfo,CartInfo.class));
            }
            //根据id排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else{
            //redis中没有数据，去db查询数据并更新到缓存
            cartInfoList  = loadCartCache(userId);
            return cartInfoList;
        }
    }

    /**
     * 和并购物车,将未登录的合并到已登录中
     * @param cartList
     * @param userId
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId) {
        List<CartInfo> cartInfoListLogin = cartInfoMapper.selectCartListWithCurPrice(userId);

        /**
         * update cart_info set userId = ? where id = ?
         *         将未登录的购物车商品的userId改为已登录的userId
         *         更新数据库
         *  思路: 使用两层循环,外层循环已登录购物车,内存循环未登录购物车,
         *        判断外层购物车的skuId是否与内层的skuId相等,
         *        如果相等
         *        则将已登录购物车的skuNum与未登录购物车的skuNum相加并修改已登录购物车的数据库中的skuNum
         *        如果不相等
         *        则直接将未登录购物车的userId改为已登录购物车的userId
         *        最后刷新缓存
         * */
        //在已登陆购物车有数据的情况下
        if(cartInfoListLogin!=null && cartInfoListLogin.size()>0){
            //外层循环:已登录购物车
            for (CartInfo cartInfo : cartInfoListLogin) {
                //获取已登录购物车的skuId
                String skuId = cartInfo.getSkuId();
                if(cartList != null && cartList.size()>0){
                    //内层循环: 未登录购物车
                    for (CartInfo info : cartList) {
                        //如果未登录购物车的isChecked为1,则合并,否则不合并
                        if("1".equals(info.getIsChecked())){
                            //判断已登录购物车和未登录购物车的skuId是否相同
                            if(info.getSkuId().equals(skuId)){
                                //将已登录购物车的skuNum与未登录购物车的skuNum相加
                                cartInfo.setSkuNum(cartInfo.getSkuNum() + info.getSkuNum());
                                cartInfo.setIsChecked(info.getIsChecked());
                                //修改已登录购物车的数据库中的skuNum
                                cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
                                //将未登录购物车的数据库中的此条数据删除
                                cartInfoMapper.delete(info);
                            }else{
                                //给未登录数据库的userId设置成为已登录数据库的userId
                                info.setUserId(userId);
                                //修改未登录购物车的数据库
                                cartInfoMapper.updateByPrimaryKeySelective(info);
                            }
                        }else{
                            //不合并的状态下,如果已登录购物车中有未登录的商品,则取消选中状态
                            if(info.getSkuId().equals(skuId)){
                                cartInfo.setIsChecked(info.getIsChecked());
                                cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
                                cartInfoMapper.delete(info);
                            }
                        }
                    }
                }
            }
        }else{//已登录购物车无数据的情况下
            if(cartList != null && cartList.size()>0){
                for (CartInfo info : cartList) {
                    //给未登录数据库的userId设置成为已登录数据库的userId
                    info.setUserId(userId);
                    //修改未登录购物车的数据库
                    cartInfoMapper.updateByPrimaryKeySelective(info);
                }
            }
        }
        //更新缓存
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    /**
     * 删除未登录购物车的缓存中的商品
     * @param userKey
     */
    @Override
    public void deleteCartList(String userKey) {
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userKey+CartConst.USER_CART_KEY_SUFFIX;
        jedis.del(cartKey);
        jedis.close();
    }

    /**
     * 购物车复选框勾选
     * @param isChecked
     * @param skuId
     * @param userId
     */
    @Override
    public void checkCart(String isChecked, String skuId, String userId) {
        //先根据skuId与userId修改商品的ishecked状态
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId",skuId).andEqualTo("userId",userId);
        cartInfoMapper.updateByExampleSelective(cartInfo,example);
        //修改redis中的商品状态
        CartInfo cartInfo1 = cartInfoMapper.selectOneByExample(example);
        cartInfo1.setSkuPrice(cartInfo1.getCartPrice());
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo1));

        jedis.close();
    }

    /**
     * 获取选中的购物车商品
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartList = jedis.hvals(cartKey);
        if (cartList!=null && cartList.size()>0){
            // 循环遍历
            for (String cartJson : cartList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                if ("1".equals(cartInfo.getIsChecked())){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    @Override
    public Double checkPrice(String userId) {
        Double price = null;
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectByExample(example);
        for (CartInfo cartInfo : cartInfoList) {
            if("1".equals(cartInfo.getIsChecked())){
                price += cartInfo.getCartPrice().doubleValue();
            }
        }
        return price;
    }

    /**
     * 获取数据库中的数据并放入缓存
     * 根据userId查询数据库中购物车的商品,并将查询完的数据存入redis
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        //根据userId查询属于此用户的商品
       List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
       if(cartInfoList == null || cartInfoList.size()==0){
           return null;
       }
        // 定义key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //此map中存储redis中hash数据类型中的field和value组成的数据
        Map<String,String> map = new HashMap<>();
        //循环遍历将cartInfoList集合中的值转换为JSON字符串存储到map中,在用下面的hmset存入redis中
        for (CartInfo cartInfo : cartInfoList) {
            //field:cartInfo.getId()    value:JSON.toJSONString(cartInfo)
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        //以redis中hash类型存储从db中查询出的值
        jedis.hmset(cartKey,map);
        jedis.close();
        return cartInfoList;
    }
}
