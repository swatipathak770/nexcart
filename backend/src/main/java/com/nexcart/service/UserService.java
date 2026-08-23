package com.nexcart.service;

import com.nexcart.dto.request.ChangePasswordRequest;
import com.nexcart.dto.request.UpdateProfileRequest;
import com.nexcart.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse getProfile();

    UserResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    void deleteUser(Long id);

}
