package edu.aitutor.common.constant;

/**
 * 异步任务 Redis Stream 通用常量
 * 包含知识库向量化和简历分析两个异步任务的配置
 */
public final class AsyncTaskStreamConstants {

    private AsyncTaskStreamConstants() {
        // 私有构造函数，防止实例化
    }

    // ========== 通用消息字段 ==========

    /**
     * 重试次数字段
     */
    public static final String FIELD_RETRY_COUNT = "retryCount";

    /**
     * 文档内容字段
     */
    public static final String FIELD_CONTENT = "content";

    public static final int MAX_RETRY_COUNT = 3;

    public static final int BATCH_SIZE = 10;

    public static final long POLL_INTERVAL_MS = 1000;

    public static final int STREAM_MAX_LEN = 1000;

    // ========== 知识库向量化 Stream 配置 ==========

    public static final String KB_VECTORIZE_STREAM_KEY = "knowledgebase:vectorize:stream";

    public static final String KB_VECTORIZE_GROUP_NAME = "vectorize-group";

    public static final String KB_VECTORIZE_SUBSCRIBER_PREFIX = "vectorize-subscriber-";

    /**
     * 知识库ID字段
     */
    public static final String FIELD_KB_ID = "kbId";

    // ========== 简历分析 Stream 配置 ==========

    public static final String RESUME_ANALYZE_STREAM_KEY = "resume:analyze:stream";

    public static final String RESUME_ANALYZE_GROUP_NAME = "analyze-group";

    public static final String PROFILE_ANALYZE_SUBSCRIBER_PREFIX = "analyze-subscriber-";

    public static final String FIELD_RESUME_ID = "studentProfileId";

    // ========== 辅导评估 Stream 配置 ==========

    public static final String TUTOR_EVALUATE_STREAM_KEY = "aitutor:evaluate:stream";

    public static final String TUTOR_EVALUATE_GROUP_NAME = "evaluate-group";

    public static final String TUTOR_EVALUATE_SUBSCRIBER_PREFIX = "evaluate-subscriber-";

    public static final String FIELD_SESSION_ID = "sessionId";
}
