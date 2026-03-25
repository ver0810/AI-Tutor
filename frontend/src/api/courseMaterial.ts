import { request, getErrorMessage } from './request';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

// 向量化状态
export type VectorStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface CourseMaterialItem {
  id: number;
  name: string;
  category: string | null;
  originalFilename: string;
  fileSize: number;
  contentType: string;
  uploadedAt: string;
  lastAccessedAt: string;
  accessCount: number;
  questionCount: number;
  vectorStatus: VectorStatus;
  vectorError: string | null;
  chunkCount: number;
}

// 统计信息
export interface CourseMaterialStats {
  totalCount: number;
  totalQuestionCount: number;
  totalAccessCount: number;
  completedCount: number;
  processingCount: number;
}

export type SortOption = 'time' | 'size' | 'access' | 'question';

export interface UploadCourseMaterialResponse {
  knowledgeBase: {
    id: number;
    name: string;
    category: string;
    fileSize: number;
    contentLength: number;
  };
  storage: {
    fileKey: string;
    fileUrl: string;
  };
  duplicate: boolean;
}

export interface QueryRequest {
  knowledgeBaseIds: number[];  // 支持多个课程资料
  question: string;
}

export interface QueryResponse {
  answer: string;
  knowledgeBaseId: number;
  knowledgeBaseName: string;
}

export interface LearningStep {
  step: number;
  title: string;
  description: string;
}

export interface CourseMaterialAnalysis {
  summary: string;
  tags: string[];
  learningPath: LearningStep[];
  difficulty: number;
}

export const courseMaterialApi = {
  /**
   * 上传课程资料文件
   */
  async uploadCourseMaterial(file: File, name?: string, category?: string): Promise<UploadCourseMaterialResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (name) {
      formData.append('name', name);
    }
    if (category) {
      formData.append('category', category);
    }
    return request.upload<UploadCourseMaterialResponse>('/api/course/materials/upload', formData);
  },

  /**
   * 获取课程资料分析报告
   */
  async analyze(id: number): Promise<CourseMaterialAnalysis> {
    return request.get<CourseMaterialAnalysis>(`/api/course/materials/${id}/analysis`);
  },

  /**
   * 获取所有课程资料列表
   */
  async getAllCourseMaterials(sortBy?: SortOption, vectorStatus?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'): Promise<CourseMaterialItem[]> {
    const params = new URLSearchParams();
    if (sortBy) {
      params.append('sortBy', sortBy);
    }
    if (vectorStatus) {
      params.append('vectorStatus', vectorStatus);
    }
    const queryString = params.toString();
    return request.get<CourseMaterialItem[]>(`/api/course/materials/list${queryString ? `?${queryString}` : ''}`);
  },

  /**
   * 获取课程资料详情
   */
  async getCourseMaterial(id: number): Promise<CourseMaterialItem> {
    return request.get<CourseMaterialItem>(`/api/course/materials/${id}`);
  },

  /**
   * 删除课程资料
   */
  async deleteCourseMaterial(id: number): Promise<void> {
    return request.delete(`/api/course/materials/${id}`);
  },

  // ========== 分类管理 ==========

  /**
   * 获取所有分类
   */
  async getAllCategories(): Promise<string[]> {
    return request.get<string[]>('/api/course/materials/categories');
  },

  /**
   * 根据分类获取课程资料
   */
  async getByCategory(category: string): Promise<CourseMaterialItem[]> {
    return request.get<CourseMaterialItem[]>(`/api/course/materials/category/${encodeURIComponent(category)}`);
  },

  /**
   * 获取未分类的课程资料
   */
  async getUncategorized(): Promise<CourseMaterialItem[]> {
    return request.get<CourseMaterialItem[]>('/api/course/materials/uncategorized');
  },

  /**
   * 更新课程资料分类
   */
  async updateCategory(id: number, category: string | null): Promise<void> {
    return request.put(`/api/course/materials/${id}/category`, { category });
  },

  // ========== 搜索 ==========

  /**
   * 搜索课程资料
   */
  async search(keyword: string): Promise<CourseMaterialItem[]> {
    return request.get<CourseMaterialItem[]>(`/api/course/materials/search?keyword=${encodeURIComponent(keyword)}`);
  },

  // ========== 统计 ==========

  /**
   * 获取课程资料统计信息
   */
  async getStatistics(): Promise<CourseMaterialStats> {
    return request.get<CourseMaterialStats>('/api/course/materials/stats');
  },

  // ========== 向量化管理 ==========

  /**
   * 重新向量化课程资料（手动重试）
   */
  async revectorize(id: number): Promise<void> {
    return request.post(`/api/course/materials/${id}/revectorize`);
  },

  /**
   * 基于课程资料回答问题
   */
  async queryCourseMaterial(req: QueryRequest): Promise<QueryResponse> {
    return request.post<QueryResponse>('/api/course/materials/query', req, {
      timeout: 180000, // 3分钟超时
    });
  },

  /**
   * 基于课程资料回答问题（流式SSE）
   * 注意：SSE 使用 fetch API，不走统一的 axios 封装
   */
  async queryCourseMaterialStream(
    req: QueryRequest,
    onMessage: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/course/materials/query/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(req),
      });

      if (!response.ok) {
        // 尝试解析错误响应
        try {
          const errorData = await response.json();
          if (errorData && errorData.message) {
            throw new Error(errorData.message);
          }
        } catch {
          // 忽略解析错误
        }
        throw new Error(`请求失败 (${response.status})`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('无法获取响应流');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      // 辅助函数：处理 data: 行并提取内容
      const extractContent = (line: string): string | null => {
        if (!line.startsWith('data:')) {
          return null;
        }
        let content = line.substring(5); // 移除 "data:" 前缀
        // SSE 标准：如果 data: 后第一个字符是空格，这是协议层面的空格，应该移除
        // 但这是可选的，有些实现可能没有这个空格
        if (content.startsWith(' ')) {
          content = content.substring(1);
        }
        // 如果内容为空（data: 或 data: ），可能表示换行，返回换行符
        if (content.length === 0) {
          return '\n';
        }
        return content;
      };

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          // 处理剩余的 buffer
          if (buffer) {
            const content = extractContent(buffer);
            if (content) {
              onMessage(content);
            }
          }
          onComplete();
          break;
        }

        // 解码数据块并添加到 buffer
        buffer += decoder.decode(value, { stream: true });

        // 按行分割处理 SSE 格式
        // SSE 格式：data: content\n 或 data:content\n，空行 \n\n 表示事件结束
        const lines = buffer.split('\n');
        // 保留最后一行（可能不完整，等待更多数据）
        buffer = lines.pop() || '';

        // 处理完整的行
        for (const line of lines) {
          const content = extractContent(line);
          if (content !== null) {
            // 发送内容（保留所有格式，包括空格、换行等，因为 Markdown 需要）
            onMessage(content);
          }
          // 空行（line === ''）在 SSE 中表示事件结束，但我们不需要特殊处理
          // 因为每个 data: 行已经是一个完整的数据块
        }
      }
    } catch (error) {
      onError(new Error(getErrorMessage(error)));
    }
  },
};
