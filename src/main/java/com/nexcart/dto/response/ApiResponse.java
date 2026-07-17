package com.nexcart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;

    private String message;

    private T data;

    private LocalDateTime timestamp;
}
