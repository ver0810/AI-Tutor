import { request } from './request';

export type AnalyzeStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type EvaluateStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface LearningStep {
  step: number;
  title: string;
  description: string;
}

export interface StudentProfileAnalysis {
  id: number;
  overallScore: number;
  difficulty: number;
  summary: string;
  tags: string[];
  learningPath: LearningStep[];
  analyzedAt: string;
}

export interface TutoringItem {
  id: number;
  sessionId: string;
  totalQuestions: number;
  status: string;
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string | null;
  overallScore: number | null;
  createdAt: string;
  completedAt: string | null;
}

export interface StudentProfileListItem {
  id: number;
  filename: string;
  fileSize: number;
  uploadedAt: string;
  accessCount: number;
  latestScore?: number;
  lastAnalyzedAt?: string | null;
  tutoringCount: number;
  analyzeStatus?: AnalyzeStatus;
  storageUrl?: string;
}

export interface StudentProfileStats {
  totalCount: number;
  totalTutoringCount: number;
  totalAccessCount: number;
}

export interface TutoringAnswerDetail {
  questionIndex: number;
  question: string;
  category: string;
  userAnswer: string | null;
  score: number;
  feedback: string | null;
  referenceAnswer: string | null;
  keyPoints: string[] | null;
  answeredAt: string | null;
}

export interface TutoringDetail {
  id: number;
  sessionId: string;
  totalQuestions: number;
  status: string;
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string | null;
  overallScore: number | null;
  overallFeedback: string | null;
  createdAt: string;
  completedAt: string | null;
  questions: Array<Record<string, unknown>>;
  strengths: string[];
  improvements: string[];
  referenceAnswers: Array<Record<string, unknown>>;
  answers: TutoringAnswerDetail[];
}

export interface StudentProfileDetail {
  id: number;
  filename: string;
  fileSize: number;
  contentType: string;
  storageUrl: string | null;
  uploadedAt: string;
  accessCount: number;
  studentProfileText: string;
  analyzeStatus?: AnalyzeStatus;
  analyzeError: string | null;
  analyses: StudentProfileAnalysis[];
  aitutors?: Array<Record<string, unknown>>;
  tutorings: TutoringItem[];
}

function mapTutoringItem(raw: Record<string, unknown>): TutoringItem {
  return {
    id: Number(raw.id),
    sessionId: String(raw.sessionId),
    totalQuestions: Number(raw.totalQuestions ?? 0),
    status: String(raw.status ?? ''),
    evaluateStatus: (raw.evaluateStatus as EvaluateStatus | undefined) ?? undefined,
    evaluateError: (raw.evaluateError as string | null | undefined) ?? null,
    overallScore: raw.overallScore === null || raw.overallScore === undefined ? null : Number(raw.overallScore),
    createdAt: String(raw.createdAt ?? ''),
    completedAt: (raw.completedAt as string | null | undefined) ?? null,
  };
}

function mapStudentProfileListItem(item: StudentProfileDetail): StudentProfileListItem {
  const latest = item.analyses?.[0];
  return {
    id: item.id,
    filename: item.filename,
    fileSize: item.fileSize,
    uploadedAt: item.uploadedAt,
    accessCount: item.accessCount,
    latestScore: latest?.overallScore,
    lastAnalyzedAt: latest?.analyzedAt ?? null,
    tutoringCount: item.tutorings?.length ?? 0,
    analyzeStatus: item.analyzeStatus,
    storageUrl: item.storageUrl ?? undefined,
  };
}

export const historyApi = {
  async getStudentProfiles(): Promise<StudentProfileListItem[]> {
    const details = await request.get<StudentProfileDetail[]>('/api/student/profiles');
    return details.map(mapStudentProfileListItem);
  },

  async getStatistics(): Promise<StudentProfileStats> {
    const profiles = await this.getStudentProfiles();
    return {
      totalCount: profiles.length,
      totalTutoringCount: profiles.reduce((sum, p) => sum + (p.tutoringCount ?? 0), 0),
      totalAccessCount: profiles.reduce((sum, p) => sum + (p.accessCount ?? 0), 0),
    };
  },

  async getStudentProfileDetail(id: number): Promise<StudentProfileDetail> {
    const detail = await request.get<StudentProfileDetail>(`/api/student/profiles/${id}`);
    return {
      ...detail,
      tutorings: (detail.aitutors as unknown as Record<string, unknown>[] | undefined)?.map(mapTutoringItem) ??
        detail.tutorings ??
        [],
    };
  },

  async deleteStudentProfile(id: number): Promise<void> {
    return request.delete(`/api/student/profiles/${id}`);
  },

  async reanalyze(id: number): Promise<void> {
    return request.post(`/api/student/profiles/${id}/reanalyze`);
  },

  async deleteTutoring(sessionId: string): Promise<void> {
    return request.delete(`/api/tutoring/sessions/${sessionId}`);
  },

  async getTutoringDetail(sessionId: string): Promise<TutoringDetail> {
    return request.get<TutoringDetail>(`/api/tutoring/sessions/${sessionId}/details`);
  },

  async exportAnalysisPdf(id: number): Promise<Blob> {
    return request.get<Blob>(`/api/student/profiles/${id}/export/analysis`, { responseType: 'blob' });
  },

  async exportTutoringPdf(sessionId: string): Promise<Blob> {
    return request.get<Blob>(`/api/tutoring/sessions/${sessionId}/export`, { responseType: 'blob' });
  },
};
