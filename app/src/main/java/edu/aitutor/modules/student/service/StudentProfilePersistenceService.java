package edu.aitutor.modules.student.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.file.FileHashService;
import edu.aitutor.infrastructure.mapper.StudentProfileMapper;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.student.model.StudentProfileAnalysisEntity;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.repository.StudentProfileAnalysisRepository;
import edu.aitutor.modules.student.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 简历持久化服务
 * 简历和评测结果的持久化，简历删除时删除所有关联数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfilePersistenceService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final FileHashService fileHashService;
    
    /**
     * 检查简历是否已存在（基于文件内容hash）
     * 
     * @param file 上传的文件
     * @return 如果存在返回已有的简历实体，否则返回空
     */
    public Optional<StudentProfileEntity> findExistingStudentProfile(MultipartFile file) {
        try {
            String fileHash = fileHashService.calculateHash(file);
            Optional<StudentProfileEntity> existing = studentProfileRepository.findByFileHash(fileHash);
            
            if (existing.isPresent()) {
                log.info("检测到重复简历: hash={}", fileHash);
                StudentProfileEntity studentProfile = existing.get();
                studentProfile.incrementAccessCount();
                studentProfileRepository.save(studentProfile);
            }
            
            return existing;
        } catch (Exception e) {
            log.error("检查简历重复时出错: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 保存新简历
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileEntity saveStudentProfile(MultipartFile file, String studentProfileText,
                                   String storageKey, String storageUrl) {
        try {
            String fileHash = fileHashService.calculateHash(file);
            
            StudentProfileEntity studentProfile = new StudentProfileEntity();
            studentProfile.setFileHash(fileHash);
            studentProfile.setOriginalFilename(file.getOriginalFilename());
            studentProfile.setFileSize(file.getSize());
            studentProfile.setContentType(file.getContentType());
            studentProfile.setStorageKey(storageKey);
            studentProfile.setStorageUrl(storageUrl);
            studentProfile.setStudentProfileText(studentProfileText);
            
            StudentProfileEntity saved = studentProfileRepository.save(studentProfile);
            log.info("简历已保存: id={}, hash={}", saved.getId(), fileHash);
            
            return saved;
        } catch (Exception e) {
            log.error("保存简历失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_UPLOAD_FAILED, "保存简历失败");
        }
    }
    
    /**
     * 保存简历评测结果
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileAnalysisEntity saveAnalysis(StudentProfileEntity studentProfile, StudentProfileAnalysisResponse analysis) {
        try {
            // 使用 MapStruct 映射基础字段
            StudentProfileAnalysisEntity entity = studentProfileMapper.toAnalysisEntity(analysis);
            entity.setStudentProfile(studentProfile);

            // JSON 字段需要手动序列化
            entity.setStrengthsJson(objectMapper.writeValueAsString(analysis.strengths()));
            entity.setSuggestionsJson(objectMapper.writeValueAsString(analysis.suggestions()));

            StudentProfileAnalysisEntity saved = analysisRepository.save(entity);
            log.info("简历评测结果已保存: analysisId={}, studentProfileId={}, score={}",
                    saved.getId(), studentProfile.getId(), analysis.overallScore());

            return saved;
        } catch (JacksonException e) {
            log.error("序列化评测结果失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED, "保存评测结果失败");
        }
    }
    
    /**
     * 获取简历的最新评测结果
     */
    public Optional<StudentProfileAnalysisEntity> getLatestAnalysis(Long studentProfileId) {
        return Optional.ofNullable(analysisRepository.findFirstByStudentProfileIdOrderByAnalyzedAtDesc(studentProfileId));
    }
    
    /**
     * 获取简历的最新评测结果（返回DTO）
     */
    public Optional<StudentProfileAnalysisResponse> getLatestAnalysisAsDTO(Long studentProfileId) {
        return getLatestAnalysis(studentProfileId).map(this::entityToDTO);
    }
    
    /**
     * 获取所有简历列表
     */
    public List<StudentProfileEntity> findAllStudentProfiles() {
        return studentProfileRepository.findAll();
    }
    
    /**
     * 获取简历的所有评测记录
     */
    public List<StudentProfileAnalysisEntity> findAnalysesByStudentProfileId(Long studentProfileId) {
        return analysisRepository.findByStudentProfileIdOrderByAnalyzedAtDesc(studentProfileId);
    }
    
    /**
     * 将实体转换为DTO
     */
    public StudentProfileAnalysisResponse entityToDTO(StudentProfileAnalysisEntity entity) {
        try {
            List<String> strengths = objectMapper.readValue(
                entity.getStrengthsJson() != null ? entity.getStrengthsJson() : "[]",
                    new TypeReference<>() {
                    }
            );
            
            List<StudentProfileAnalysisResponse.Suggestion> suggestions = objectMapper.readValue(
                entity.getSuggestionsJson() != null ? entity.getSuggestionsJson() : "[]",
                    new TypeReference<>() {
                    }
            );
            
            return new StudentProfileAnalysisResponse(
                entity.getOverallScore(),
                studentProfileMapper.toScoreDetail(entity),  // 使用MapStruct自动映射
                entity.getSummary(),
                strengths,
                suggestions,
                entity.getStudentProfile().getStudentProfileText()
            );
        } catch (JacksonException e) {
            log.error("反序列化评测结果失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED, "获取评测结果失败");
        }
    }
    
    /**
     * 根据ID获取简历
     */
    public Optional<StudentProfileEntity> findById(Long id) {
        return studentProfileRepository.findById(id);
    }
    
    /**
     * 删除简历及其所有关联数据
     * 包括：简历分析记录、面试会话（会自动删除面试答案）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudentProfile(Long id) {
        Optional<StudentProfileEntity> studentProfileOpt = studentProfileRepository.findById(id);
        if (studentProfileOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND);
        }
        
        StudentProfileEntity studentProfile = studentProfileOpt.get();
        
        // 1. 删除所有简历分析记录
        List<StudentProfileAnalysisEntity> analyses = analysisRepository.findByStudentProfileIdOrderByAnalyzedAtDesc(id);
        if (!analyses.isEmpty()) {
            analysisRepository.deleteAll(analyses);
            log.info("已删除 {} 条简历分析记录", analyses.size());
        }
        
        // 2. 删除简历实体（面试会话会在服务层删除）
        studentProfileRepository.delete(studentProfile);
        log.info("简历已删除: id={}, filename={}", id, studentProfile.getOriginalFilename());
    }
}
