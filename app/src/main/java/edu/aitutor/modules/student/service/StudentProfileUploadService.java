package edu.aitutor.modules.student.service;

import edu.aitutor.common.config.AppConfigProperties;
import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.common.model.AsyncTaskStatus;
import edu.aitutor.infrastructure.file.FileStorageService;
import edu.aitutor.infrastructure.file.FileValidationService;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.student.listener.AnalyzeStreamProducer;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * 简历上传服务
 * 处理简历上传、解析的业务逻辑
 * AI 分析改为异步处理，通过 Redis Stream 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileUploadService {

    private final StudentProfileParseService parseService;
    private final FileStorageService storageService;
    private final StudentProfilePersistenceService persistenceService;
    private final AppConfigProperties appConfig;
    private final FileValidationService fileValidationService;
    private final AnalyzeStreamProducer analyzeStreamProducer;
    private final StudentProfileRepository studentProfileRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 上传并分析简历（异步）
     *
     * @param file 简历文件
     * @return 上传结果（分析将异步进行）
     */
    public Map<String, Object> uploadAndAnalyze(org.springframework.web.multipart.MultipartFile file) {
        // 1. 验证文件
        fileValidationService.validateFile(file, MAX_FILE_SIZE, "简历");

        String fileName = file.getOriginalFilename();
        log.info("收到简历上传请求: {}, 大小: {} bytes", fileName, file.getSize());

        // 2. 验证文件类型
        String contentType = parseService.detectContentType(file);
        validateContentType(contentType);

        // 3. 检查简历是否已存在（去重）
        Optional<StudentProfileEntity> existingStudentProfile = persistenceService.findExistingStudentProfile(file);
        if (existingStudentProfile.isPresent()) {
            return handleDuplicateStudentProfile(existingStudentProfile.get());
        }

        // 4. 解析简历文本
        String studentProfileText = parseService.parseStudentProfile(file);
        if (studentProfileText == null || studentProfileText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_PARSE_FAILED, "无法从文件中提取文本内容，请确保文件不是扫描版PDF");
        }

        // 5. 保存简历到RustFS
        String fileKey = storageService.uploadStudentProfile(file);
        String fileUrl = storageService.getFileUrl(fileKey);
        log.info("简历已存储到RustFS: {}", fileKey);

        // 6. 保存简历到数据库（状态为 PENDING）
        StudentProfileEntity savedStudentProfile = persistenceService.saveStudentProfile(file, studentProfileText, fileKey, fileUrl);

        // 7. 发送分析任务到 Redis Stream（异步处理）
        analyzeStreamProducer.sendAnalyzeTask(savedStudentProfile.getId(), studentProfileText);

        log.info("简历上传完成，分析任务已入队: {}, studentProfileId={}", fileName, savedStudentProfile.getId());

        // 8. 返回结果（状态为 PENDING，前端可轮询获取最新状态）
        return Map.of(
            "studentProfile", Map.of(
                "id", savedStudentProfile.getId(),
                "filename", savedStudentProfile.getOriginalFilename(),
                "analyzeStatus", AsyncTaskStatus.PENDING.name()
            ),
            "storage", Map.of(
                "fileKey", fileKey,
                "fileUrl", fileUrl,
                "studentProfileId", savedStudentProfile.getId()
            ),
            "duplicate", false
        );
    }

    /**
     * 验证文件类型
     */
    private void validateContentType(String contentType) {
        fileValidationService.validateContentTypeByList(
            contentType,
            appConfig.getAllowedTypes(),
            "不支持的文件类型: " + contentType
        );
    }

    /**
     * 处理重复简历
     */
    private Map<String, Object> handleDuplicateStudentProfile(StudentProfileEntity studentProfile) {
        log.info("检测到重复简历，返回历史分析结果: studentProfileId={}", studentProfile.getId());

        // 获取历史分析结果
        Optional<StudentProfileAnalysisResponse> analysisOpt = persistenceService.getLatestAnalysisAsDTO(studentProfile.getId());

        // 已有分析结果，直接返回
        // 没有分析结果（可能之前分析失败），返回当前状态
        return analysisOpt.map(studentProfileAnalysisResponse -> Map.of(
                "analysis", studentProfileAnalysisResponse,
                "storage", Map.of(
                        "fileKey", studentProfile.getStorageKey() != null ? studentProfile.getStorageKey() : "",
                        "fileUrl", studentProfile.getStorageUrl() != null ? studentProfile.getStorageUrl() : "",
                        "studentProfileId", studentProfile.getId()
                ),
                "duplicate", true
        )).orElseGet(() -> Map.of(
                "studentProfile", Map.of(
                        "id", studentProfile.getId(),
                        "filename", studentProfile.getOriginalFilename(),
                        "analyzeStatus", studentProfile.getAnalyzeStatus() != null ? studentProfile.getAnalyzeStatus().name() : AsyncTaskStatus.PENDING.name()
                ),
                "storage", Map.of(
                        "fileKey", studentProfile.getStorageKey() != null ? studentProfile.getStorageKey() : "",
                        "fileUrl", studentProfile.getStorageUrl() != null ? studentProfile.getStorageUrl() : "",
                        "studentProfileId", studentProfile.getId()
                ),
                "duplicate", true
        ));
    }

    /**
     * 重新分析简历（手动重试）
     * 从数据库获取简历文本并发送分析任务
     *
     * @param studentProfileId 简历ID
     */
    @Transactional
    public void reanalyze(Long studentProfileId) {
        StudentProfileEntity studentProfile = studentProfileRepository.findById(studentProfileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND, "简历不存在"));

        log.info("开始重新分析简历: studentProfileId={}, filename={}", studentProfileId, studentProfile.getOriginalFilename());

        String studentProfileText = studentProfile.getStudentProfileText();
        if (studentProfileText == null || studentProfileText.trim().isEmpty()) {
            // 如果没有缓存的文本，尝试重新解析
            studentProfileText = parseService.downloadAndParseContent(studentProfile.getStorageKey(), studentProfile.getOriginalFilename());
            if (studentProfileText == null || studentProfileText.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.STUDENT_PROFILE_PARSE_FAILED, "无法获取简历文本内容");
            }
            // 更新缓存的文本
            studentProfile.setStudentProfileText(studentProfileText);
        }

        // 更新状态为 PENDING
        studentProfile.setAnalyzeStatus(AsyncTaskStatus.PENDING);
        studentProfile.setAnalyzeError(null);
        studentProfileRepository.save(studentProfile);

        // 发送分析任务到 Stream
        analyzeStreamProducer.sendAnalyzeTask(studentProfileId, studentProfileText);

        log.info("重新分析任务已发送: studentProfileId={}", studentProfileId);
    }
}
