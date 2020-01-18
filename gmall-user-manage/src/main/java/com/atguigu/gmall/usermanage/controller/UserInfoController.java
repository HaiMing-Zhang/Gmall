package com.atguigu.gmall.usermanage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserInfoController {
    @Reference
    private UserInfoService userInfoService;

    /**
     * 查询所有用户
     * @return
     */
    @GetMapping("/findAll")
    public List<UserInfo> findAll(){
        return userInfoService.findAll();
    }

    /**
     * 根据name查询用户
     * @param name
     * @return
     */
    @GetMapping("/getUserByName")
    public UserInfo getUserByName(@RequestParam("name") String name){
        return userInfoService.getUserByName(name);
    }

    /**
     * 根据name模糊查询用户
     * @param name
     * @return
     */
    @GetMapping("/getUserByNameToLink")
    public List<UserInfo> getUserByNameToLink(@RequestParam("name") String name){
        return userInfoService.getUserByNameToLink(name);
    }

    /**
     * 插入用户
     * @param userInfo
     */
    @GetMapping("/insertByUser")
    public void insertByUser(UserInfo userInfo){
        userInfoService.insertUser(userInfo);
    }

    /**
     * 插入用户
     * @param userInfo
     */
    @GetMapping("/updateByUser")
    public void updateByUser(UserInfo userInfo){
        userInfoService.updateByName(userInfo);
    }

    /**
     * 根据id删除用户
     * @param id
     */
    @GetMapping("/deleteUserById")
    public void deleteUserById(String id){
        userInfoService.deleteUserById(id);
    }

}
