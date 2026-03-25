import {useEffect, useState, useCallback} from 'react';
import {useLocation} from 'react-router-dom';
import {AnimatePresence, motion} from 'framer-motion';
import {historyApi, TutoringDetail, StudentProfileDetail} from '../api/history';
import AnalysisPanel from '../components/AnalysisPanel';
import TutoringPanel from '../components/TutoringPanel';
import TutoringDetailPanel from '../components/TutoringDetailPanel';
import {formatDateOnly} from '../utils/date';
import {
  ChevronLeft,
  Clock,
  Download,
  Mic,
  CheckSquare,
  MessageSquare
} from 'lucide-react';

interface StudentProfileDetailPageProps {
  studentProfileId: number;
  onBack: () => void;
  onStartTutoring: (studentProfileText: string, studentProfileId: number) => void;
}

type TabType = 'analysis' | 'tutoring';
type DetailViewType = 'list' | 'tutoringDetail';

export default function StudentProfileDetailPage({ studentProfileId, onBack, onStartTutoring }: StudentProfileDetailPageProps) {
  const location = useLocation();
  const [studentProfile, setStudentProfile] = useState<StudentProfileDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<TabType>('analysis');
  const [exporting, setExporting] = useState<string | null>(null);
  const [[page, direction], setPage] = useState([0, 0]);
  const [detailView, setDetailView] = useState<DetailViewType>('list');
  const [selectedTutoring, setSelectedTutoring] = useState<TutoringDetail | null>(null);
  const [loadingTutoring, setLoadingTutoring] = useState(false);
  const [reanalyzing, setReanalyzing] = useState(false);

  // 静默加载数据（用于轮询）
  const loadStudentProfileDetailSilent = useCallback(async () => {
    try {
      const data = await historyApi.getStudentProfileDetail(studentProfileId);
      setStudentProfile(data);
    } catch (err) {
      console.error('加载课程资料详情失败', err);
    }
  }, [studentProfileId]);

  const loadStudentProfileDetail = useCallback(async () => {
    setLoading(true);
    try {
      const data = await historyApi.getStudentProfileDetail(studentProfileId);
      setStudentProfile(data);
    } catch (err) {
      console.error('加载课程资料详情失败', err);
    } finally {
      setLoading(false);
    }
  }, [studentProfileId]);

  useEffect(() => {
    loadStudentProfileDetail();
  }, [loadStudentProfileDetail]);

  // 轮询：当分析状态为待处理时，每5秒刷新一次
  // 待处理判断：显式的 PENDING/PROCESSING 状态，或状态未定义且无分析结果
  useEffect(() => {
    const isProcessing = studentProfile && (
      studentProfile.analyzeStatus === 'PENDING' ||
      studentProfile.analyzeStatus === 'PROCESSING' ||
      (studentProfile.analyzeStatus === undefined && (!studentProfile.analyses || studentProfile.analyses.length === 0))
    );

    if (isProcessing && !loading) {
      const timer = setInterval(() => {
        loadStudentProfileDetailSilent();
      }, 5000);

      return () => clearInterval(timer);
    }
  }, [studentProfile, loading, loadStudentProfileDetailSilent]);

  // 重新分析
  const handleReanalyze = async () => {
    try {
      setReanalyzing(true);
      await historyApi.reanalyze(studentProfileId);
      await loadStudentProfileDetailSilent();
    } catch (err) {
      console.error('重新分析失败', err);
    } finally {
      setReanalyzing(false);
    }
  };

  // 检查是否需要自动打开测验详情
  useEffect(() => {
    const viewTutoring = (location.state as { viewTutoring?: string })?.viewTutoring;
    if (viewTutoring && studentProfile) {
      // 切换到测验标签页
      setActiveTab('tutoring');
      // 加载并显示测验详情
      const loadAndViewTutoring = async () => {
        setLoadingTutoring(true);
        try {
          const detail = await historyApi.getTutoringDetail(viewTutoring);
          setSelectedTutoring(detail);
          setDetailView('tutoringDetail');
        } catch (err) {
          console.error('加载测验详情失败', err);
        } finally {
          setLoadingTutoring(false);
        }
      };
      loadAndViewTutoring();
    }
  }, [location.state, studentProfile]);

  const handleExportAnalysisPdf = async () => {
    setExporting('analysis');
    try {
      const blob = await historyApi.exportAnalysisPdf(studentProfileId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `课程资料分析报告_${studentProfile?.filename || studentProfileId}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('导出失败，请重试');
    } finally {
      setExporting(null);
    }
  };

  const handleExportTutoringPdf = async (sessionId: string) => {
    setExporting(sessionId);
    try {
      const blob = await historyApi.exportTutoringPdf(sessionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `测验报告_${sessionId}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('导出失败，请重试');
    } finally {
      setExporting(null);
    }
  };

  const handleViewTutoring = async (sessionId: string) => {
    setLoadingTutoring(true);
    try {
      const detail = await historyApi.getTutoringDetail(sessionId);
      setSelectedTutoring(detail);
      setDetailView('tutoringDetail');
    } catch (err) {
      alert('加载测验详情失败');
    } finally {
      setLoadingTutoring(false);
    }
  };

  const handleBackToTutoringList = () => {
    setDetailView('list');
    setSelectedTutoring(null);
  };

  const handleDeleteTutoring = async (sessionId: string) => {
    // 删除后重新加载课程资料详情
    await loadStudentProfileDetail();
    // 如果删除的是当前查看的测验，返回列表
    if (selectedTutoring?.sessionId === sessionId) {
      setDetailView('list');
      setSelectedTutoring(null);
    }
  };

  const handleTabChange = (tab: TabType) => {
    const newPage = tab === 'analysis' ? 0 : 1;
    setPage([newPage, newPage > page ? 1 : -1]);
    setActiveTab(tab);
    setDetailView('list');
    setSelectedTutoring(null);
  };

  const slideVariants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 300 : -300,
      opacity: 0,
    }),
    center: {
      x: 0,
      opacity: 1,
    },
    exit: (direction: number) => ({
      x: direction < 0 ? 300 : -300,
      opacity: 0,
    }),
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <motion.div 
          className="w-12 h-12 border-4 border-slate-200 border-t-primary-500 rounded-full"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        />
      </div>
    );
  }

  if (!studentProfile) {
    return (
      <div className="text-center py-20">
        <p className="text-red-500 mb-4">加载失败，请返回重试</p>
        <button onClick={onBack} className="px-6 py-2 bg-primary-500 text-white rounded-lg">返回列表</button>
      </div>
    );
  }

  const latestAnalysis = studentProfile.analyses?.[0];
  const tabs = [
    { id: 'analysis' as const, label: '资料分析', icon: CheckSquare },
    { id: 'tutoring' as const, label: '测验记录', icon: MessageSquare, count: studentProfile.tutorings?.length || 0 },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full"
    >
      {/* 顶部导航栏 */}
      <div className="flex justify-between items-center mb-8 flex-wrap gap-4">
        <div className="flex items-center gap-4">
          <motion.button 
            onClick={detailView === 'tutoringDetail' ? handleBackToTutoringList : onBack}
            className="w-10 h-10 bg-white rounded-xl flex items-center justify-center text-slate-500 hover:bg-slate-50 hover:text-slate-700 transition-all shadow-sm"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            <ChevronLeft className="w-5 h-5" />
          </motion.button>
          <div>
            <h2 className="text-xl font-bold text-slate-900">
              {detailView === 'tutoringDetail' ? `测验详情 #${selectedTutoring?.sessionId?.slice(-6) || ''}` : studentProfile.filename}
            </h2>
            <p className="text-sm text-slate-500 flex items-center gap-1.5">
              <Clock className="w-4 h-4" />
              {detailView === 'tutoringDetail' 
                ? `完成于 ${formatDateOnly(selectedTutoring?.completedAt || selectedTutoring?.createdAt || '')}`
                : `上传于 ${formatDateOnly(studentProfile.uploadedAt)}`
              }
            </p>
          </div>
        </div>
        
        <div className="flex gap-3">
          {detailView === 'tutoringDetail' && selectedTutoring && (
            <motion.button
              onClick={() => handleExportTutoringPdf(selectedTutoring.sessionId)}
              disabled={exporting === selectedTutoring.sessionId}
              className="px-5 py-2.5 border border-slate-200 bg-white rounded-xl text-slate-600 font-medium hover:bg-slate-50 transition-all disabled:opacity-50 flex items-center gap-2"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <Download className="w-4 h-4" />
              {exporting === selectedTutoring.sessionId ? '导出中...' : '导出 PDF'}
            </motion.button>
          )}
          {detailView !== 'tutoringDetail' && (
            <motion.button
              onClick={() => onStartTutoring(studentProfile.studentProfileText, studentProfileId)}
              className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 hover:shadow-xl transition-all flex items-center gap-2"
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.98 }}
            >
              <Mic className="w-4 h-4" />
              开始课后测验
            </motion.button>
          )}
        </div>
      </div>

      {/* 标签页切换 - 仅在非测验详情时显示 */}
      {detailView !== 'tutoringDetail' && (
        <div className="bg-white rounded-2xl p-2 mb-6 inline-flex gap-1">
          {tabs.map((tab) => (
            <motion.button
              key={tab.id}
              onClick={() => handleTabChange(tab.id)}
              className={`relative px-6 py-3 rounded-xl font-medium flex items-center gap-2 transition-colors
                ${activeTab === tab.id ? 'text-primary-600' : 'text-slate-500 hover:text-slate-700'}`}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              {activeTab === tab.id && (
                <motion.div
                  layoutId="activeTab"
                  className="absolute inset-0 bg-primary-50 rounded-xl"
                  transition={{ type: "spring", bounce: 0.2, duration: 0.6 }}
                />
              )}
              <span className="relative z-10 flex items-center gap-2">
                <tab.icon className="w-5 h-5" />
                {tab.label}
                {tab.count !== undefined && tab.count > 0 && (
                  <span className="px-2 py-0.5 bg-primary-100 text-primary-600 text-xs rounded-full">{tab.count}</span>
                )}
              </span>
            </motion.button>
          ))}
        </div>
      )}

      {/* 内容区域 */}
      <div className="relative overflow-hidden">
        {detailView === 'tutoringDetail' && selectedTutoring ? (
          <TutoringDetailPanel tutoring={selectedTutoring} />
        ) : (
          <AnimatePresence initial={false} custom={direction} mode="wait">
            <motion.div
              key={activeTab}
              custom={direction}
              variants={slideVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
            >
              {activeTab === 'analysis' ? (
                <AnalysisPanel
                  analysis={latestAnalysis}
                  analyzeStatus={studentProfile.analyzeStatus}
                  analyzeError={studentProfile.analyzeError}
                  onExport={handleExportAnalysisPdf}
                  exporting={exporting === 'analysis'}
                  onReanalyze={handleReanalyze}
                  reanalyzing={reanalyzing}
                />
              ) : (
                <TutoringPanel 
                  tutorings={studentProfile.tutorings || []} 
                  onStartTutoring={() => onStartTutoring(studentProfile.studentProfileText, studentProfileId)}
                  onViewTutoring={handleViewTutoring}
                  onExportTutoring={handleExportTutoringPdf}
                  onDeleteTutoring={handleDeleteTutoring}
                  exporting={exporting}
                  loadingTutoring={loadingTutoring}
                />
              )}
            </motion.div>
          </AnimatePresence>
        )}
      </div>
    </motion.div>
  );
}
