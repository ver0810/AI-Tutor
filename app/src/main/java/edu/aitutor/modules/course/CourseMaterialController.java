package edu.aitutor.modules.course;

import edu.aitutor.common.annotation.RateLimit;
import edu.aitutor.common.result.Result;
import edu.aitutor.modules.course.model.*;
import edu.aitutor.modules.course.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 课程资料控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CourseMaterialController {

    private final CourseMaterialUploadService uploadService;
    private final CourseMaterialQueryService queryService;
    private final CourseMaterialListService listService;
    private final CourseMaterialDeleteService deleteService;
    private final CourseMaterialAnalysisService analysisService;

    /**
     * 获取课程资料分析报告
     */
    @GetMapping("/api/course/materials/{id}/analysis")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    public Result<CourseMaterialAnalysisResponse> analyzeMaterial(@PathVariable Long id) {
        return Result.success(analysisService.analyzeMaterial(id));
    }

    /**
     * 获取所有课程资料列表
     */
    @GetMapping("/api/course/materials/list")
    public Result<List<CourseMaterialListItemDTO>> getAllCourseMaterials(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "vectorStatus", required = false) String vectorStatus) {
        
        MaterialVectorStatus status = null;
        if (vectorStatus != null && !vectorStatus.isBlank()) {
            try {
                status = MaterialVectorStatus.valueOf(vectorStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.error("无效的向量化状态: " + vectorStatus);
            }
        }
        
        return Result.success(listService.listCourseMaterials(courseId, status, sortBy));
    }

    /**
     * 获取课程资料详情
     */
    @GetMapping("/api/course/materials/{id}")
    public Result<CourseMaterialListItemDTO> getCourseMaterial(@PathVariable Long id) {
        return listService.getCourseMaterial(id)
                .map(Result::success)
                .orElse(Result.error("课程资料不存在"));
    }

    /**
     * 删除课程资料
     */
    @DeleteMapping("/api/course/materials/{id}")
    public Result<Void> deleteCourseMaterial(@PathVariable Long id) {
        deleteService.deleteCourseMaterial(id);
        return Result.success(null);
    }

    /**
     * 基于课程资料回答问题（支持多资料）
     */
    @PostMapping("/api/course/materials/query")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 10)
    public Result<QueryResponse> queryCourseMaterial(@Valid @RequestBody QueryRequest request) {
        return Result.success(queryService.queryCourseMaterial(request));
    }

    /**
     * 基于课程资料回答问题（流式SSE，支持多资料）
     */
    @PostMapping(value = "/api/course/materials/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Flux<String> queryCourseMaterialStream(@Valid @RequestBody QueryRequest request) {
        log.debug("收到课程资料流式查询请求: materialIds={}, question={}, 线程: {} (虚拟线程: {})",
            request.courseMaterialIds(), request.question(), Thread.currentThread(), Thread.currentThread().isVirtual());
        return queryService.answerQuestionStream(request.courseMaterialIds(), request.question());
    }

    // ========== 分类管理 API ==========

    /**
     * 获取所有分类
     */
    @GetMapping("/api/course/materials/categories")
    public Result<List<String>> getAllCategories() {
        return Result.success(listService.getAllCategories());
    }

    /**
     * 根据分类获取课程资料列表
     */
    @GetMapping("/api/course/materials/category/{category}")
    public Result<List<CourseMaterialListItemDTO>> getByCategory(@PathVariable String category) {
        return Result.success(listService.listByCategory(category));
    }

    /**
     * 获取未分类的课程资料
     */
    @GetMapping("/api/course/materials/uncategorized")
    public Result<List<CourseMaterialListItemDTO>> getUncategorized() {
        return Result.success(listService.listByCategory(null));
    }

    /**
     * 更新课程资料分类
     */
    @PutMapping("/api/course/materials/{id}/category")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> body) {
        listService.updateCategory(id, body.get("category"));
        return Result.success(null);
    }

    // ========== 上传下载 API ==========

    /**
     * 上传课程资料文件
     */
    @PostMapping(value = "/api/course/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 3)
    public Result<Map<String, Object>> uploadCourseMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category) {
        return Result.success(uploadService.uploadCourseMaterial(file, courseId, name, category));
    }

    /**
     * 下载课程资料文件
     */
    @GetMapping("/api/course/materials/{id}/download")
    public ResponseEntity<byte[]> downloadCourseMaterial(@PathVariable Long id) {
        var entity = listService.getEntityForDownload(id);
        byte[] fileContent = listService.downloadFile(id);

        String filename = entity.getOriginalFilename();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_TYPE,
                        entity.getContentType() != null ? entity.getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(fileContent);
    }

    // ========== 搜索 API ==========

    /**
     * 搜索课程资料
     */
    @GetMapping("/api/course/materials/search")
    public Result<List<CourseMaterialListItemDTO>> search(@RequestParam("keyword") String keyword) {
        return Result.success(listService.search(keyword));
    }

    // ========== 统计 API ==========

    /**
     * 获取课程资料统计信息
     */
    @GetMapping("/api/course/materials/stats")
    public Result<CourseMaterialStatsDTO> getStatistics() {
        return Result.success(listService.getStatistics());
    }

    // ========== 向量化管理 API ==========

    /**
     * 重新向量化课程资料（手动重试）
     * 用于向量化失败后的重试
     */
    @PostMapping("/api/course/materials/{id}/revectorize")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    public Result<Void> revectorize(@PathVariable Long id) {
        uploadService.revectorize(id);
        return Result.success(null);
    }

}
