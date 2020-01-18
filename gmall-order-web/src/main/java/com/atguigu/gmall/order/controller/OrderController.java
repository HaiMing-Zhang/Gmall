package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.service.UserAddressService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller

public class OrderController {
    @Reference
    private UserAddressService userAddressService;
    @Reference
    private CartService cartService;
    @Reference
    private OrderService orderService;
    @Reference
    private PaymentService paymentService;
    @LoginRequire
    @RequestMapping("/trade")
    public String trade(HttpServletRequest request){
        //从作用域中获取userId
        String userId = (String)request.getAttribute("userId");
        //获取此用户的地址
        List<UserAddress> userAddressList = userAddressService.getUserAddressByUserId(userId);
        //将此地址集合放入作用域中
        request.setAttribute("userAddressList",userAddressList);
        //获取选中的购物车商品
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 订单信息集合
        List<OrderDetail> orderDetailList=new ArrayList<>(cartInfoList.size());
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList",orderDetailList);
        //创建订单信息,将订单信息集合放入,并计算价格
        OrderInfo orderInfo=new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        //获取流水号
        String tradeNo = orderService.getTradeNo(userId);

        request.setAttribute("tradeCode",tradeNo);
        return "trade";
    }

    /**
     * 保存订单
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("/submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        /**
         * 页面传回的参数orderInfo
         *  consignee:atguigu
         *  deliveryAddress:北京市海淀区
         *  paymentWay:ONLINE
         *  orderComment:
         *  orderDetailList[0].skuId:49
         *  orderDetailList[0].skuNum:3
         *  orderDetailList[0].orderPrice:10000.00
         *  orderDetailList[0].skuNume:小米max一亿像素
         */
        String userId = (String)request.getAttribute("userId");
        String tradeNo = request.getParameter("tradeNo");
        //判断此次提交订单的流水号是否和上一次提交相同,如果相同则报错
        if(!orderService.checkTradeCode(tradeNo,userId)){
            request.setAttribute("errMsg","不能重复提交订单！");
            return "tradeFail";
        }
        //放入userId
        orderInfo.setUserId(userId);
        //初始化订单状态:未支付
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //初始化进度状态:未支付
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //初始化商品的总金额
        orderInfo.sumTotalAmount();
        //获取商品信息
        //List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
       /* for (OrderDetail orderDetail : orderDetailList) {
            boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if(!flag){
                request.setAttribute("errMsg","商品库存不足，请重新下单！");
                return "tradeFail";
            }
        }*/
        //保存订单,并返回订单Id
        String orderId = orderService.saveOrder(orderInfo);
        OrderInfo orderInfoGetOutTradeNo = orderService.getOrderInfo(orderId);
        paymentService.closeOrderInfo(orderInfoGetOutTradeNo.getOutTradeNo(),15);
        //删除流水号
        orderService.deleteTradeCode(userId);
        // String orderId =
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }



}
