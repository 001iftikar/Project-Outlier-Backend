package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.DrawerUserInfoDto;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/drawer-user-info")
    ResponseEntity<ApiResponse<DrawerUserInfoDto>> getDrawerUserInfo(
            @AuthenticationPrincipal String userId
    ) {
        DrawerUserInfoDto info = userService.getDrawerUserInfo(userId);
        ApiResponse<DrawerUserInfoDto> response = new ApiResponse<>(
                "SUCCESS",
                "success",
                info
        );

        return ResponseEntity.ok(response);
    }
}






















