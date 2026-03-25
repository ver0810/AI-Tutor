package edu.aitutor.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 助教系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.tutor")
public class TutorConfigProperties {
    
    private int followUpCount = 1;
    private EvaluationConfig evaluation = new EvaluationConfig();
    
    @Data
    public static class EvaluationConfig {
        private int batchSize = 8;
    }
}
