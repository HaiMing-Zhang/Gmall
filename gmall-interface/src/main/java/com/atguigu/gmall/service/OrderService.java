package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

public interface OrderService {
    /**
     * 保存订单,并返回订单Id
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号是否相同
     * @param tradeCodeNo
     * @param userId
     * @return
     */
    boolean checkTradeCode(String tradeCodeNo,String userId);

    /**
     * 删除流水号TradeNo
     * @param userId
     */
    void deleteTradeCode(String userId);

    /**
     *  验证库存
     * @param skuId
     * @param num
     * @return
     */
    boolean checkStock(String skuId,Integer num);

    /**
     * 获取订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 根据分布式事务修改订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 根据OutTradeNo查询订单
     * @return
     */
    OrderInfo getOrderInfoByOutTradeNo(String outTradeNo);

}
