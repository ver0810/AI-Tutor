package edu.aitutor.modules.student;

import edu.aitutor.common.annotation.RateLimit;
import edu.aitutor.common.result.Result;
import edu.aitutor.modules.student.model.StudentProfileDetailDTO;
import edu.aitutor.modules.student.model.StudentProfileListItemDTO;
import edu.aitutor.modules.student.service.StudentProfileDeleteService;
import edu.aitutor.modules.student.service.StudentProfileHistoryService;
import edu.aitutor.modules.student.service.StudentProfileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 课程资料控制器 (业务重构: 简历 → 课程资料)
 * 
 * API映射: /api/studentProfiles/* (兼容保留)
 * 业务流程: 上传 → 解析 → AI分析 → PDF导出学习计划
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StudentProfileController {
    
    private final StudentProfileUploadService uploadService;
    private final StudentProfileDeleteService deleteService;
    private final StudentProfileHistoryService historyService;
    
    /**
     * 上传课程资料并获取分析结果
     *
     * @param file 课程资料文件（支持PDF、DOCX、DOC、TXT）
     * @return 课程资料分析结果，包含评分和建议
     */
    @PostMapping(value = "/api/studentProfiles/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<Map<String, Object>> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = uploadService.uploadAndAnalyze(file);
        boolean isDuplicate = (Boolean) result.get("duplicate");
        if (isDuplicate) {
            return Result.success("检测到相同资料，已返回历史分析结果", result);
        }
        return Result.success(result);
    }
    
    /**
     * 获取所有课程资料列表
     */
    @GetMapping("/api/studentProfiles")
    public Result<List<StudentProfileListItemDTO>> getAllStudentProfiles() {
        List<StudentProfileListItemDTO> studentProfiles = historyService.getAllStudentProfiles();
        return Result.success(studentProfiles);
    }
    
    /**
     * 获取课程资料详情（包含分析历史）
     */
    @GetMapping("/api/studentProfiles/{id}/detail")
    public Result<StudentProfileDetailDTO> getStudentProfileDetail(@PathVariable Long id) {
        StudentProfileDetailDTO detail = historyService.getStudentProfileDetail(id);
        return Result.success(detail);
    }
    
    /**
     * 导出课程资料分析报告为PDF
     */
    @GetMapping("/api/studentProfiles/{id}/export")
    public ResponseEntity<byte[]> exportAnalysisPdf(@PathVariable Long id) {
        try {
            var result = historyService.exportAnalysisPdf(id);
            String filename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.pdfBytes());
        } catch (Exception e) {
            log.error("导出PDF失败: studentProfileId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除课程资料
     *
     * @param id 课程资料ID
     * @return 删除结果
     */
    @DeleteMapping("/api/studentProfiles/{id}")
    public Result<Void> deleteStudentProfile(@PathVariable Long id) {
        deleteService.deleteStudentProfile(id);
        return Result.success(null);
    }

    /**
     * 重新分析课程资料（手动重试）
     * 用于分析失败后的重试
     *
     * @param id 课程资料ID
     * @return 结果
     */
    @PostMapping("/api/studentProfiles/{id}/reanalyze")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    public Result<Void> reanalyze(@PathVariable Long id) {
        uploadService.reanalyze(id);
        return Result.success(null);
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/api/studentProfiles/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
            "status", "UP",
            "service", "AI Tutor Platform - StudentProfile Service"
        ));
    }
    
}
