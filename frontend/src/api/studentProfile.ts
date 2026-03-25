import { request } from './request';
import type { UploadResponse } from '../types/studentProfile';

export const studentProfileApi = {
  /**
   * 上传学生档案并分析 (资料分析业务)
   */
  async uploadAndAnalyze(file: File): Promise<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return request.upload<UploadResponse>('/api/student/profiles/upload', formData);
  },

  /**
   * 健康检查
   */
  async healthCheck(): Promise<{ status: string; service: string }> {
    return request.get('/api/student/profiles/health');
  },
};
