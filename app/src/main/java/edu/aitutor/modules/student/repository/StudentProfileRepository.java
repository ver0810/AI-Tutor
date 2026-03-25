package edu.aitutor.modules.student.repository;

import edu.aitutor.modules.student.model.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 简历Repository
 */
@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, Long> {
    
    /**
     * 根据文件哈希查找简历（用于去重）
     */
    Optional<StudentProfileEntity> findByFileHash(String fileHash);
    
    /**
     * 检查文件哈希是否存在
     */
    boolean existsByFileHash(String fileHash);
    
    /**
     * 根据课程ID查找教材
     */
    List<StudentProfileEntity> findByCourseIdOrderByUploadedAtDesc(Long courseId);
    
    /**
     * 查找未关联课程的通用教材
     */
    List<StudentProfileEntity> findByCourseIdIsNullOrderByUploadedAtDesc();
}
