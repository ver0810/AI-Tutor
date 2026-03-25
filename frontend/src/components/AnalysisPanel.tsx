import React from 'react';
import { motion } from 'framer-motion';
import { BookOpen, Tag, Map, Star, AlertCircle, RefreshCcw, Download } from 'lucide-react';

interface LearningStep {
  step: number;
  title: string;
  description: string;
}

interface AnalysisData {
  summary?: string;
  tags?: string[];
  learningPath?: LearningStep[];
  difficulty?: number;
  overallScore?: number;
}

interface AnalysisPanelProps {
  analysis?: AnalysisData | null;
  isLoading?: boolean;
  // 以下是兼容 StudentProfileDetailPage 的 Props
  analyzeStatus?: string;
  analyzeError?: string | null;
  onExport?: () => void;
  exporting?: boolean;
  onReanalyze?: () => void;
  reanalyzing?: boolean;
}

const AnalysisPanel: React.FC<AnalysisPanelProps> = ({ 
  analysis, 
  isLoading, 
  analyzeStatus, 
  analyzeError,
  onExport,
  exporting,
  onReanalyze,
  reanalyzing
}) => {
  // 处理分析中的状态
  if (isLoading || analyzeStatus === 'PENDING' || analyzeStatus === 'PROCESSING') {
    return (
      <div className="flex flex-col items-center justify-center p-20 space-y-6 text-slate-500 bg-white rounded-3xl border border-slate-100 shadow-sm">
        <div className="relative">
          <BookOpen className="w-16 h-16 text-blue-500/20" />
          <motion.div 
            className="absolute inset-0 border-4 border-t-blue-500 border-transparent rounded-full"
            animate={{ rotate: 360 }}
            transition={{ duration: 1.5, repeat: Infinity, ease: "linear" }}
          />
        </div>
        <div className="text-center">
          <p className="text-lg font-semibold text-slate-800">正在深度分析资料核心内容...</p>
          <p className="text-sm text-slate-400 mt-2">预计需要 15-30 秒，AI 正在提炼核心知识与学习路径</p>
        </div>
      </div>
    );
  }

  // 处理失败状态
  if (analyzeStatus === 'FAILED' || analyzeError) {
    return (
      <div className="bg-red-50 border border-red-100 rounded-3xl p-12 text-center space-y-4">
        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h3 className="text-lg font-semibold text-red-900">分析失败</h3>
        <p className="text-red-700 max-w-md mx-auto">{analyzeError || '由于 AI 服务响应异常，未能完成分析报告。'}</p>
        <button 
          onClick={onReanalyze}
          disabled={reanalyzing}
          className="px-6 py-2 bg-red-600 text-white rounded-xl hover:bg-red-700 transition-colors flex items-center gap-2 mx-auto disabled:opacity-50"
        >
          <RefreshCcw className={`w-4 h-4 ${reanalyzing ? 'animate-spin' : ''}`} />
          重新尝试分析
        </button>
      </div>
    );
  }

  // 如果没有数据
  if (!analysis) {
    return (
      <div className="p-20 text-center bg-slate-50 rounded-3xl border-2 border-dashed border-slate-200">
        <p className="text-slate-400">暂无分析报告，请点击右上角开始分析</p>
      </div>
    );
  }

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="space-y-8"
    >
      {/* 顶部操作栏 (可选) */}
      {(onExport || onReanalyze) && (
        <div className="flex justify-end gap-3">
          {onReanalyze && (
            <button 
              onClick={onReanalyze}
              disabled={reanalyzing}
              className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl transition-all disabled:opacity-50"
            >
              <RefreshCcw className={`w-4 h-4 ${reanalyzing ? 'animate-spin' : ''}`} />
              重新分析
            </button>
          )}
          {onExport && (
            <button 
              onClick={onExport}
              disabled={exporting}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-xl transition-all shadow-sm"
            >
              <Download className="w-4 h-4" />
              {exporting ? '导出中...' : '下载报告 PDF'}
            </button>
          )}
        </div>
      )}

      {/* 核心总结 */}
      <section className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100 group hover:shadow-md transition-all">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl group-hover:scale-110 transition-transform">
            <BookOpen className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-800">核心内容总结</h3>
        </div>
        <p className="text-slate-600 leading-relaxed text-base whitespace-pre-wrap">
          {analysis.summary || '暂无总结内容'}
        </p>
      </section>

      {/* 知识标签与难度 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <section className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2.5 bg-purple-50 text-purple-600 rounded-xl">
                <Tag className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-semibold text-slate-800">知识点标签</h3>
            </div>
            <div className="flex flex-wrap gap-3">
              {(analysis.tags && analysis.tags.length > 0) ? (
                analysis.tags.map((tag, idx) => (
                  <span 
                    key={idx}
                    className="px-4 py-2 bg-purple-50 text-purple-700 rounded-2xl text-sm font-semibold border border-purple-100/50 hover:bg-purple-100 transition-colors cursor-default"
                  >
                    #{tag}
                  </span>
                ))
              ) : (
                <p className="text-slate-400">暂无知识点标签</p>
              )}
            </div>
          </div>
        </section>

        <section className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-xl">
              <Star className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-semibold text-slate-800">学习难度评估</h3>
          </div>
          <div className="flex flex-col space-y-4">
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((s) => (
                <Star 
                  key={s}
                  className={`w-8 h-8 ${s <= (analysis.difficulty || 0) ? 'text-amber-400 fill-amber-400' : 'text-slate-200'}`}
                />
              ))}
            </div>
            <div className="p-4 bg-amber-50/50 rounded-2xl border border-amber-100">
              <p className="text-amber-800 font-semibold text-base">
                {analysis.difficulty ? (
                  analysis.difficulty >= 5 ? '🎓 专家级资料：需要深厚的背景知识' :
                  analysis.difficulty >= 4 ? '📖 进阶级资料：适合已有基础的学生' :
                  analysis.difficulty >= 3 ? '📐 中等难度：需要专注研读' :
                  '🌱 入门资料：适合初学者快速掌握'
                ) : '尚未评估难度'}
              </p>
            </div>
          </div>
        </section>
      </div>

      {/* 学习路径 */}
      <section className="bg-slate-900 rounded-[2.5rem] p-10 text-white shadow-2xl overflow-hidden relative border-4 border-slate-800">
        <div className="absolute top-0 right-0 p-12 opacity-5">
          <Map className="w-64 h-64" />
        </div>
        
        <div className="flex items-center gap-3 mb-10 relative z-10">
          <div className="p-3 bg-blue-500/20 text-blue-400 rounded-2xl ring-1 ring-blue-400/30">
            <Map className="w-8 h-8" />
          </div>
          <div>
            <h3 className="text-2xl font-semibold">推荐学习路径</h3>
            <p className="text-slate-400 mt-1">AI 已为你制定循序渐进的学习步骤</p>
          </div>
        </div>

        <div className="relative space-y-12 mb-10">
          {/* 连接线 */}
          <div className="absolute left-6 top-4 bottom-4 w-1 bg-gradient-to-b from-blue-500/50 via-blue-500/20 to-transparent" />

          {analysis.learningPath && analysis.learningPath.length > 0 ? (
            analysis.learningPath.map((step, idx) => (
              <motion.div 
                key={idx}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.1 }}
                className="relative pl-20 group"
              >
                {/* 步骤圆点 */}
                <div className="absolute left-0 top-0 w-10 h-10 bg-blue-600 rounded-2xl flex items-center justify-center text-base font-bold border-4 border-slate-900 z-10 shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
                  {step.step || (idx + 1)}
                </div>
                
                <div className="bg-white/5 p-6 rounded-3xl border border-white/10 hover:bg-white/10 transition-colors">
                  <h4 className="text-xl font-semibold text-blue-400 mb-2">
                    {step.title}
                  </h4>
                  <p className="text-slate-300 text-base leading-relaxed">
                    {step.description}
                  </p>
                </div>
              </motion.div>
            ))
          ) : (
            <div className="pl-20 py-10 text-slate-500 text-base font-medium">
              暂未生成学习路径建议。
            </div>
          )}
        </div>

        <div className="mt-8 p-6 bg-blue-500/10 rounded-3xl border border-blue-500/20 flex items-start gap-4">
          <AlertCircle className="w-6 h-6 text-blue-400 shrink-0 mt-1" />
          <p className="text-base text-blue-200/90 leading-relaxed font-medium">
            AI 助教提示：此学习路径是基于资料内容的逻辑深度生成的。完成每个阶段后，建议点击顶部的“开始测验”来检验你的掌握程度。
          </p>
        </div>
      </section>
    </motion.div>
  );
};

export default AnalysisPanel;
