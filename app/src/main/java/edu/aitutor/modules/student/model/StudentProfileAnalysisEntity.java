package edu.aitutor.modules.student.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 资料分析结果实体 (业务重构: 简历分析 → 资料分析)
 */
@Entity
@Table(name = "student_profile_analyses")
public class StudentProfileAnalysisEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 关联的简历/档案
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfileEntity studentProfile;
    
    // 综合评分 (0-100)
    private Integer overallScore;
    
    // 资料难度 (1-5)
    private Integer difficulty;
    
    // 核心总结
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    // 知识点标签 (JSON格式)
    @Column(columnDefinition = "TEXT")
    private String tagsJson;
    
    // 学习路径 (JSON格式)
    @Column(columnDefinition = "TEXT")
    private String learningPathJson;
    
    // 评测时间
    @Column(nullable = false)
    private LocalDateTime analyzedAt;
    
    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public StudentProfileEntity getStudentProfile() {
        return studentProfile;
    }
    
    public void setStudentProfile(StudentProfileEntity studentProfile) {
        this.studentProfile = studentProfile;
    }
    
    public Integer getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getTagsJson() {
        return tagsJson;
    }
    
    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }
    
    public String getLearningPathJson() {
        return learningPathJson;
    }
    
    public void setLearningPathJson(String learningPathJson) {
        this.learningPathJson = learningPathJson;
    }
    
    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
    
    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
