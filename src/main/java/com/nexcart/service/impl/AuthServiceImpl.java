package com.nexcart.service.impl;

import com.nexcart.config.JwtProperties;
import com.nexcart.dto.request.LoginRequest;
import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.dto.response.LoginResponse;
import com.nexcart.dto.response.UserResponse;
import com.nexcart.entity.User;
import com.nexcart.exception.EmailAlreadyExistsException;
import com.nexcart.mapper.UserMapper;
import com.nexcart.repository.UserRepository;
import com.nexcart.security.CustomUserDetails;
import com.nexcart.security.JwtService;
import com.nexcart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        User user = userMapper.toEntity(request, passwordEncoder);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid email or password"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = jwtService.generateToken(userDetails);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration())
                .user(userMapper.toResponse(user))
                .build();
    }
}