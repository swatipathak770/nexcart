package com.nexcart.security;

import com.nexcart.config.SecurityConfig;
import com.nexcart.controller.AuthController;
import com.nexcart.controller.UserController;
import com.nexcart.config.JwtProperties;
import com.nexcart.dto.request.ChangePasswordRequest;
import com.nexcart.dto.request.LoginRequest;
import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.dto.request.UpdateProfileRequest;
import com.nexcart.dto.response.LoginResponse;
import com.nexcart.dto.response.UserResponse;
import com.nexcart.entity.Role;
import com.nexcart.service.AuthService;
import com.nexcart.service.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, AuthenticationSecurityTest.TestBeans.class})
class AuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationIsPublicAndIgnoresStaleBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer stale-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Riya Pandey",
                                  "email":"riya@example.com",
                                  "password":"strong-password",
                                  "phoneNumber":"9595722561"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginIsPublicAndIgnoresStaleBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer stale-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"riya@example.com","password":"strong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("signed-token"));
    }

    @Test
    void protectedCustomerEndpointRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAdminUserList() throws Exception {
        mockMvc.perform(get("/api/user/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminUserList() throws Exception {
        mockMvc.perform(get("/api/user/users"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        AuthService authService() {
            return new AuthService() {
                @Override
                public UserResponse register(RegisterRequest request) {
                    return customer();
                }

                @Override
                public LoginResponse login(LoginRequest request) {
                    return LoginResponse.builder()
                            .accessToken("signed-token")
                            .tokenType("Bearer")
                            .expiresIn(86400000)
                            .user(customer())
                            .build();
                }
            };
        }

        @Bean
        UserService userService() {
            return new UserService() {
                @Override
                public UserResponse getProfile() {
                    return customer();
                }

                @Override
                public UserResponse updateProfile(UpdateProfileRequest request) {
                    return customer();
                }

                @Override
                public void changePassword(ChangePasswordRequest request) { }

                @Override
                public Page<UserResponse> getAllUsers(Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                public UserResponse getUserById(Long id) {
                    return customer();
                }

                @Override
                public void deleteUser(Long id) { }
            };
        }

        @Bean
        JwtService jwtService() {
            return new JwtService(new JwtProperties()) {
                @Override
                public String extractUsername(String token) {
                    throw new AssertionError("Public auth routes must skip JWT parsing");
                }
            };
        }

        @Bean
        CustomUserDetailsService userDetailsService() {
            return new CustomUserDetailsService(null) {
                @Override
                public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String email) {
                    throw new AssertionError("Public auth routes must skip user lookup");
                }
            };
        }

        private static UserResponse customer() {
            return UserResponse.builder()
                    .id(1L)
                    .fullName("Riya Pandey")
                    .email("riya@example.com")
                    .phoneNumber("9595722561")
                    .role(Role.CUSTOMER)
                    .build();
        }
    }
}
