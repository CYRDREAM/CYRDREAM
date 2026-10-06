package com.sast.readtrack.controller;

import com.sast.readtrack.common.ApiResponse;
import com.sast.readtrack.dto.Requests.Credentials;
import com.sast.readtrack.model.User;
import com.sast.readtrack.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> register(@Valid @RequestBody Credentials input) {
        User user = userService.register(input);
        return ApiResponse.ok("注册成功", Map.of("id", user.getId(), "username", user.getUsername()));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody Credentials input) {
        User user = userService.login(input);
        return ApiResponse.ok("登录成功", Map.of("id", user.getId(), "username", user.getUsername()));
    }
}
