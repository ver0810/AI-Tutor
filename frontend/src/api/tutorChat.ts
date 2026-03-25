import { request, getErrorMessage } from './request';
import { type CourseMaterialItem } from './courseMaterial';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

// ========== 类型定义 ==========

export interface TutorChatSession {
  id: number;
  title: string;
  courseMaterialIds: number[];
  createdAt: string;
}

export interface TutorChatSessionListItem {
  id: number;
  title: string;
  messageCount: number;
  courseMaterialNames: string[];
  updatedAt: string;
  isPinned: boolean;
}

export interface TutorChatMessage {
  id: number;
  type: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

export interface TutorChatSessionDetail {
  id: number;
  title: string;
  courseMaterials: CourseMaterialItem[];
  messages: TutorChatMessage[];
  createdAt: string;
  updatedAt: string;
}

// ========== API 函数 ==========

export const tutorChatApi = {
  /**
   * 创建新会话
   */
  async createSession(courseMaterialIds: number[], title?: string): Promise<TutorChatSession> {
    return request.post<TutorChatSession>('/api/tutor-chat/sessions', {
      courseMaterialIds,
      title,
    });
  },

  /**
   * 获取会话列表
   */
  async listSessions(): Promise<TutorChatSessionListItem[]> {
    return request.get<TutorChatSessionListItem[]>('/api/tutor-chat/sessions');
  },

  /**
   * 获取会话详情
   */
  async getSessionDetail(sessionId: number): Promise<TutorChatSessionDetail> {
    return request.get<TutorChatSessionDetail>(`/api/tutor-chat/sessions/${sessionId}`);
  },

  /**
   * 更新会话标题
   */
  async updateSessionTitle(sessionId: number, title: string): Promise<void> {
    return request.put(`/api/tutor-chat/sessions/${sessionId}/title`, { title });
  },

  /**
   * 更新会话使用的课程资料
   */
  async updateCourseMaterials(sessionId: number, courseMaterialIds: number[]): Promise<void> {
    return request.put(`/api/tutor-chat/sessions/${sessionId}/course-materials`, {
      courseMaterialIds,
    });
  },

  /**
   * 切换会话置顶状态
   */
  async togglePin(sessionId: number): Promise<void> {
    return request.put(`/api/tutor-chat/sessions/${sessionId}/pin`);
  },

  /**
   * 删除会话
   */
  async deleteSession(sessionId: number): Promise<void> {
    return request.delete(`/api/tutor-chat/sessions/${sessionId}`);
  },

  /**
   * 发送消息（流式SSE）
   */
  async sendMessageStream(
    sessionId: number,
    question: string,
    onMessage: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/tutor-chat/sessions/${sessionId}/messages/stream`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ question }),
        }
      );

      if (!response.ok) {
        try {
          const errorData = await response.json();
          if (errorData && errorData.message) {
            throw new Error(errorData.message);
          }
        } catch {
          // ignore
        }
        throw new Error(`请求失败 (${response.status})`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('无法获取响应流');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      const extractEventContent = (event: string): string | null => {
        if (!event.trim()) return null;

        const lines = event.split('\n');
        const contentParts: string[] = [];

        for (const line of lines) {
          if (line.startsWith('data:')) {
            contentParts.push(line.substring(5));
          }
        }

        if (contentParts.length === 0) return null;

        return contentParts.join('')
          .replace(/\\n/g, '\n')
          .replace(/\\r/g, '\r');
      };

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          if (buffer) {
            const content = extractEventContent(buffer);
            if (content) {
              onMessage(content);
            }
          }
          onComplete();
          break;
        }

        buffer += decoder.decode(value, { stream: true });

        let newlineIndex = buffer.indexOf('\n\n');
        if (newlineIndex === -1) {
          const singleLineIndex = buffer.indexOf('\n');
          if (singleLineIndex !== -1 && buffer.substring(0, singleLineIndex).startsWith('data:')) {
            const line = buffer.substring(0, singleLineIndex);
            const content = extractEventContent(line);
            if (content) {
              onMessage(content);
            }
            buffer = buffer.substring(singleLineIndex + 1);
          }
          continue;
        }

        const eventBlock = buffer.substring(0, newlineIndex);
        buffer = buffer.substring(newlineIndex + 2);

        const content = extractEventContent(eventBlock);
        if (content !== null) {
          onMessage(content);
        }
      }
    } catch (error) {
      onError(new Error(getErrorMessage(error)));
    }
  },
};
