package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {
    @Autowired
    private AuthInterceptor authInterceptor;
    //添加拦截器
    /*
     *  <mvc:interceptors>
            <mvc:interceptor pathPatterns="/**">
            </mvc:interceptor>
        </mvc:interceptors>
     */
    public void addInterceptors(InterceptorRegistry registry){
        //将自定义的拦截器authInterceptor添加进来,"/**"代表拦截所有
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
        super.addInterceptors(registry);
    }
}
