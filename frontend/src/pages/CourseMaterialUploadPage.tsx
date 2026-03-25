import { useState } from 'react';
import { courseMaterialApi } from '../api/courseMaterial';
import type { UploadCourseMaterialResponse } from '../api/courseMaterial';
import FileUploadCard from '../components/FileUploadCard';

interface CourseMaterialUploadPageProps {
  onUploadComplete: (result: UploadCourseMaterialResponse) => void;
  onBack: () => void;
}

export default function CourseMaterialUploadPage({ onUploadComplete, onBack }: CourseMaterialUploadPageProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleUpload = async (file: File, name?: string) => {
    setUploading(true);
    setError('');

    try {
      const data = await courseMaterialApi.uploadCourseMaterial(file, name);
      onUploadComplete(data);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : '上传失败，请重试';
      setError(errorMessage);
      setUploading(false);
    }
  };

  return (
    <FileUploadCard
      title="上传课程资料"
      subtitle="上传文档，AI 将基于课程资料内容回答您的问题"
      accept=".pdf,.doc,.docx,.txt,.md"
      formatHint="支持 PDF、DOCX、DOC、TXT、MD"
      maxSizeHint="最大 50MB"
      uploading={uploading}
      uploadButtonText="开始上传"
      selectButtonText="选择文件"
      showNameInput={true}
      nameLabel="课程资料名称（可选）"
      namePlaceholder="留空则使用文件名"
      error={error}
      onUpload={handleUpload}
      onBack={onBack}
    />
  );
}
