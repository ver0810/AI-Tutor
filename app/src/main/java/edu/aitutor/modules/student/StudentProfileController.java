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
 * 学生档案/资料控制器
 * 
 * API映射: /api/student/profiles/*
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/profiles")
public class StudentProfileController {
    
    private final StudentProfileUploadService uploadService;
    private final StudentProfileDeleteService deleteService;
    private final StudentProfileHistoryService historyService;
    
    /**
     * 上传并分析
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<Map<String, Object>> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = uploadService.uploadAndAnalyze(file);
        return Result.success(result);
    }
    
    /**
     * 获取列表
     */
    @GetMapping("")
    public Result<List<StudentProfileListItemDTO>> getAllStudentProfiles() {
        return Result.success(historyService.getAllStudentProfiles());
    }
    
    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    public Result<StudentProfileDetailDTO> getStudentProfileDetail(@PathVariable Long id) {
        return Result.success(historyService.getStudentProfileDetail(id));
    }
    
    /**
     * 导出 PDF (资料分析)
     */
    @GetMapping("/{id}/export/analysis")
    public ResponseEntity<byte[]> exportAnalysisPdf(@PathVariable Long id) {
        try {
            var result = historyService.exportAnalysisPdf(id);
            String filename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.pdfBytes());
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteStudentProfile(@PathVariable Long id) {
        deleteService.deleteStudentProfile(id);
        return Result.success(null);
    }

    /**
     * 重新分析
     */
    @PostMapping("/{id}/reanalyze")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    public Result<Void> reanalyze(@PathVariable Long id) {
        uploadService.reanalyze(id);
        return Result.success(null);
    }
}
