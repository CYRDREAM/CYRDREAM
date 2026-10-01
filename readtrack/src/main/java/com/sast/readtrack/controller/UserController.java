package com.sast.readtrack.controller;
import com.sast.readtrack.common.ApiResponse;
import com.sast.readtrack.dto.Requests.Credentials;
import com.sast.readtrack.service.UserService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> register(@Valid @RequestBody Credentials input) {
        var user = users.register(input);
        return ApiResponse.ok("注册成功", Map.of("id", user.getId(), "username", user.getUsername()));
    }
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody Credentials input, HttpServletRequest request) {
        var user = users.login(input);
        // 登录后创建新会话，避免复用登录前的会话 ID。
        var old = request.getSession(false);
        if (old != null) old.invalidate();
        var session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        return ApiResponse.ok("登录成功", Map.of("id", user.getId(), "username", user.getUsername()));
    }
    @GetMapping("/me")
    public ApiResponse<?> me(HttpSession session) {
        return ApiResponse.ok("查询成功", Map.of("id", session.getAttribute("userId"), "username", session.getAttribute("username")));
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) { session.invalidate(); return ApiResponse.ok("退出成功", null); }
}
