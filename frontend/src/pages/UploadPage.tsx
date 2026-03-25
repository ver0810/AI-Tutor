import { useState } from 'react';
import { studentProfileApi } from '../api/studentProfile';
import { getErrorMessage } from '../api/request';
import FileUploadCard from '../components/FileUploadCard';

interface UploadPageProps {
  onUploadComplete: (studentProfileId: number) => void;
}

export default function UploadPage({ onUploadComplete }: UploadPageProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleUpload = async (file: File) => {
    setUploading(true);
    setError('');

    try {
      const data = await studentProfileApi.uploadAndAnalyze(file);

      // 异步模式：只检查上传是否成功（storage 信息）
      if (!data.storage || !data.storage.studentProfileId) {
        throw new Error('上传失败，请重试');
      }

      // 上传成功，跳转到课程资料库（分析在后台进行）
      onUploadComplete(data.storage.studentProfileId);
    } catch (err) {
      setError(getErrorMessage(err));
      setUploading(false);
    }
  };

  return (
    <FileUploadCard
      title="开始您的 AI 助教学习"
      subtitle="上传 PDF 或 Word 课程资料，AI 将为您定制专属学习方案"
      accept=".pdf,.doc,.docx,.txt"
      formatHint="支持 PDF, DOCX, TXT"
      maxSizeHint="最大 10MB"
      uploading={uploading}
      uploadButtonText="开始上传"
      selectButtonText="选择课程资料文件"
      error={error}
      onUpload={handleUpload}
    />
  );
}
