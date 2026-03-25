package edu.aitutor.modules.student.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.file.FileHashService;
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
 * 资料/简历持久化服务 (适配高校教研业务)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfilePersistenceService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final FileHashService fileHashService;
    
    public Optional<StudentProfileEntity> findExistingStudentProfile(MultipartFile file) {
        try {
            String fileHash = fileHashService.calculateHash(file);
            Optional<StudentProfileEntity> existing = studentProfileRepository.findByFileHash(fileHash);
            if (existing.isPresent()) {
                StudentProfileEntity profile = existing.get();
                profile.incrementAccessCount();
                studentProfileRepository.save(profile);
            }
            return existing;
        } catch (Exception e) {
            log.error("检查重复失败", e);
            return Optional.empty();
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileEntity saveStudentProfile(MultipartFile file, String text, String key, String url) {
        try {
            StudentProfileEntity profile = new StudentProfileEntity();
            profile.setFileHash(fileHashService.calculateHash(file));
            profile.setOriginalFilename(file.getOriginalFilename());
            profile.setFileSize(file.getSize());
            profile.setContentType(file.getContentType());
            profile.setStorageKey(key);
            profile.setStorageUrl(url);
            profile.setStudentProfileText(text);
            return studentProfileRepository.save(profile);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_UPLOAD_FAILED, "保存资料失败");
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileAnalysisEntity saveAnalysis(StudentProfileEntity profile, StudentProfileAnalysisResponse analysis) {
        try {
            StudentProfileAnalysisEntity entity = new StudentProfileAnalysisEntity();
            entity.setStudentProfile(profile);
            entity.setOverallScore(analysis.overallScore());
            entity.setDifficulty(analysis.difficulty());
            entity.setSummary(analysis.summary());
            
            // 序列化 JSON 字段
            entity.setTagsJson(objectMapper.writeValueAsString(analysis.tags()));
            entity.setLearningPathJson(objectMapper.writeValueAsString(analysis.learningPath()));

            return analysisRepository.save(entity);
        } catch (JacksonException e) {
            log.error("分析结果序列化失败", e);
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED, "序列化失败");
        }
    }

    /**
     * 获取最新分析实体
     */
    public Optional<StudentProfileAnalysisEntity> getLatestAnalysis(Long profileId) {
        return Optional.ofNullable(analysisRepository.findFirstByStudentProfileIdOrderByAnalyzedAtDesc(profileId));
    }

    /**
     * 获取历史分析列表
     */
    public List<StudentProfileAnalysisEntity> findAnalysesByStudentProfileId(Long profileId) {
        return analysisRepository.findByStudentProfileIdOrderByAnalyzedAtDesc(profileId);
    }

    /**
     * 获取所有资料实体
     */
    public List<StudentProfileEntity> findAllStudentProfiles() {
        return studentProfileRepository.findAll();
    }
    
    public Optional<StudentProfileAnalysisResponse> getLatestAnalysisAsDTO(Long profileId) {
        return getLatestAnalysis(profileId).map(this::entityToDTO);
    }
    
    public StudentProfileAnalysisResponse entityToDTO(StudentProfileAnalysisEntity entity) {
        try {
            List<String> tags = objectMapper.readValue(
                entity.getTagsJson() != null ? entity.getTagsJson() : "[]",
                new TypeReference<List<String>>() {}
            );
            
            List<StudentProfileAnalysisResponse.LearningStep> path = objectMapper.readValue(
                entity.getLearningPathJson() != null ? entity.getLearningPathJson() : "[]",
                new TypeReference<List<StudentProfileAnalysisResponse.LearningStep>>() {}
            );
            
            return new StudentProfileAnalysisResponse(
                entity.getSummary(),
                tags,
                path,
                entity.getOverallScore(),
                entity.getDifficulty(),
                entity.getStudentProfile().getStudentProfileText()
            );
        } catch (JacksonException e) {
            log.error("分析结果反序列化失败", e);
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED, "获取分析结果失败");
        }
    }
    
    public Optional<StudentProfileEntity> findById(Long id) {
        return studentProfileRepository.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStudentProfile(Long id) {
        studentProfileRepository.findById(id).ifPresent(p -> {
            analysisRepository.deleteAll(analysisRepository.findByStudentProfileIdOrderByAnalyzedAtDesc(id));
            studentProfileRepository.delete(p);
        });
    }
}
