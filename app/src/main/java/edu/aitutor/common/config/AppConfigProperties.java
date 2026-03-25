package edu.aitutor.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 课程资料上传配置属性
 */
@Component
@ConfigurationProperties(prefix = "app.material")
public class AppConfigProperties {
    
    private String uploadDir;
    private List<String> allowedTypes;
    
    public String getUploadDir() {
        return uploadDir;
    }
    
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
    
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }
    
    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
}
