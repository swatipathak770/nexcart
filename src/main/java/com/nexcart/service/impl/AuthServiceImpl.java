package com.nexcart.service.impl;

import com.nexcart.dto.request.LoginRequest;
import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.dto.response.LoginResponse;
import com.nexcart.dto.response.UserResponse;
import com.nexcart.entity.User;
import com.nexcart.entity.Role;
import com.nexcart.exception.EmailAlreadyExistsException;
import com.nexcart.repository.UserRepository;
import com.nexcart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        User savedUser = userRepository.save(user);

        UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return null;
    }
}