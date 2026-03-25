import { request } from './request';

// 向量化/分析状态
export type AnalyzeStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface LearningStep {
  step: number;
  title: string;
  description: string;
}

export interface StudentProfileAnalysis {
  id: number;
  summary: string;
  tags: string[];
  learningPath: LearningStep[];
  overallScore: number;
  difficulty: number;
  analyzedAt: string;
}

export interface TutoringItem {
  sessionId: string;
  studentProfileId: number;
  score: number;
  status: string;
  createdAt: string;
  completedAt: string | null;
}

export interface TutoringDetail extends TutoringItem {
  questions: Array<{
    id: string;
    question: string;
    options: string[];
    answer: string;
    userAnswer: string;
    isCorrect: boolean;
    explanation: string;
  }>;
  aiFeedback: string;
}

export interface StudentProfileDetail {
  id: number;
  filename: string;
  uploadedAt: string;
  studentProfileText: string;
  analyzeStatus: AnalyzeStatus;
  analyzeError: string | null;
  analyses: StudentProfileAnalysis[];
  tutorings: TutoringItem[];
}

export const historyApi = {
  /**
   * 获取简历/档案列表
   */
  async getStudentProfiles(): Promise<StudentProfileDetail[]> {
    return request.get<StudentProfileDetail[]>('/api/student/profiles');
  },

  /**
   * 获取简历/档案详情
   */
  async getStudentProfileDetail(id: number): Promise<StudentProfileDetail> {
    return request.get<StudentProfileDetail>(`/api/student/profiles/${id}`);
  },

  /**
   * 重新分析
   */
  async reanalyze(id: number): Promise<void> {
    return request.post(`/api/student/profiles/${id}/reanalyze`);
  },

  /**
   * 获取测验详情
   */
  async getTutoringDetail(sessionId: string): Promise<TutoringDetail> {
    return request.get<TutoringDetail>(`/api/tutoring/sessions/${sessionId}`);
  },

  /**
   * 导出分析 PDF
   */
  async exportAnalysisPdf(id: number): Promise<Blob> {
    return request.get<Blob>(`/api/student/profiles/${id}/export/analysis`, {
      responseType: 'blob',
    });
  },

  /**
   * 导出测验 PDF
   */
  async exportTutoringPdf(sessionId: string): Promise<Blob> {
    return request.get<Blob>(`/api/tutoring/sessions/${sessionId}/export`, {
      responseType: 'blob',
    });
  },
};
