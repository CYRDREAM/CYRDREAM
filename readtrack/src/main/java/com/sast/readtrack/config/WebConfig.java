package com.sast.readtrack.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor login;
    public WebConfig(LoginInterceptor login) { this.login = login; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(login).addPathPatterns("/books", "/books/**", "/user/me", "/user/logout");
    }
}
