import { useEffect, useState, useCallback, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { historyApi, TutoringItem, EvaluateStatus } from '../api/history';
import { formatDate } from '../utils/date';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import {
  Users,
  Search,
  Download,
  Trash2,
  ChevronRight,
  CheckCircle,
  Clock,
  PlayCircle,
  TrendingUp,
  FileText,
  Loader2,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';

interface TutoringHistoryPageProps {
  onBack: () => void;
  onViewTutoring: (sessionId: string, studentProfileId?: number) => void;
}

interface TutoringWithStudentProfile extends TutoringItem {
  studentProfileId: number;
  studentProfileFilename: string;
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string | null;
}

interface TutoringStats {
  totalCount: number;
  completedCount: number;
  averageScore: number;
}

// 统计卡片组件
function StatCard({
  icon: Icon,
  label,
  value,
  suffix,
  color,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: number | string;
  suffix?: string;
  color: string;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-xl p-6 shadow-sm border border-slate-100"
    >
      <div className="flex items-center gap-4">
        <div className={`p-3 rounded-lg ${color}`}>
          <Icon className="w-6 h-6 text-white" />
        </div>
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="text-2xl font-bold text-slate-800">
            {value}{suffix && <span className="text-base font-normal text-slate-400 ml-1">{suffix}</span>}
          </p>
        </div>
      </div>
    </motion.div>
  );
}

// 判断是否为已完成状态（包括 COMPLETED 和 EVALUATED）
function isCompletedStatus(status: string): boolean {
  return status === 'COMPLETED' || status === 'EVALUATED';
}

// 判断评估是否完成
function isEvaluateCompleted(tutoring: TutoringWithStudentProfile): boolean {
  // 如果 evaluateStatus 存在且为 COMPLETED，则评估已完成
  if (tutoring.evaluateStatus === 'COMPLETED') return true;
  // 向后兼容：如果 status 为 EVALUATED，也认为评估已完成
  if (tutoring.status === 'EVALUATED') return true;
  return false;
}

// 判断是否正在评估中
function isEvaluating(tutoring: TutoringWithStudentProfile): boolean {
  return tutoring.evaluateStatus === 'PENDING' || tutoring.evaluateStatus === 'PROCESSING';
}

// 判断评估是否失败
function isEvaluateFailed(tutoring: TutoringWithStudentProfile): boolean {
  return tutoring.evaluateStatus === 'FAILED';
}

// 状态图标
function StatusIcon({ tutoring }: { tutoring: TutoringWithStudentProfile }) {
  // 评估失败
  if (isEvaluateFailed(tutoring)) {
    return <AlertCircle className="w-4 h-4 text-red-500" />;
  }
  // 正在评估
  if (isEvaluating(tutoring)) {
    return <RefreshCw className="w-4 h-4 text-blue-500 animate-spin" />;
  }
  // 评估完成
  if (isEvaluateCompleted(tutoring)) {
    return <CheckCircle className="w-4 h-4 text-green-500" />;
  }
  // 测验进行中
  if (tutoring.status === 'IN_PROGRESS') {
    return <PlayCircle className="w-4 h-4 text-blue-500" />;
  }
  // 测验已完成但评估未开始
  if (isCompletedStatus(tutoring.status)) {
    return <Clock className="w-4 h-4 text-yellow-500" />;
  }
  // 已创建
  return <Clock className="w-4 h-4 text-yellow-500" />;
}

// 状态文本
function getStatusText(tutoring: TutoringWithStudentProfile): string {
  // 评估失败
  if (isEvaluateFailed(tutoring)) {
    return '评估失败';
  }
  // 正在评估
  if (isEvaluating(tutoring)) {
    return tutoring.evaluateStatus === 'PROCESSING' ? '评估中' : '等待评估';
  }
  // 评估完成
  if (isEvaluateCompleted(tutoring)) {
    return '已完成';
  }
  // 测验进行中
  if (tutoring.status === 'IN_PROGRESS') {
    return '进行中';
  }
  // 测验已完成但评估未开始
  if (isCompletedStatus(tutoring.status)) {
    return '已提交';
  }
  return '已创建';
}

// 获取分数颜色
function getScoreColor(score: number): string {
  if (score >= 80) return 'bg-green-500';
  if (score >= 60) return 'bg-yellow-500';
  return 'bg-red-500';
}

export default function TutoringHistoryPage({ onBack: _onBack, onViewTutoring }: TutoringHistoryPageProps) {
  const [tutorings, setTutorings] = useState<TutoringWithStudentProfile[]>([]);
  const [stats, setStats] = useState<TutoringStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingSessionId, setDeletingSessionId] = useState<string | null>(null);
  const [deleteItem, setDeleteItem] = useState<TutoringWithStudentProfile | null>(null);
  const [exporting, setExporting] = useState<string | null>(null);
  const pollingRef = useRef<number | null>(null);

  const loadAllTutorings = useCallback(async (isPolling = false) => {
    if (!isPolling) {
      setLoading(true);
    }
    try {
      const studentProfiles = await historyApi.getStudentProfiles();
      const allTutorings: TutoringWithStudentProfile[] = [];

      for (const studentProfile of studentProfiles) {
        const detail = await historyApi.getStudentProfileDetail(studentProfile.id);
        if (detail.tutorings && detail.tutorings.length > 0) {
          detail.tutorings.forEach(tutoring => {
            allTutorings.push({
              ...tutoring,
              studentProfileId: studentProfile.id,
              studentProfileFilename: studentProfile.filename
            });
          });
        }
      }

      // 按创建时间倒序排序
      allTutorings.sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );

      setTutorings(allTutorings);

      // 计算统计信息（只统计评估已完成的测验）
      const evaluated = allTutorings.filter(i => isEvaluateCompleted(i));
      const totalScore = evaluated.reduce((sum, i) => sum + (i.overallScore || 0), 0);
      setStats({
        totalCount: allTutorings.length,
        completedCount: evaluated.length,
        averageScore: evaluated.length > 0 ? Math.round(totalScore / evaluated.length) : 0,
      });
    } catch (err) {
      console.error('加载测验记录失败', err);
    } finally {
      if (!isPolling) {
        setLoading(false);
      }
    }
  }, []);

  // 初始加载
  useEffect(() => {
    loadAllTutorings();
  }, [loadAllTutorings]);

  // 轮询检查评估状态
  useEffect(() => {
    // 检查是否有正在评估的测验
    const hasEvaluating = tutorings.some(i => isEvaluating(i));

    if (hasEvaluating) {
      // 启动轮询
      pollingRef.current = window.setInterval(() => {
        loadAllTutorings(true);
      }, 3000); // 每3秒轮询一次
    } else {
      // 停止轮询
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    }

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [tutorings, loadAllTutorings]);

  const handleDeleteClick = (tutoring: TutoringWithStudentProfile, e: React.MouseEvent) => {
    e.stopPropagation();
    setDeleteItem(tutoring);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteItem) return;

    setDeletingSessionId(deleteItem.sessionId);
    try {
      await historyApi.deleteTutoring(deleteItem.sessionId);
      await loadAllTutorings();
      setDeleteItem(null);
    } catch (err) {
      alert(err instanceof Error ? err.message : '删除失败，请稍后重试');
    } finally {
      setDeletingSessionId(null);
    }
  };

  const handleExport = async (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExporting(sessionId);
    try {
      const blob = await historyApi.exportTutoringPdf(sessionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `测验报告_${sessionId.slice(-8)}.pdf`;
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

  const filteredTutorings = tutorings.filter(tutoring =>
    tutoring.studentProfileFilename.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <motion.div
      className="w-full"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      {/* 头部 */}
      <div className="flex justify-between items-start mb-8 flex-wrap gap-6">
        <div>
          <motion.h1
            className="text-2xl font-bold text-slate-800 flex items-center gap-3"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
          >
            <Users className="w-7 h-7 text-primary-500" />
            测验记录
          </motion.h1>
          <motion.p
            className="text-slate-500 mt-1"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.1 }}
          >
            查看和管理所有课后测验记录
          </motion.p>
        </div>

        <motion.div
          className="flex items-center gap-3 bg-white border border-slate-200 rounded-xl px-4 py-2.5 min-w-[280px] focus-within:border-primary-500 focus-within:ring-2 focus-within:ring-primary-100 transition-all"
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
        >
          <Search className="w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索课程资料名称..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="flex-1 outline-none text-slate-700 placeholder:text-slate-400"
          />
        </motion.div>
      </div>

      {/* 统计卡片 */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <StatCard
            icon={Users}
            label="测验总数"
            value={stats.totalCount}
            color="bg-primary-500"
          />
          <StatCard
            icon={CheckCircle}
            label="已完成"
            value={stats.completedCount}
            color="bg-emerald-500"
          />
          <StatCard
            icon={TrendingUp}
            label="平均分数"
            value={stats.averageScore}
            suffix="分"
            color="bg-indigo-500"
          />
        </div>
      )}

      {/* 加载状态 */}
      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
        </div>
      )}

      {/* 空状态 */}
      {!loading && filteredTutorings.length === 0 && (
        <motion.div
          className="text-center py-20 bg-white rounded-2xl shadow-sm border border-slate-100"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
        >
          <Users className="w-16 h-16 text-slate-300 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-slate-700 mb-2">暂无测验记录</h3>
          <p className="text-slate-500">开始一次课后测验后，记录将显示在这里</p>
        </motion.div>
      )}

      {/* 表格 */}
      {!loading && filteredTutorings.length > 0 && (
        <motion.div
          className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600">关联课程资料</th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600">题目数</th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600">状态</th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600">得分</th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600">创建时间</th>
                <th className="text-right px-6 py-4 text-sm font-medium text-slate-600">操作</th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {filteredTutorings.map((tutoring, index) => (
                  <motion.tr
                    key={tutoring.sessionId}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    onClick={() => onViewTutoring(tutoring.sessionId, tutoring.studentProfileId)}
                    className="border-b border-slate-50 hover:bg-slate-50 cursor-pointer transition-colors group"
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <FileText className="w-5 h-5 text-slate-400" />
                        <div>
                          <p className="font-medium text-slate-800">{tutoring.studentProfileFilename}</p>
                          <p className="text-xs text-slate-400">#{tutoring.sessionId.slice(-8)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-slate-100 text-slate-600 rounded-lg text-sm">
                        {tutoring.totalQuestions} 题
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <StatusIcon tutoring={tutoring} />
                        <span className="text-sm text-slate-600">
                          {getStatusText(tutoring)}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {isEvaluateCompleted(tutoring) && tutoring.overallScore !== null ? (
                        <div className="flex items-center gap-3">
                          <div className="w-16 h-2 bg-slate-100 rounded-full overflow-hidden">
                            <motion.div
                              className={`h-full ${getScoreColor(tutoring.overallScore)} rounded-full`}
                              initial={{ width: 0 }}
                              animate={{ width: `${tutoring.overallScore}%` }}
                              transition={{ duration: 0.8, delay: index * 0.05 }}
                            />
                          </div>
                          <span className="font-bold text-slate-800">{tutoring.overallScore}</span>
                        </div>
                      ) : isEvaluating(tutoring) ? (
                        <span className="text-blue-500 text-sm">生成中...</span>
                      ) : isEvaluateFailed(tutoring) ? (
                        <span className="text-red-500 text-sm" title={tutoring.evaluateError ?? undefined}>失败</span>
                      ) : (
                        <span className="text-slate-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-500">
                      {formatDate(tutoring.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {/* 导出按钮 */}
                        {isEvaluateCompleted(tutoring) && (
                          <button
                            onClick={(e) => handleExport(tutoring.sessionId, e)}
                            disabled={exporting === tutoring.sessionId}
                            className="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 rounded-lg transition-colors disabled:opacity-50"
                            title="导出PDF"
                          >
                            {exporting === tutoring.sessionId ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <Download className="w-4 h-4" />
                            )}
                          </button>
                        )}
                        {/* 删除按钮 */}
                        <button
                          onClick={(e) => handleDeleteClick(tutoring, e)}
                          disabled={deletingSessionId === tutoring.sessionId}
                          className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                          title="删除"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                        <ChevronRight className="w-5 h-5 text-slate-300 group-hover:text-primary-500 group-hover:translate-x-1 transition-all" />
                      </div>
                    </td>
                  </motion.tr>
                ))}
              </AnimatePresence>
            </tbody>
          </table>
        </motion.div>
      )}

      {/* 删除确认对话框 */}
      <DeleteConfirmDialog
        open={deleteItem !== null}
        item={deleteItem ? { id: deleteItem.id, name: deleteItem.sessionId } : null}
        itemType="测验记录"
        loading={deletingSessionId !== null}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteItem(null)}
      />
    </motion.div>
  );
}
