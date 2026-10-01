package com.sast.readtrack.service;
import com.sast.readtrack.common.ApiException;
import com.sast.readtrack.dto.Requests.Credentials;
import com.sast.readtrack.mapper.UserMapper;
import com.sast.readtrack.model.User;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class UserService {
    private final UserMapper users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public UserService(UserMapper users) { this.users = users; }
    public User register(Credentials request) {
        checkPasswordLength(request.password());
        User user = new User();
        user.setUsername(request.username().strip());
        user.setPassword(encoder.encode(request.password()));
        try { users.insert(user); }
        catch (DuplicateKeyException e) { throw new ApiException(409, "用户名已存在"); }
        return user;
    }
    public User login(Credentials request) {
        checkPasswordLength(request.password());
        User user = users.findByUsername(request.username().strip());
        if (user == null || !encoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        return user;
    }
    private void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(400, "密码 UTF-8 编码长度不能超过 72 字节");
        }
    }
}
