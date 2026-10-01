package com.sast.readtrack.config;
import com.sast.readtrack.common.ApiException;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Long)) {
            throw new ApiException(401, "请先登录");
        }
        return true;
    }
}
