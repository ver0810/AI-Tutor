package edu.aitutor.modules.student.listener;

import edu.aitutor.common.async.AbstractTaskPublisher;
import edu.aitutor.common.constant.AsyncTaskStreamConstants;
import edu.aitutor.common.model.AsyncTaskStatus;
import edu.aitutor.infrastructure.redis.RedisService;
import edu.aitutor.modules.student.repository.StudentProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AnalyzeTaskPublisher extends AbstractTaskPublisher<AnalyzeTaskPublisher.AnalyzeTaskPayload> {

    private final StudentProfileRepository studentProfileRepository;

    record AnalyzeTaskPayload(Long studentProfileId, String content) {}

    public AnalyzeTaskPublisher(RedisService redisService, StudentProfileRepository studentProfileRepository) {
        super(redisService);
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * 发送分析任务到 Redis Stream
     *
     * @param studentProfileId 简历ID
     * @param content  简历内容
     */
    public void publishAnalyzeTask(Long studentProfileId, String content) {
        publishTask(new AnalyzeTaskPayload(studentProfileId, content));
    }

    @Override
    protected String taskDisplayName() {
        return "分析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.RESUME_ANALYZE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(AnalyzeTaskPayload payload) {
        return Map.of(
            AsyncTaskStreamConstants.FIELD_RESUME_ID, payload.studentProfileId().toString(),
            AsyncTaskStreamConstants.FIELD_CONTENT, payload.content(),
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0"
        );
    }

    @Override
    protected String payloadIdentifier(AnalyzeTaskPayload payload) {
        return "studentProfileId=" + payload.studentProfileId();
    }

    @Override
    protected void onSendFailed(AnalyzeTaskPayload payload, String error) {
        updateAnalyzeStatus(payload.studentProfileId(), AsyncTaskStatus.FAILED, truncateError(error));
    }

    /**
     * 更新分析状态
     */
    private void updateAnalyzeStatus(Long studentProfileId, AsyncTaskStatus status, String error) {
        studentProfileRepository.findById(studentProfileId).ifPresent(studentProfile -> {
            studentProfile.setAnalyzeStatus(status);
            if (error != null) {
                studentProfile.setAnalyzeError(error.length() > 500 ? error.substring(0, 500) : error);
            }
            studentProfileRepository.save(studentProfile);
        });
    }
}
