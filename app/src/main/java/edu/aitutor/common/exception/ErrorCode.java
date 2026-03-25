package edu.aitutor.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // ========== 通用错误 1xxx ==========
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // ========== 学情资料模块错误 2xxx ==========
    STUDENT_PROFILE_NOT_FOUND(2001, "资料不存在"),
    STUDENT_PROFILE_PARSE_FAILED(2002, "资料解析失败"),
    STUDENT_PROFILE_UPLOAD_FAILED(2003, "资料上传失败"),
    STUDENT_PROFILE_DUPLICATE(2004, "资料已存在"),
    STUDENT_PROFILE_FILE_EMPTY(2005, "文件内容为空"),

    STUDENT_PROFILE_ANALYSIS_FAILED(2007, "资料分析失败"),
    STUDENT_PROFILE_ANALYSIS_NOT_FOUND(2008, "分析结果不存在"),

    // ========== 测评模块错误 3xxx ==========
    TUTORING_SESSION_NOT_FOUND(3001, "辅导会话不存在"),
    TUTORING_SESSION_EXPIRED(3002, "辅导会话已过期"),
    TUTORING_QUESTION_NOT_FOUND(3003, "测评题目不存在"),
    TUTORING_ALREADY_COMPLETED(3004, "测评已完成"),
    TUTORING_EVALUATION_FAILED(3005, "评估失败"),
    TUTORING_QUESTION_GENERATION_FAILED(3006, "题目生成失败"),
    TUTORING_NOT_COMPLETED(3007, "辅导会话尚未完成"),

    
    // ========== 存储模块错误 4xxx ==========
    STORAGE_UPLOAD_FAILED(4001, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(4002, "文件下载失败"),
    STORAGE_DELETE_FAILED(4003, "文件删除失败"),
    
    // ========== 导出模块错误 5xxx ==========
    EXPORT_PDF_FAILED(5001, "PDF导出失败"),
    
    // ========== 课程资料模块错误 6xxx ==========
    COURSE_MATERIAL_NOT_FOUND(6001, "资料不存在"),
    COURSE_MATERIAL_PARSE_FAILED(6002, "资料文件解析失败"),
    COURSE_MATERIAL_UPLOAD_FAILED(6003, "资料上传失败"),
    COURSE_MATERIAL_QUERY_FAILED(6004, "资料查询失败"),
    COURSE_MATERIAL_DELETE_FAILED(6005, "资料删除失败"),
    COURSE_MATERIAL_VECTORIZATION_FAILED(6006, "资料向量化失败"),
    
    // ========== AI服务错误 7xxx ==========
    AI_SERVICE_UNAVAILABLE(7001, "AI服务暂时不可用，请稍后重试"),
    AI_SERVICE_TIMEOUT(7002, "AI服务响应超时"),
    AI_SERVICE_ERROR(7003, "AI服务调用失败"),
    AI_API_KEY_INVALID(7004, "AI服务密钥无效"),
    AI_RATE_LIMIT_EXCEEDED(7005, "AI服务调用频率超限"),

    // ========== 限流模块错误 8xxx ==========
    RATE_LIMIT_EXCEEDED(8001, "请求过于频繁，请稍后再试");
    
    private final Integer code;
    private final String message;
}
