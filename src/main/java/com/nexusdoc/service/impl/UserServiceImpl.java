package com.nexusdoc.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.dto.UserLoginRequest;
import com.nexusdoc.dto.UserRegisterRequest;
import com.nexusdoc.entity.User;
import com.nexusdoc.mapper.UserMapper;
import com.nexusdoc.service.UserService;
import com.nexusdoc.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserLoginVO register(UserRegisterRequest request) {
        validateRegisterRequest(request);
        User existedUser = findByUsername(request.getUsername());
        if (existedUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(SecureUtil.sha256(request.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("用户注册成功，userId={}", user.getId());
        return UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public UserLoginVO login(UserLoginRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("用户名和密码不能为空");
        }

        User user = findByUsername(request.getUsername());
        if (user == null || !SecureUtil.sha256(request.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        log.info("用户登录成功，userId={}", user.getId());
        return UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getConfirmPassword())) {
            throw new BusinessException("注册信息不能为空");
        }
        if (request.getUsername().trim().length() < 3 || request.getUsername().trim().length() > 50) {
            throw new BusinessException("用户名长度必须在 3 到 50 个字符之间");
        }
        if (request.getPassword().length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username.trim())
                .last("LIMIT 1"));
    }
}
