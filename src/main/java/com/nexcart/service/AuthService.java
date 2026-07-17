package com.nexcart.service;

import com.nexcart.dto.request.LoginRequest;
import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.dto.response.LoginResponse;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

}
