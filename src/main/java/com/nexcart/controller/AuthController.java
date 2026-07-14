package com.nexcart.controller;

import com.nexcart.dto.request.RegisterRequest;
import com.nexcart.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return "User Registered Successfully";
    }

}
