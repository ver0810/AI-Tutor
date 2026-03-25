package edu.aitutor.modules.student.listener;

import edu.aitutor.common.async.AbstractTaskSubscriber;
import edu.aitutor.common.constant.AsyncTaskStreamConstants;
import edu.aitutor.common.model.AsyncTaskStatus;
import edu.aitutor.infrastructure.redis.RedisService;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.repository.StudentProfileRepository;
import edu.aitutor.modules.student.service.StudentProfileAnalysisService;
import edu.aitutor.modules.student.service.StudentProfilePersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AnalyzeTaskSubscriber extends AbstractTaskSubscriber<AnalyzeTaskSubscriber.AnalyzePayload> {

    private final StudentProfileAnalysisService gradingService;
    private final StudentProfilePersistenceService persistenceService;
    private final StudentProfileRepository studentProfileRepository;

    public AnalyzeTaskSubscriber(
        RedisService redisService,
        StudentProfileAnalysisService gradingService,
        StudentProfilePersistenceService persistenceService,
        StudentProfileRepository studentProfileRepository
    ) {
        super(redisService);
        this.gradingService = gradingService;
        this.persistenceService = persistenceService;
        this.studentProfileRepository = studentProfileRepository;
    }

    record AnalyzePayload(Long studentProfileId, String content) {}

    @Override
    protected String taskDisplayName() {
        return "简历分析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.RESUME_ANALYZE_STREAM_KEY;
    }

    @Override
    protected String groupName() {
        return AsyncTaskStreamConstants.RESUME_ANALYZE_GROUP_NAME;
    }

    @Override
    protected String subscriberPrefix() {
        return AsyncTaskStreamConstants.PROFILE_ANALYZE_SUBSCRIBER_PREFIX;
    }

    @Override
    protected String threadName() {
        return "analyze-subscriber";
    }

    @Override
    protected AnalyzePayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String studentProfileIdStr = data.get(AsyncTaskStreamConstants.FIELD_RESUME_ID);
        String content = data.get(AsyncTaskStreamConstants.FIELD_CONTENT);
        if (studentProfileIdStr == null || content == null) {
            log.warn("消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        return new AnalyzePayload(Long.parseLong(studentProfileIdStr), content);
    }

    @Override
    protected String payloadIdentifier(AnalyzePayload payload) {
        return "studentProfileId=" + payload.studentProfileId();
    }

    @Override
    protected void markProcessing(AnalyzePayload payload) {
        updateAnalyzeStatus(payload.studentProfileId(), AsyncTaskStatus.PROCESSING, null);
    }

    @Override
    protected void processBusiness(AnalyzePayload payload) {
        Long studentProfileId = payload.studentProfileId();
        if (!studentProfileRepository.existsById(studentProfileId)) {
            log.warn("简历已被删除，跳过分析任务: studentProfileId={}", studentProfileId);
            return;
        }

        StudentProfileAnalysisResponse analysis = gradingService.analyzeStudentProfile(payload.content());
        StudentProfileEntity studentProfile = studentProfileRepository.findById(studentProfileId).orElse(null);
        if (studentProfile == null) {
            log.warn("简历在分析期间被删除，跳过保存结果: studentProfileId={}", studentProfileId);
            return;
        }
        persistenceService.saveAnalysis(studentProfile, analysis);
    }

    @Override
    protected void markCompleted(AnalyzePayload payload) {
        updateAnalyzeStatus(payload.studentProfileId(), AsyncTaskStatus.COMPLETED, null);
    }

    @Override
    protected void markFailed(AnalyzePayload payload, String error) {
        updateAnalyzeStatus(payload.studentProfileId(), AsyncTaskStatus.FAILED, error);
    }

    @Override
    protected void retryMessage(AnalyzePayload payload, int retryCount) {
        Long studentProfileId = payload.studentProfileId();
        String content = payload.content();
        try {
            Map<String, String> message = Map.of(
                AsyncTaskStreamConstants.FIELD_RESUME_ID, studentProfileId.toString(),
                AsyncTaskStreamConstants.FIELD_CONTENT, content,
                AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount)
            );

            redisService().streamAdd(
                AsyncTaskStreamConstants.RESUME_ANALYZE_STREAM_KEY,
                message,
                AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("简历分析任务已重新入队: studentProfileId={}, retryCount={}", studentProfileId, retryCount);

        } catch (Exception e) {
            log.error("重试入队失败: studentProfileId={}, error={}", studentProfileId, e.getMessage(), e);
            updateAnalyzeStatus(studentProfileId, AsyncTaskStatus.FAILED, truncateError("重试入队失败: " + e.getMessage()));
        }
    }

    /**
     * 更新分析状态
     */
    private void updateAnalyzeStatus(Long studentProfileId, AsyncTaskStatus status, String error) {
        try {
            studentProfileRepository.findById(studentProfileId).ifPresent(studentProfile -> {
                studentProfile.setAnalyzeStatus(status);
                studentProfile.setAnalyzeError(error);
                studentProfileRepository.save(studentProfile);
                log.debug("分析状态已更新: studentProfileId={}, status={}", studentProfileId, status);
            });
        } catch (Exception e) {
            log.error("更新分析状态失败: studentProfileId={}, status={}, error={}", studentProfileId, status, e.getMessage(), e);
        }
    }

}
