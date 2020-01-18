package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存支付信息
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 查询支付信息
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 修改交易状态
     * @param paymentInfo
     */
    void updatePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 退款
     * @return
     */
    boolean refund(String orderId);

    /**
     * 微信支付
     * @param s
     * @param s1
     * @return
     */
    Map createNative(String s, String s1);

    /**
     * 支付成功通过activeMQ进行通知
     * @param orderId
     * @param result
     */
    void sendPaymentResult(String orderId,String result);

    /**
     * 判断支付宝支付是否成功
     * @param orderInfo
     * @return
     */
    boolean checkPayment(OrderInfo orderInfo);

    /**
     * 发送延迟队列,如果支付失败,则关闭订单
     * @param outTradeNo
     * @param delaySec
     */
    void closeOrderInfo(String outTradeNo,int delaySec);
}
