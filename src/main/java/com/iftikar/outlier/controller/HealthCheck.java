package com.iftikar.outlier.controller;

import com.iftikar.outlier.result.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthCheck {
    @GetMapping
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        ApiResponse<String> response = new ApiResponse<>(
                "OK",
                "ran successfully",
                "Api successful"
        );

        return ResponseEntity.ok(response);
    }
}
