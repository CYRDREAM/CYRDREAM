package com.sast.readtrack.service;

import com.sast.readtrack.dto.Requests.Credentials;
import com.sast.readtrack.mapper.UserMapper;
import com.sast.readtrack.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User register(Credentials request) {
        String username = request.username().strip();
        if (userMapper.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(request.password());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 唯一约束也能拦住同时提交的重复注册。
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        return user;
    }

    public User login(Credentials request) {
        User user = userMapper.findByUsername(request.username().strip());
        if (user == null || !user.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return user;
    }
}
