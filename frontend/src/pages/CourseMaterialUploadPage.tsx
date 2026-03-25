import { useState } from 'react';
import { courseMaterialApi } from '../api/courseMaterial';
import type { UploadCourseMaterialResponse, CourseMaterialAnalysis } from '../api/courseMaterial';
import FileUploadCard from '../components/FileUploadCard';
import AnalysisPanel from '../components/AnalysisPanel';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle2, ArrowRight, LayoutDashboard } from 'lucide-react';

interface CourseMaterialUploadPageProps {
  onUploadComplete: (result: UploadCourseMaterialResponse) => void;
  onBack: () => void;
}

export default function CourseMaterialUploadPage({ onUploadComplete, onBack }: CourseMaterialUploadPageProps) {
  const [step, setStep] = useState<'upload' | 'analyzing' | 'result'>('upload');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [analysis, setAnalysis] = useState<CourseMaterialAnalysis | null>(null);
  const [lastResult, setLastResult] = useState<UploadCourseMaterialResponse | null>(null);

  const handleUpload = async (file: File, name?: string) => {
    setUploading(true);
    setError('');

    try {
      // 1. 上传文件
      const data = await courseMaterialApi.uploadCourseMaterial(file, name);
      setLastResult(data);
      
      // 2. 进入分析阶段
      setStep('analyzing');
      setUploading(false);

      // 3. 调用 AI 分析接口
      const analysisResult = await courseMaterialApi.analyze(data.knowledgeBase.id);
      setAnalysis(analysisResult);
      setStep('result');
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : '操作失败，请重试';
      setError(errorMessage);
      setUploading(false);
      setStep('upload');
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <AnimatePresence mode="wait">
        {step === 'upload' && (
          <motion.div
            key="upload"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
          >
            <FileUploadCard
              title="上传课程资料"
              subtitle="上传讲义、课件或论文，AI 将为您智能提取核心知识"
              accept=".pdf,.doc,.docx,.txt,.md"
              formatHint="支持 PDF、DOCX、TXT、MD"
              maxSizeHint="最大 50MB"
              uploading={uploading}
              uploadButtonText="开始分析资料"
              selectButtonText="选择文件"
              showNameInput={true}
              nameLabel="课程资料名称"
              namePlaceholder="输入课程或章节名称"
              error={error}
              onUpload={handleUpload}
              onBack={onBack}
            />
          </motion.div>
        )}

        {step === 'analyzing' && (
          <motion.div
            key="analyzing"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="bg-white rounded-3xl p-12 shadow-xl border border-slate-100 flex flex-col items-center"
          >
            <AnalysisPanel analysis={{} as any} isLoading={true} />
          </motion.div>
        )}

        {step === 'result' && analysis && (
          <motion.div
            key="result"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="space-y-6"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-green-100 rounded-full text-green-600">
                  <CheckCircle2 className="w-6 h-6" />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-slate-800">分析完成</h2>
                  <p className="text-slate-500">资料已成功入库，AI 已为您生成核心报告</p>
                </div>
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => onBack()}
                  className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl transition-colors font-medium"
                >
                  <LayoutDashboard className="w-4 h-4" />
                  返回管理页
                </button>
                <button
                  onClick={() => lastResult && onUploadComplete(lastResult)}
                  className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-xl transition-colors font-bold shadow-lg shadow-blue-200"
                >
                  去提问
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>

            <AnalysisPanel analysis={analysis} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
