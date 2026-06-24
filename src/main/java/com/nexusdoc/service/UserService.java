package com.nexusdoc.service;

import com.nexusdoc.dto.UserLoginRequest;
import com.nexusdoc.dto.UserRegisterRequest;
import com.nexusdoc.vo.UserLoginVO;

public interface UserService {

    UserLoginVO register(UserRegisterRequest request);

    UserLoginVO login(UserLoginRequest request);
}
