package com.nexcart.service;

import com.nexcart.dto.request.ChangePasswordRequest;
import com.nexcart.dto.request.UpdateProfileRequest;
import com.nexcart.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse getProfile();

    UserResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(Long id);

    void deleteUser(Long id);

}
