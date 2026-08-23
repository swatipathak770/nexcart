package com.nexcart.service;

import com.nexcart.dto.request.LoginRequest;
import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.dto.response.LoginResponse;
import com.nexcart.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
