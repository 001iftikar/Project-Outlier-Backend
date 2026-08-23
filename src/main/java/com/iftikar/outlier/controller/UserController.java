package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.dto.RegisterResponse;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


}






















