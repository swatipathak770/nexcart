package com.nexcart.dto.response;

import com.nexcart.entity.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;

    private String fullName;

    private String email;

    private String phoneNumber;

    private Role role;
}
