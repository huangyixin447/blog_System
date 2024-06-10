package com.itheima.web.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * @Classname WebMvcConfig
 * @Description TODO
 * @Date 2019-3-14 10:01
 * @Created by CrazyStone
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private BaseInterceptor baseInterceptor;

    @Override
    // 重写addInterceptors()方法，注册自定义拦截器
    public void addInterceptors(InterceptorRegistry registry) {
//        用于注册自定义拦截器的注册

        registry.addInterceptor(baseInterceptor);
    }
}

