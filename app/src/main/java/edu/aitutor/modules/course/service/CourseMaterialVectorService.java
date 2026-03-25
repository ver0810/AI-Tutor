package edu.aitutor.modules.course.service;

import edu.aitutor.modules.course.repository.VectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库向量存储服务
 * 负责文档分块、向量化和检索
 * 
 * 重构: 支持课程数据隔离 (course_id)
 */
@Slf4j
@Service
public class CourseMaterialVectorService {
    
    /**
     * 阿里云 DashScope Embedding API 批量大小限制
     */
    private static final int MAX_BATCH_SIZE = 10;
    
    /**
     * 教材分块重叠 tokens 数 (针对长篇教材优化)
     */
    private static final int OVERLAP_TOKENS = 150;
    
    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;
    private final VectorRepository vectorRepository;
    
    public CourseMaterialVectorService(VectorStore vectorStore, VectorRepository vectorRepository) {
        this.vectorStore = vectorStore;
        this.vectorRepository = vectorRepository;
        // 使用 TokenTextSplitter，每个 chunk 约 500 tokens，重叠 150 tokens
        // 针对长篇教材优化，增加重叠率以保持上下文连贯性
        this.textSplitter = new TokenTextSplitter();
    }
    
    /**
     * 将知识库内容向量化并存储
     * @param knowledgeBaseId 知识库ID
     * @param content 知识库文本内容
     * @param courseId 课程ID (用于数据隔离，可为 null)
     */
    @Transactional
    public void vectorizeAndStore(Long knowledgeBaseId, String content, Long courseId) {
        log.info("开始向量化知识库: kbId={}, courseId={}, contentLength={}", knowledgeBaseId, courseId, content.length());
        try {
            // 1. 先删除该知识库的旧向量数据
            deleteByCourseMaterialId(knowledgeBaseId);
            
            // 2. 将文本分块
            List<Document> chunks = textSplitter.apply(
                List.of(new Document(content))
            );
            
            log.info("文本分块完成: {} 个chunks", chunks.size());
            
            // 3. 为每个 chunk 添加 metadata（知识库ID + 课程ID）
            // 统一使用 String 类型存储，确保查询一致性
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("kb_id", knowledgeBaseId.toString());
                if (courseId != null) {
                    chunk.getMetadata().put("course_id", courseId.toString());
                }
            });
            
            // 4. 分批向量化并存储（阿里云 DashScope API 限制 batch size <= 10）
            int totalChunks = chunks.size();
            int batchCount = (totalChunks + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;
            log.info("开始分批向量化: 总共 {} 个chunks，分 {} 批处理，每批最多 {} 个",
                    totalChunks, batchCount, MAX_BATCH_SIZE);
            for (int i = 0; i < batchCount; i++) {
                int start = i * MAX_BATCH_SIZE;
                int end = Math.min(start + MAX_BATCH_SIZE, totalChunks);
                List<Document> batch = chunks.subList(start, end);
                log.debug("处理第 {}/{} 批: chunks {}-{}", i + 1, batchCount, start + 1, end);
                vectorStore.add(batch);
            }
            log.info("知识库向量化完成: kbId={}, courseId={}, chunks={}, batches={}",
                    knowledgeBaseId, courseId, totalChunks, batchCount);
        } catch (Exception e) {
            log.error("向量化知识库失败: kbId={}, error={}", knowledgeBaseId, e.getMessage(), e);
            throw new RuntimeException("向量化知识库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 基于多个知识库进行相似度搜索
     * 
     * @param query 查询文本
     * @param knowledgeBaseIds 知识库ID列表（如果为空则搜索所有）
     * @param courseId 课程ID（用于数据隔离，可为 null）
     * @param topK 返回top K个结果
     * @return 相关文档列表
     */
    public List<Document> similaritySearch(String query, List<Long> knowledgeBaseIds, Long courseId, int topK) {
        log.info("向量相似度搜索: query={}, kbIds={}, courseId={}, topK={}", query, knowledgeBaseIds, courseId, topK);
        
        try {
            // 使用 Spring AI 的 SearchRequest 构建结构化查询，实现数据库级过滤
            org.springframework.ai.vectorstore.SearchRequest.Builder builder = org.springframework.ai.vectorstore.SearchRequest.builder()
                .query(query)
                .topK(topK * 2);

            // 构建过滤表达式 (Metadata Filtering)
            StringBuilder filter = new StringBuilder();
            if (courseId != null) {
                filter.append("course_id == '").append(courseId).append("'");
            }
            
            if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
                if (!filter.isEmpty()) filter.append(" && ");
                filter.append("kb_id in [");
                for (int i = 0; i < knowledgeBaseIds.size(); i++) {
                    filter.append("'").append(knowledgeBaseIds.get(i)).append("'");
                    if (i < knowledgeBaseIds.size() - 1) filter.append(",");
                }
                filter.append("]");
            }

            if (!filter.isEmpty()) {
                builder.filterExpression(filter.toString());
                log.debug("应用向量检索过滤器: {}", filter);
            }

            // 执行检索
            List<Document> results = vectorStore.similaritySearch(builder.build());
            
            // 确保返回数量符合 topK
            if (results.size() > topK) {
                results = results.subList(0, topK);
            }
            
            log.info("搜索完成: 找到 {} 个相关文档", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage(), e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除指定知识库的所有向量数据
     * 委托给 VectorRepository 处理
     * 
     * @param knowledgeBaseId 知识库ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByCourseMaterialId(Long knowledgeBaseId) {
        try {
            vectorRepository.deleteByCourseMaterialId(knowledgeBaseId);
        } catch (Exception e) {
            log.error("删除向量数据失败: kbId={}, error={}", knowledgeBaseId, e.getMessage(), e);
            // 不抛出异常，允许继续执行其他删除操作
            // 如果确实需要严格保证，可以取消下面的注释
            // throw new RuntimeException("删除向量数据失败: " + e.getMessage(), e);
        }
    }
}

