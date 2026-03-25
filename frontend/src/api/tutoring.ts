import { request } from './request';
import type {
  CreateTutoringRequest,
  CurrentQuestionResponse,
  TutoringReport,
  TutoringSession,
  SubmitAnswerRequest,
  SubmitAnswerResponse
} from '../types/tutoring';

export const tutoringApi = {
  /**
   * 创建面试会话
   */
  async createSession(req: CreateTutoringRequest): Promise<TutoringSession> {
    return request.post<TutoringSession>('/api/tutoring/sessions', req, {
      timeout: 180000, // 3分钟超时，AI生成问题需要时间
    });
  },

  /**
   * 获取会话信息
   */
  async getSession(sessionId: string): Promise<TutoringSession> {
    return request.get<TutoringSession>(`/api/tutoring/sessions/${sessionId}`);
  },

  /**
   * 获取当前问题
   */
  async getCurrentQuestion(sessionId: string): Promise<CurrentQuestionResponse> {
    return request.get<CurrentQuestionResponse>(`/api/tutoring/sessions/${sessionId}/question`);
  },

  /**
   * 提交答案
   */
  async submitAnswer(req: SubmitAnswerRequest): Promise<SubmitAnswerResponse> {
    return request.post<SubmitAnswerResponse>(
      `/api/tutoring/sessions/${req.sessionId}/answers`,
      { questionIndex: req.questionIndex, answer: req.answer },
      {
        timeout: 180000, // 3分钟超时
      }
    );
  },

  /**
   * 获取面试报告
   */
  async getReport(sessionId: string): Promise<TutoringReport> {
    return request.get<TutoringReport>(`/api/tutoring/sessions/${sessionId}/report`, {
      timeout: 180000, // 3分钟超时，AI评估需要时间
    });
  },

  /**
   * 查找未完成的面试会话
   */
  async findUnfinishedSession(studentProfileId: number): Promise<TutoringSession | null> {
    try {
      return await request.get<TutoringSession>(`/api/tutoring/sessions/unfinished/${studentProfileId}`);
    } catch {
      // 如果没有未完成的会话，返回null
      return null;
    }
  },

  /**
   * 暂存答案（不进入下一题）
   */
  async saveAnswer(req: SubmitAnswerRequest): Promise<void> {
    return request.put<void>(
      `/api/tutoring/sessions/${req.sessionId}/answers`,
      { questionIndex: req.questionIndex, answer: req.answer }
    );
  },

  /**
   * 提前交卷
   */
  async completeTutoring(sessionId: string): Promise<void> {
    return request.post<void>(`/api/tutoring/sessions/${sessionId}/complete`);
  },
};
