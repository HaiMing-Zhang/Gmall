package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取token
        String token = request.getParameter("newToken");
        if(token != null){
            //将token存入cookie
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        //如果token为null,则去cookie中取
        if(token == null){
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        //如果token不为null,则取出昵称,并放到作用域中
        if(token != null){
           Map<String,Object> map = getUserMapByToken(token);
           String nickName = (String) map.get("nickName");
           request.setAttribute("nickName",nickName);

        }
        //获取方法上的注解
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        CheckPriceRequire checkPriceRequire = handlerMethod.getMethodAnnotation(CheckPriceRequire.class);
        //此注解是自定义的注解,判断施加注解的控制器是否需要认证是否登录
        //如果loginRequire不等于null,说明此控制器必须登陆后才能访问
        if(loginRequire != null){
            //获取服务器ip地址
            String salt = request.getHeader("X-forwarded-for");
            //远程调用,进行认证是否登录
            //http://passport.atguigu.com/verify?token=""&salt=""
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            //如果result为success说明已经登录,否则未登录,重定向到登陆页面
            if("success".equals(result)){
                //已经登陆的状态下,渲染昵称nickName
                Map<String,Object> map = getUserMapByToken(token);
                String nickName = (String) map.get("nickName");
                String userId = (String)map.get("userId");
                request.setAttribute("nickName",nickName);
                request.setAttribute("userId",userId);
            }else {
                //判断页面是否必须登录,loginRequire注解中的autoRedirect如果为true,则必须登录
                if(loginRequire.autoRedirect()){
                //获取到当前页面的url
                String requestURL = request.getRequestURL().toString();
                //将页面的Url进行编码
                String pageUrl = URLEncoder.encode(requestURL, "UTF-8");
                //重定向到登陆页面
                //http://passport.atguigu.com/index?originUrl=""
                response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + pageUrl);
                return false;
                }
            }

        }

        if(checkPriceRequire != null){
            String result = HttpClientUtil.doGet("http://cart.gmall.com/checkPrice");
            if("success".equals(result)){
                return true;
            }
        }
        return true;
    }

    /**
     * 解码token,获取到用户信息
     * @param token
     * @return
     */
    private Map<String, Object> getUserMapByToken(String token) {
        //取出token的中间部分,用户信息
        String tokenUserInfo  = StringUtils.substringBetween(token, ".");
        //用Base64UrlCodec工具类进行解码JWT转码后的tokenUserInfo
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes  = base64UrlCodec.decode(tokenUserInfo);
        //将tokenBytes转换为map,但不能直接转换,所以先转换为String,在将String转换为Map
        String tokenString = new String(tokenBytes);
        //将string转换为Map
        Map map = JSON.parseObject(tokenString, Map.class);
        return map;
    }

}
