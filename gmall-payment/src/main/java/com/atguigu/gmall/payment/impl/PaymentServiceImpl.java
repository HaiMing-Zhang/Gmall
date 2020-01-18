package com.atguigu.gmall.payment.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentMapper;
import com.atguigu.gmall.service.PaymentService;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;
import util.HttpClient;

import javax.jms.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private ActiveMQUtil activeMQUtil;
    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;
    /**
     * 保存支付信息
     * @param paymentInfo
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentMapper.insertSelective(paymentInfo);
    }

    /**
     * 查询支付信息
     * @param paymentInfo
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        PaymentInfo paymentInfo1 = paymentMapper.selectOne(paymentInfo);
        return paymentInfo1;
    }

    /**
     * 修改交易状态
     * @param paymentInfo
     */
    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());
        paymentMapper.updateByExampleSelective(paymentInfo,example);
    }

    /**
     * 退款
     * @return
     */
    @Override
    public boolean refund(String orderId) {
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //根据订单号查询支付信息
        PaymentInfo paymentInfo = getPaymentInfoByOrderId(orderId);
        Map map = new HashMap<String,String>();
        //设置订单编号
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        //设置提款总金额
        map.put("refund_amount",paymentInfo.getTotalAmount());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            //调用阿里支付宝退款接口
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    /**
     * 微信支付
     * @param orderId
     * @param total_fee
     * @return
     */
    @Override
    public Map createNative(String orderId, String 	total_fee) {
        HashMap<String,String> param = new HashMap<>();
        //微信支付分配的公众账号ID（企业号corpid即为此appId）
        param.put("appid",appid);
        //微信支付分配的商户号
        param.put("mch_id",partner);
        //随机字符串，长度要求在32位以内。
        param.put("nonce_str", WXPayUtil.generateNonceStr());
        param.put("body", "尚硅谷");//商品描述
        param.put("out_trade_no", orderId);//商户订单号
        param.put("total_fee",total_fee);//总金额（分）
        param.put("spbill_create_ip", "127.0.0.1");//IP
        param.put("notify_url", " http://2z72m78296.wicp.vip/wx/callback/notify");//回调地址(随便写)
        param.put("trade_type", "NATIVE");//交易类型
        try {
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println(xmlParam);
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            //转换为xml
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            String content = httpClient.getContent();
            System.out.println(content);
            //将xmp转为map
            Map<String, String> stringStringMap = WXPayUtil.xmlToMap(content);
            Map<String, String> map=new HashMap<>();
            map.put("code_url", stringStringMap.get("code_url"));//支付地址
            map.put("total_fee", total_fee);//总金额
            map.put("out_trade_no",orderId);//订单号
            return map;

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 支付成功通过activeMQ进行通知
     * @param orderId
     * @param result
     */
    @Override
    public void sendPaymentResult(String orderId, String result) {
        //从工具类中获取连接
        Connection connection = activeMQUtil.getConnection();
        try {
            //启动链接
            connection.start();
            //获取session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //创建消费者
            MessageProducer producer = session.createProducer(payment_result_queue);
            //创建map消息方式
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId",orderId);
            mapMessage.setString("result",result);
            producer.send(mapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /**
     * 根据out_trade_no判断支付宝支付是否成功
     * @param orderInfo
     * @return
     */
    @Override
    public boolean checkPayment(OrderInfo orderInfo) {
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        if(orderInfo == null){
            return false;
        }
        //传入参数,根据out_trade_no查询支付是否成功
        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            if("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())){
                System.out.println("调用成功");
                return true;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
        return false;
    }

    /**
     * 发送延迟队列,如果支付失败,则关闭订单
     * @param outTradeNo
     * @param delaySec
     */
    @Override
    public void closeOrderInfo(String outTradeNo, int delaySec) {
        //获取连接
        Connection connection = activeMQUtil.getConnection();
        try {
            //启动连接
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建延迟队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            //创建消息生产者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            //创建消息内容
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            // 开启延迟队列的参数设置
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            //发送消息
            producer.send(activeMQMapMessage);
            //提交
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据订单号查询支付信息
     * @param orderId
     * @return
     */
    private PaymentInfo getPaymentInfoByOrderId(String orderId) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        PaymentInfo paymentInfo = paymentMapper.selectOneByExample(example);
        return paymentInfo;
    }

}
