package com.nexcart.service.impl;

import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.entity.User;
import com.nexcart.entity.Role;
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
            throw new RuntimeException("Email already exists.");
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(Role.CUSTOMER);

        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        userRepository.save(user);
    }
}
