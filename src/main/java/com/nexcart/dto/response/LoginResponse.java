package com.nexcart.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;

    private String tokenType;

    private UserResponse user;
}