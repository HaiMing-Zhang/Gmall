package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.service.UserAddressService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class UserAddressServiceImpl implements UserAddressService {
    @Autowired
    private UserAddressMapper userAddressMapper;
    /**
     * 根据用户id查询用户地址
     * @return
     */
    @Override
    public List<UserAddress> getUserAddressByUserId(String UserId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(UserId);
        List<UserAddress> UserAdds = userAddressMapper.select(userAddress);
        return UserAdds;
    }
}
