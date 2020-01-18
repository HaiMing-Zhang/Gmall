package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
@Component
public class OrderConsumer {
    @Reference
    private OrderService orderService;
    @Reference
    private PaymentService paymentService;

    /**
     * activeMq消费,如果支付成功,更改ProcessStatus状态
     * @param mapMessage
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){
        //从消息队列中获取到orderId
        try {
            String orderId = mapMessage.getString("orderId");
            String result = mapMessage.getString("result");

            if("success".equals(result)){
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 消费ActiveMQ,判断支付是否成功,如果不成功则关闭订单
     * @param mapMessage
     */
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void checkQueue(MapMessage mapMessage){
        try {
            String outTradeNo = mapMessage.getString("outTradeNo");
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOutTradeNo(outTradeNo);
            boolean flag = paymentService.checkPayment(orderInfo);
            if(!flag){
                OrderInfo orderInfoQuery = orderService.getOrderInfoByOutTradeNo(outTradeNo);
                orderService.updateOrderStatus(orderInfoQuery.getId(),ProcessStatus.CLOSED);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
