package edu.aitutor.modules.course.repository;

import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.model.MaterialVectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库Repository
 */
@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterialEntity, Long> {

    /**
     * 根据文件哈希查找知识库（用于去重）
     */
    Optional<CourseMaterialEntity> findByFileHash(String fileHash);

    /**
     * 检查文件哈希是否存在
     */
    boolean existsByFileHash(String fileHash);

    /**
     * 按上传时间倒序查找所有知识库
     */
    List<CourseMaterialEntity> findAllByOrderByUploadedAtDesc();

    /**
     * 获取所有不同的分类
     */
    @Query("SELECT DISTINCT k.category FROM CourseMaterialEntity k WHERE k.category IS NOT NULL ORDER BY k.category")
    List<String> findAllCategories();

    /**
     * 根据分类查找知识库
     */
    List<CourseMaterialEntity> findByCategoryOrderByUploadedAtDesc(String category);

    /**
     * 查找未分类的知识库
     */
    List<CourseMaterialEntity> findByCategoryIsNullOrderByUploadedAtDesc();

    /**
     * 按名称或文件名模糊搜索（不区分大小写）
     */
    @Query("SELECT k FROM CourseMaterialEntity k WHERE LOWER(k.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(k.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY k.uploadedAt DESC")
    List<CourseMaterialEntity> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 按文件大小排序
     */
    List<CourseMaterialEntity> findAllByOrderByFileSizeDesc();

    /**
     * 按访问次数排序
     */
    List<CourseMaterialEntity> findAllByOrderByAccessCountDesc();

    /**
     * 按提问次数排序
     */
    List<CourseMaterialEntity> findAllByOrderByQuestionCountDesc();

    // ==================== 批量更新 ====================

    /**
     * 批量增加知识库提问计数
     * @param ids 知识库ID列表
     * @return 更新的行数
     */
    @Modifying
    @Query("UPDATE CourseMaterialEntity k SET k.questionCount = k.questionCount + 1 WHERE k.id IN :ids")
    int incrementQuestionCountBatch(@Param("ids") List<Long> ids);

    // ==================== 统计查询 ====================

    /**
     * 统计总提问次数
     */
    @Query("SELECT COALESCE(SUM(k.questionCount), 0) FROM CourseMaterialEntity k")
    long sumQuestionCount();

    /**
     * 统计总访问次数
     */
    @Query("SELECT COALESCE(SUM(k.accessCount), 0) FROM CourseMaterialEntity k")
    long sumAccessCount();

    /**
     * 按向量化状态统计数量
     */
    long countByVectorStatus(MaterialVectorStatus vectorStatus);

    /**
     * 按向量化状态查找知识库（按上传时间倒序）
     */
    List<CourseMaterialEntity> findByVectorStatusOrderByUploadedAtDesc(MaterialVectorStatus vectorStatus);

    /**
     * 根据课程ID查找知识库
     */
    List<CourseMaterialEntity> findByCourseIdOrderByUploadedAtDesc(Long courseId);

    /**
     * 根据课程ID和向量化状态查找
     */
    List<CourseMaterialEntity> findByCourseIdAndVectorStatusOrderByUploadedAtDesc(Long courseId, MaterialVectorStatus vectorStatus);
}

