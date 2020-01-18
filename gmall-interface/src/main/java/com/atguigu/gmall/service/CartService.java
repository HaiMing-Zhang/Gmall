package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    /**
     * 将商品加入购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 获取购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 和并购物车,将未登录的合并到已登录中
     * @param cartList
     * @param userId
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId);

    /**
     * 删除未登录购物车中的商品
     * @param userKey
     */
    void deleteCartList(String userKey);

    /**
     * 购物车复选框勾选
     * @param isChecked
     * @param skuId
     * @param userId
     */
    void checkCart(String isChecked, String skuId, String userId);

    /**
     *获取选中的购物车商品
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    Double checkPrice(String userId);
}
