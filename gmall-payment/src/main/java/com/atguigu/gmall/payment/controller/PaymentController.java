package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import util.IdWorker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class PaymentController {
    @Reference
    private OrderService orderService;
    @Reference
    private UserInfoService userInfoService;
    @Reference
    private PaymentService paymentService;
    @Autowired
    private AlipayClient alipayClient;
    @RequestMapping("/index")
    @LoginRequire
    public String index(String orderId,HttpServletRequest request){
        //String orderId = request.getParameter("orderId");
        String userId = (String)request.getAttribute("userId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        UserInfo userInfo = userInfoService.getUserById(userId);
        request.setAttribute("nickName",userInfo.getNickName());
        request.setAttribute("orderId",orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }
    //立即支付,支付宝支付
    @PostMapping("/alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response){
        // 获取订单Id
        String orderId = request.getParameter("orderId");
        // 取得订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("-----");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);

        // 保存信息
        paymentService.savePaymentInfo(paymentInfo);
        //填写支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        //设置同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //设置异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        //创建一个map，将需要的参数都装进map，在传给支付宝
        Map bizContnetMap = new HashMap<String,String>();
        //设置订单编号
        bizContnetMap.put("out_trade_no",paymentInfo.getOutTradeNo());
        //设置销售产品码，与支付宝签约的产品码名称。 注：目前仅支持FAST_INSTANT_TRADE_PAY
        bizContnetMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        //设置金额
        bizContnetMap.put("total_amount",paymentInfo.getTotalAmount());
        //支付标题
        bizContnetMap.put("subject",paymentInfo.getSubject());
        //转换为json字符串
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        return form;
    }

    //同步回调
    @RequestMapping("/alipay/callback/return")
    @ResponseBody
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }

    //异步回调
    @RequestMapping("/alipay/callback/notify")
    public String paymentNotify(@RequestParam Map<String,String> paramMap){
        boolean signVerified = false; //调用SDK验证签名
        try {
            //验证签名
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            String trade_status = paramMap.get("trade_status");
            if("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                //获取商户订单号
                String out_trade_no = paramMap.get("out_trade_no");
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoHas = paymentService.getPaymentInfo(paymentInfo);
                //判断paymentInfoHas的交易状态,如果为PAID或ClOSED则失败
                if (paymentInfoHas.getPaymentStatus()==PaymentStatus.PAID || paymentInfoHas.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                }
                //修改交易状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setOutTradeNo(out_trade_no);
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUpd.setCallbackTime(new Date());
                paymentService.updatePaymentInfo(paymentInfoUpd);

                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    /**
     * 退款；;;';;
     */
    @RequestMapping("/refund")
    @ResponseBody
    public String refund(String orderId){
        boolean flag = paymentService.refund(orderId);
        return flag+"";
    }

    @PostMapping("/wx/submit")
    @ResponseBody
    public Map createNative(){
        HashMap<String,String> map = new HashMap<>();
        IdWorker idWorker = new IdWorker();
        long orderId = idWorker.nextId();
        Map map = paymentService.createNative(orderId +"", "1");
        return map;
    }
    // 发送验证
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(String orderId,@RequestParam("result") String result){
        paymentService.sendPaymentResult(orderId,result);
        return "sent payment result";
    }

    /**
     * 根据out_trade_no查询支付是否成功
     * @param orderInfo
     * @return
     */
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(OrderInfo orderInfo){
        boolean flag = paymentService.checkPayment(orderInfo);
        return ""+flag;
    }
}
