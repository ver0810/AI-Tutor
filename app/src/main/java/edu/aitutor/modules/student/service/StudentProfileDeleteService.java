package edu.aitutor.modules.student.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.file.FileStorageService;
import edu.aitutor.modules.tutoring.service.TutoringPersistenceService;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 简历删除服务
 * 处理简历删除的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileDeleteService {
    
    private final StudentProfilePersistenceService persistenceService;
    private final TutoringPersistenceService aitutorPersistenceService;
    private final FileStorageService storageService;
    
    /**
     * 删除简历
     * 
     * @param id 简历ID
     * @throws edu.aitutor.common.exception.BusinessException 如果简历不存在
     */
    public void deleteStudentProfile(Long id) {
        log.info("收到删除简历请求: id={}", id);
        
        // 获取简历信息（用于删除存储文件）
        StudentProfileEntity studentProfile = persistenceService.findById(id)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        
        // 1. 删除存储的文件（FileStorageService 已内置存在性检查）
        try {
            storageService.deleteStudentProfile(studentProfile.getStorageKey());
        } catch (Exception e) {
            log.warn("删除存储文件失败，继续删除数据库记录: {}", e.getMessage());
        }
        
        // 2. 删除面试会话（会自动删除面试答案）
        aitutorPersistenceService.deleteSessionsByStudentProfileId(id);
        
        // 3. 删除数据库记录（包括分析记录）
        persistenceService.deleteStudentProfile(id);
        
        log.info("简历删除完成: id={}", id);
    }
}

