package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserAddressController {
    @Reference
    private UserAddressService userAddressService;
    @RequestMapping("/findUserAdd")
    public List<UserAddress> findUserAdd(String userId){
        return userAddressService.getUserAddressByUserId(userId);
    }


}
