package com.nexcart.service.impl;

import com.nexcart.dto.request.ChangePasswordRequest;
import com.nexcart.dto.request.UpdateProfileRequest;
import com.nexcart.dto.response.UserResponse;
import com.nexcart.entity.User;
import com.nexcart.exception.InvalidPasswordException;
import com.nexcart.exception.UserNotFoundException;
import com.nexcart.mapper.UserMapper;
import com.nexcart.repository.UserRepository;
import com.nexcart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getProfile() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(UpdateProfileRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());

        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {

            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        userRepository.delete(user);
    }
}