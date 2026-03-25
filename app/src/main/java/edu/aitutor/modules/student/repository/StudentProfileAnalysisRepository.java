package edu.aitutor.modules.student.repository;

import edu.aitutor.modules.student.model.StudentProfileAnalysisEntity;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 简历评测Repository
 */
@Repository
public interface StudentProfileAnalysisRepository extends JpaRepository<StudentProfileAnalysisEntity, Long> {
    
    /**
     * 根据简历查找所有评测记录
     */
    List<StudentProfileAnalysisEntity> findByStudentProfileOrderByAnalyzedAtDesc(StudentProfileEntity studentProfile);
    
    /**
     * 根据简历ID查找最新评测记录
     */
    StudentProfileAnalysisEntity findFirstByStudentProfileIdOrderByAnalyzedAtDesc(Long studentProfileId);
    
    /**
     * 根据简历ID查找所有评测记录
     */
    List<StudentProfileAnalysisEntity> findByStudentProfileIdOrderByAnalyzedAtDesc(Long studentProfileId);
}
