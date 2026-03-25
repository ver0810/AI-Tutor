package edu.aitutor.modules.course.model;

import java.time.LocalDateTime;

/**
 * 知识库列表项DTO
 * 使用MapStruct进行转换，见CourseMaterialMapper
 */
public record CourseMaterialListItemDTO(
    Long id,
    String name,
    String category,
    String originalFilename,
    Long fileSize,
    String contentType,
    LocalDateTime uploadedAt,
    LocalDateTime lastAccessedAt,
    Integer accessCount,
    Integer questionCount,
    MaterialVectorStatus vectorStatus,
    String vectorError,
    Integer chunkCount
) {
}

