package com.atguigu.gmall.payment.interceptor;

import com.atguigu.gmall.common.bean.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    //public String userId;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("userId");
        if (userId == null){

            // 重定向登录页面：TODO：
            return false;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(Long.valueOf(userId));

        THREAD_LOCAL.set(userInfo);

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
