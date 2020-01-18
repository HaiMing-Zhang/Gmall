package com.atguigu.gmall.order.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.HttpClientUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderMapper;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;
    /**
     * 保存订单,并返回订单Id
     * @param orderInfo
     * @return
     */
    @Override
    public String saveOrder(OrderInfo orderInfo) {
        //设置订单创建时间
        orderInfo.setCreateTime(new Date());

        //设置订单过期时间,24小时
        //创建设置时间的类
        Calendar calendar = Calendar.getInstance();
        //设置时间为一天24小时
        calendar.add(Calendar.DATE,1);
        //设置到实体类bean中
        orderInfo.setExpireTime(calendar.getTime());

        //生成调用第三方支付时所需的支付编号
        //ATGUIGU+当前时间戳+1000以内的随机整数
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderMapper.insertSelective(orderInfo);

        //将订单商品信息也插入到表 order_detail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        return orderInfo.getId();
    }

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        String tradeNoKey="user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        String tradeCode = UUID.randomUUID().toString().replace("-", "");
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    /**
     * 比较流水号是否相同
     * @param tradeCodeNo
     * @param userId
     * @return
     */
    @Override
    public boolean checkTradeCode(String tradeCodeNo, String userId) {
        String tradeNoKey="user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        String tradeCodeNoJson = jedis.get(tradeNoKey);
        jedis.close();
        return tradeCodeNo.equals(tradeCodeNoJson);
    }

    /**
     * 删除流水号TradeNo
     * @param userId
     */
    @Override
    public void deleteTradeCode(String userId) {
        // 获取jedis 中的流水号
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey ="user:"+userId+":tradeCode";

        String tradeCode = jedis.get(tradeNoKey);


        // jedis.del(tradeNoKey);
        String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedis.eval(script, Collections.singletonList(tradeNoKey),Collections.singletonList(tradeCode));

        jedis.close();
    }

    /**
     * 验证库存
     * @param skuId
     * @param num
     * @return返回值	0：无库存   1：有库存
     */
    @Override
    public boolean checkStock(String skuId, Integer num) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + num);
        if("1".equals(result)){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 获取订单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        // 将orderDetai 放入orderInfo 中
        List<OrderDetail> select = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(select);
        return orderInfo;
    }

    /**
     * 根据分布式事务修改订单状态
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setId(orderId);
        //orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderMapper.updateByPrimaryKeySelective(orderInfo);
    }

    /**
     * 根据OutTradeNo查询订单
     * @return
     */
    @Override
    public OrderInfo getOrderInfoByOutTradeNo(String outTradeNo) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        return orderMapper.selectOne(orderInfo);

    }
}
