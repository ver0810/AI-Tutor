package edu.aitutor.modules.course.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.mapper.CourseMaterialMapper;
import edu.aitutor.infrastructure.mapper.TutorChatMapper;
import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.model.CourseMaterialListItemDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.CreateSessionRequest;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatMessageEntity;
import edu.aitutor.modules.course.model.TutorChatSessionEntity;
import edu.aitutor.modules.course.repository.CourseMaterialRepository;
import edu.aitutor.modules.course.repository.TutorChatMessageRepository;
import edu.aitutor.modules.course.repository.TutorChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.List;

/**
 * RAG 聊天会话服务
 * 提供RAG聊天会话的创建、获取、更新、删除等操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TutorChatSessionService {

    private final TutorChatSessionRepository sessionRepository;
    private final TutorChatMessageRepository messageRepository;
    private final CourseMaterialRepository knowledgeBaseRepository;
    private final CourseMaterialQueryService queryService;
    private final TutorChatMapper ragChatMapper;
    private final CourseMaterialMapper knowledgeBaseMapper;

    /**
     * 创建新会话
     */
    @Transactional
    public SessionDTO createSession(TutorChatDTO.CreateSessionRequest request) {
        // 验证知识库存在
        List<CourseMaterialEntity> courseMaterials = knowledgeBaseRepository
            .findAllById(request.courseMaterialIds());

        if (courseMaterials.size() != request.courseMaterialIds().size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部分知识库不存在");
        }

        // 从知识库获取 courseId（用于数据隔离）
        Long courseId = courseMaterials.isEmpty() ? null : courseMaterials.getFirst().getCourseId();

        // 创建会话
        TutorChatSessionEntity session = new TutorChatSessionEntity();
        session.setTitle(request.title() != null && !request.title().isBlank()
            ? request.title()
            : generateTitle(courseMaterials));
        session.setCourseMaterials(new HashSet<>(courseMaterials));
        session.setCourseId(courseId); // 设置课程ID用于数据隔离

        session = sessionRepository.save(session);

        log.info("创建 RAG 聊天会话: id={}, title={}, courseId={}", session.getId(), session.getTitle(), courseId);

        return ragChatMapper.toSessionDTO(session);
    }

    /**
     * 获取会话列表
     */
    public List<SessionListItemDTO> listSessions() {
        return sessionRepository.findAllOrderByPinnedAndUpdatedAtDesc()
            .stream()
            .map(ragChatMapper::toSessionListItemDTO)
            .toList();
    }

    /**
     * 获取会话详情（包含消息）
     * 分两次查询避免笛卡尔积问题
     */
    public SessionDetailDTO getSessionDetail(Long sessionId) {
        // 先加载会话和知识库
        TutorChatSessionEntity session = sessionRepository
            .findByIdWithCourseMaterials(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));

        // 再单独加载消息（避免笛卡尔积）
        List<TutorChatMessageEntity> messages = messageRepository
            .findBySessionIdOrderByMessageOrderAsc(sessionId);

        // 转换知识库列表
        List<CourseMaterialListItemDTO> kbDTOs = knowledgeBaseMapper.toListItemDTOList(
            new java.util.ArrayList<>(session.getCourseMaterials())
        );

        return ragChatMapper.toSessionDetailDTO(session, messages, kbDTOs);
    }

    /**
     * 准备流式消息（保存用户消息，创建 AI 消息占位）
     *
     * @return AI 消息的 ID
     */
    @Transactional
    public Long prepareStreamMessage(Long sessionId, String question) {
        TutorChatSessionEntity session = sessionRepository.findByIdWithCourseMaterials(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));

        // 获取当前消息数量作为起始顺序
        int nextOrder = session.getMessageCount();

        // 保存用户消息
        TutorChatMessageEntity userMessage = new TutorChatMessageEntity();
        userMessage.setSession(session);
        userMessage.setType(TutorChatMessageEntity.MessageType.USER);
        userMessage.setContent(question);
        userMessage.setMessageOrder(nextOrder);
        userMessage.setCompleted(true);
        messageRepository.save(userMessage);

        // 创建 AI 消息占位（未完成）
        TutorChatMessageEntity assistantMessage = new TutorChatMessageEntity();
        assistantMessage.setSession(session);
        assistantMessage.setType(TutorChatMessageEntity.MessageType.ASSISTANT);
        assistantMessage.setContent("");
        assistantMessage.setMessageOrder(nextOrder + 1);
        assistantMessage.setCompleted(false);
        assistantMessage = messageRepository.save(assistantMessage);

        // 更新会话消息数量
        session.setMessageCount(nextOrder + 2);
        sessionRepository.save(session);

        log.info("准备流式消息: sessionId={}, messageId={}", sessionId, assistantMessage.getId());

        return assistantMessage.getId();
    }

    /**
     * 流式响应完成后更新消息
     */
    @Transactional
    public void completeStreamMessage(Long messageId, String content) {
        TutorChatMessageEntity message = messageRepository.findById(messageId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "消息不存在"));

        message.setContent(content);
        message.setCompleted(true);
        messageRepository.save(message);

        log.info("完成流式消息: messageId={}, contentLength={}", messageId, content.length());
    }

    /**
     * 获取流式回答
     */
    public Flux<String> getStreamAnswer(Long sessionId, String question) {
        TutorChatSessionEntity session = sessionRepository.findByIdWithCourseMaterials(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));

        List<Long> kbIds = session.getCourseMaterialIds();

        return queryService.answerQuestionStream(kbIds, question);
    }

    /**
     * 更新会话标题
     */
    @Transactional
    public void updateSessionTitle(Long sessionId, String title) {
        TutorChatSessionEntity session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));

        session.setTitle(title);
        sessionRepository.save(session);

        log.info("更新会话标题: sessionId={}, title={}", sessionId, title);
    }

    /**
     * 切换会话置顶状态
     */
    @Transactional
    public void togglePin(Long sessionId) {
        TutorChatSessionEntity session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));

        // 处理 null 值（兼容旧数据）
        Boolean currentPinned = session.getIsPinned() != null ? session.getIsPinned() : false;
        session.setIsPinned(!currentPinned);
        sessionRepository.save(session);

        log.info("切换会话置顶状态: sessionId={}, isPinned={}", sessionId, session.getIsPinned());
    }

    /**
     * 更新会话的知识库关联
     */
    @Transactional
    public void updateSessionCourseMaterials(Long sessionId, List<Long> knowledgeBaseIds) {
        TutorChatSessionEntity session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));

        List<CourseMaterialEntity> courseMaterials = knowledgeBaseRepository
            .findAllById(knowledgeBaseIds);

        session.setCourseMaterials(new HashSet<>(courseMaterials));

        // 更新 courseId（从新的知识库中获取）
        Long courseId = courseMaterials.isEmpty() ? null : courseMaterials.getFirst().getCourseId();
        session.setCourseId(courseId);

        sessionRepository.save(session);

        log.info("更新会话知识库: sessionId={}, kbIds={}, courseId={}", sessionId, knowledgeBaseIds, courseId);
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        sessionRepository.deleteById(sessionId);

        log.info("删除会话: sessionId={}", sessionId);
    }

    // ========== 私有方法 ==========

    private String generateTitle(List<CourseMaterialEntity> courseMaterials) {
        if (courseMaterials.isEmpty()) {
            return "新对话";
        }
        if (courseMaterials.size() == 1) {
            return courseMaterials.getFirst().getName();
        }
        return courseMaterials.size() + " 个知识库对话";
    }
}
