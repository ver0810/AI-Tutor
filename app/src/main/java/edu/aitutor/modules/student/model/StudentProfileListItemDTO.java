package edu.aitutor.modules.student.model;

import java.time.LocalDateTime;

/**
 * 简历列表项DTO
 */
public record StudentProfileListItemDTO(
    Long id,
    String filename,
    Long fileSize,
    LocalDateTime uploadedAt,
    Integer accessCount,
    Integer latestScore,
    LocalDateTime lastAnalyzedAt,
    Integer aitutorCount
) {}

