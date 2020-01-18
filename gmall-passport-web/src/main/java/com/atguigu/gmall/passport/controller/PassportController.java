package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.JwtUtil;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {
    @Reference
    private UserInfoService userInfoService;
    @Value("${token.key}")
    private String key;

    /**
     * 登录页
     * @param request
     * @return
     */
    @RequestMapping("/index")
    public String index(HttpServletRequest request){
        //获取url地址栏中,用户访问前所在的页面
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    /**
     * 生成token
     * @param request
     * @param userInfo
     * @return
     */
    @RequestMapping("/login")
    @ResponseBody
    public String login(HttpServletRequest request,UserInfo userInfo){
        //取得服务器的ip地址
        String salt = request.getHeader("X-forwarded-for");
        UserInfo info = userInfoService.login(userInfo);
        if(info != null){
            //取出userId与nickName
            Map<String,Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
            //用JWT编码生成token
            String token = JwtUtil.encode(key, map, salt);
            System.out.println(token);
            return token;
        }
        return "fail";
    }

    /**
     * 认证用户是否登录
     * 解码token,拼成key userKey_prefix+info.getId()+userinfoKey_suffix ，根据key从redis查询,
     *  如果有则已登录
     * @param token
     * @param salt
     * @return
     */
    @RequestMapping("/verify")
    @ResponseBody
    public String verify(String token,String salt){
        //取出token中间的部分
        // String tokenString= StringUtils.substringBetween(token, ".");
        //解码
        Map<String, Object> skuInfoMap = JwtUtil.decode(token, key, salt);
        //从map中取出userId
        String userId = (String)skuInfoMap.get("userId");
        //从redis中获取用户信息
        UserInfo userInfo = userInfoService.verify(userId);
        if(userInfo != null){
            return "success";
        }else{
            return "fail";

        }
    }

}
