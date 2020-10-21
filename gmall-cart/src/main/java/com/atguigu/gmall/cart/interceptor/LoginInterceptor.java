package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    //public String userId;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Autowired
    private JwtProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在购物车微服务中统一获取用户的登录状态：userId userKey
        UserInfo userInfo = new UserInfo();

        // 1.获取cookie中的Token 以及 userKey
        String userKey = CookieUtils.getCookieValue(request, this.properties.getUserKey());
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, this.properties.getUserKey(), userKey, this.properties.getExpire());
        }
        userInfo.setUserKey(userKey);

        // 判断token是否为空，如果为空，直接传递userKey即可
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        if (StringUtils.isBlank(token)){
            THREAD_LOCAL.set(userInfo);
            return true;
        }

        try {
            // 如果token不为空，解析jwt类型的token获取userId传递给后续业务
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            userInfo.setUserId(Long.valueOf(map.get("userId").toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        THREAD_LOCAL.set(userInfo);
        // 购物车拦截器中不管有没有登录都要放行
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 由于使用的tomcat线程池，请求结束，线程没有结束，只是还回线程池。所有必须手动释放ThreadLocal
        // 避免内存泄漏发生
        THREAD_LOCAL.remove();
    }
}
