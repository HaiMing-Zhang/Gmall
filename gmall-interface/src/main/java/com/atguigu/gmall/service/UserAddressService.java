package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserAddressService {
    /**
     * 根据用户id查询用户地址
     * @return
     */
    List<UserAddress> getUserAddressByUserId(String UserId);
}
