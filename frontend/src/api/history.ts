import { request } from './request';

export type AnalyzeStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type EvaluateStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface StudentProfileListItem {
  id: number;
  filename: string;
  fileSize: number;
  uploadedAt: string;
  accessCount: number;
  latestScore?: number;
  lastAnalyzedAt?: string;
  tutoringCount: number;
  analyzeStatus?: AnalyzeStatus;
  analyzeError?: string;
  storageUrl?: string;
}

export interface StudentProfileStats {
  totalCount: number;
  totalTutoringCount: number;
  totalAccessCount: number;
}

export interface AnalysisItem {
  id: number;
  overallScore: number;
  contentScore: number;
  structureScore: number;
  skillMatchScore: number;
  expressionScore: number;
  projectScore: number;
  summary: string;
  analyzedAt: string;
  strengths: string[];
  suggestions: unknown[];
}

export interface TutoringItem {
  id: number;
  sessionId: string;
  totalQuestions: number;
  status: string;
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string;
  overallScore: number | null;
  overallFeedback: string | null;
  createdAt: string;
  completedAt: string | null;
  questions?: unknown[];
  strengths?: string[];
  improvements?: string[];
  referenceAnswers?: unknown[];
}

export interface AnswerItem {
  questionIndex: number;
  question: string;
  category: string;
  userAnswer: string;
  score: number;
  feedback: string;
  referenceAnswer?: string;
  keyPoints?: string[];
  answeredAt: string;
}

export interface StudentProfileDetail {
  id: number;
  filename: string;
  fileSize: number;
  contentType: string;
  storageUrl: string;
  uploadedAt: string;
  accessCount: number;
  studentProfileText: string;
  analyzeStatus?: AnalyzeStatus;
  analyzeError?: string;
  analyses: AnalysisItem[];
  tutorings: TutoringItem[];
}

export interface TutoringDetail extends TutoringItem {
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string;
  answers: AnswerItem[];
}

export const historyApi = {
  /**
   * 获取所有简历列表
   */
  async getStudentProfiles(): Promise<StudentProfileListItem[]> {
    return request.get<StudentProfileListItem[]>('/api/studentProfiles');
  },

  /**
   * 获取简历详情
   */
  async getStudentProfileDetail(id: number): Promise<StudentProfileDetail> {
    return request.get<StudentProfileDetail>(`/api/studentProfiles/${id}/detail`);
  },

  /**
   * 获取面试详情
   */
  async getTutoringDetail(sessionId: string): Promise<TutoringDetail> {
    return request.get<TutoringDetail>(`/api/tutoring/sessions/${sessionId}/details`);
  },

  /**
   * 导出简历分析报告PDF
   */
  async exportAnalysisPdf(studentProfileId: number): Promise<Blob> {
    const response = await request.getInstance().get(`/api/studentProfiles/${studentProfileId}/export`, {
      responseType: 'blob',
      skipResultTransform: true,
    } as never);
    return response.data;
  },

  /**
   * 导出面试报告PDF
   */
  async exportTutoringPdf(sessionId: string): Promise<Blob> {
    const response = await request.getInstance().get(`/api/tutoring/sessions/${sessionId}/export`, {
      responseType: 'blob',
      skipResultTransform: true,
    } as never);
    return response.data;
  },

  /**
   * 删除简历
   */
  async deleteStudentProfile(id: number): Promise<void> {
    return request.delete(`/api/studentProfiles/${id}`);
  },

  /**
   * 删除面试记录
   */
  async deleteTutoring(sessionId: string): Promise<void> {
    return request.delete(`/api/tutoring/sessions/${sessionId}`);
  },

  /**
   * 获取简历统计信息
   */
  async getStatistics(): Promise<StudentProfileStats> {
    return request.get<StudentProfileStats>('/api/studentProfiles/statistics');
  },

  /**
   * 重新分析简历
   */
  async reanalyze(id: number): Promise<void> {
    return request.post(`/api/studentProfiles/${id}/reanalyze`);
  },
};
