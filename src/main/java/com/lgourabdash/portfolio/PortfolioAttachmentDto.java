package com.lgourabdash.portfolio;

import java.time.LocalDateTime;

public record PortfolioAttachmentDto(
        Long id,
        String kind,
        String title,
        String relatedKey,
        String fileName,
        String contentType,
        LocalDateTime createdAt,
        Boolean isActive
) {}
