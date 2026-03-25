// 面试相关类型定义

export interface TutoringSession {
  sessionId: string;
  studentProfileText: string;
  totalQuestions: number;
  currentQuestionIndex: number;
  questions: TutoringQuestion[];
  status: 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'EVALUATED';
}

export interface TutoringQuestion {
  questionIndex: number;
  question: string;
  type: QuestionType;
  category: string;
  userAnswer: string | null;
  score: number | null;
  feedback: string | null;
}

export type QuestionType = 
  | 'BASIC_CONCEPT' 
  | 'THEORY_DERIVATION' 
  | 'PRACTICAL_APPLICATION' 
  | 'LOGICAL_REASONING' 
  | 'KNOWLEDGE_EXTENSION';

export interface CreateTutoringRequest {
  studentProfileText: string;
  questionCount: number;
  studentProfileId?: number;
  forceCreate?: boolean;  // 是否强制创建新会话（忽略未完成的会话）
}

export interface SubmitAnswerRequest {
  sessionId: string;
  questionIndex: number;
  answer: string;
}

export interface SubmitAnswerResponse {
  hasNextQuestion: boolean;
  nextQuestion: TutoringQuestion | null;
  currentIndex: number;
  totalQuestions: number;
}

export interface CurrentQuestionResponse {
  completed: boolean;
  question?: TutoringQuestion;
  message?: string;
}

export interface TutoringReport {
  sessionId: string;
  totalQuestions: number;
  overallScore: number;
  categoryScores: CategoryScore[];
  questionDetails: QuestionEvaluation[];
  overallFeedback: string;
  strengths: string[];
  improvements: string[];
  referenceAnswers: ReferenceAnswer[];
}

export interface CategoryScore {
  category: string;
  score: number;
  questionCount: number;
}

export interface QuestionEvaluation {
  questionIndex: number;
  question: string;
  category: string;
  userAnswer: string;
  score: number;
  feedback: string;
}

export interface ReferenceAnswer {
  questionIndex: number;
  question: string;
  referenceAnswer: string;
  keyPoints: string[];
}
