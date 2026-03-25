import { request } from './request';
import type { UploadResponse } from '../types/studentProfile';

export const studentProfileApi = {
  /**
   * 上传简历并获取分析结果
   */
  async uploadAndAnalyze(file: File): Promise<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return request.upload<UploadResponse>('/api/studentProfiles/upload', formData);
  },

  /**
   * 健康检查
   */
  async healthCheck(): Promise<{ status: string; service: string }> {
    return request.get('/api/studentProfiles/health');
  },
};
