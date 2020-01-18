package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {
    /**
     * 查找所有用户
     * @return
     */
    List<UserInfo> findAll();

    /**
     * 根据名字获取用户
     * @return
     */
    UserInfo getUserByName(String name);
    /**
     * 根据名字模糊查询获取用户
     * @return
     */
    List<UserInfo> getUserByNameToLink(String name);

    /**
     * 插入用户
     */
    void insertUser(UserInfo userInfo);

    /**
     * 修改用户
     * @param userInfo
     */
    void updateByName(UserInfo userInfo);

    /**
     * 根据id删除用户
     */
    void deleteUserById(String id);

    /**
     * 判断用户登录是否正确
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 认证用户是否登录,从redis中查询用户信息
     * @param userId
     * @return
     */
    UserInfo verify(String userId);

    /**
     * 根据id获取用户
     * @param userId
     * @return
     */
    UserInfo getUserById(String userId);
}
