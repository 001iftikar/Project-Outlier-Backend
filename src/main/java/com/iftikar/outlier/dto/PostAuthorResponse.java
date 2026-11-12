package com.iftikar.outlier.dto;

import java.util.List;

public record PostAuthorResponse(
        String id,
        String username,
        String name,
        List<String> techStack
) {}