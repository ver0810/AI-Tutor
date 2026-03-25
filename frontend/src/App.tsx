import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import Layout from './components/Layout';
import { useEffect, useState, Suspense, lazy } from 'react';
import { historyApi } from './api';
import type { UploadCourseMaterialResponse } from './api/courseMaterial';

// Lazy load components
const UploadPage = lazy(() => import('./pages/UploadPage'));
const HistoryList = lazy(() => import('./pages/HistoryPage'));
const StudentProfileDetailPage = lazy(() => import('./pages/StudentProfileDetailPage'));
const Tutoring = lazy(() => import('./pages/TutoringPage'));
const TutoringHistoryPage = lazy(() => import('./pages/TutoringHistoryPage'));
const CourseMaterialQueryPage = lazy(() => import('./pages/CourseMaterialQueryPage'));
const CourseMaterialUploadPage = lazy(() => import('./pages/CourseMaterialUploadPage'));
const CourseMaterialManagePage = lazy(() => import('./pages/CourseMaterialManagePage'));

// Loading component
const Loading = () => (
  <div className="flex items-center justify-center min-h-[50vh]">
    <div className="w-10 h-10 border-3 border-slate-200 border-t-primary-500 rounded-full animate-spin" />
  </div>
);

// 上传页面包装器
function UploadPageWrapper() {
  const navigate = useNavigate();

  const handleUploadComplete = (studentProfileId: number) => {
    // 异步模式：上传成功后跳转到资料库，让用户在列表中查看分析状态
    navigate('/history', { state: { newStudentProfileId: studentProfileId } });
  };

  return <UploadPage onUploadComplete={handleUploadComplete} />;
}

// 历史记录列表包装器
function HistoryListWrapper() {
  const navigate = useNavigate();

  const handleSelectStudentProfile = (id: number) => {
    navigate(`/history/${id}`);
  };

  return <HistoryList onSelectStudentProfile={handleSelectStudentProfile} />;
}

// 课程资料详情包装器
function StudentProfileDetailWrapper() {
  const { studentProfileId } = useParams<{ studentProfileId: string }>();
  const navigate = useNavigate();

  if (!studentProfileId) {
    return <Navigate to="/history" replace />;
  }

  const handleBack = () => {
    navigate('/history');
  };

  const handleStartTutoring = (studentProfileText: string, studentProfileId: number) => {
    navigate(`/tutoring/${studentProfileId}`, { state: { studentProfileText } });
  };

  return (
    <StudentProfileDetailPage
      studentProfileId={parseInt(studentProfileId, 10)}
      onBack={handleBack}
      onStartTutoring={handleStartTutoring}
    />
  );
}

// 测验页面包装器
function TutoringWrapper() {
  const { studentProfileId } = useParams<{ studentProfileId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [studentProfileText, setStudentProfileText] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 优先从location state获取studentProfileText
    const stateText = (location.state as { studentProfileText?: string })?.studentProfileText;
    if (stateText) {
      setStudentProfileText(stateText);
      setLoading(false);
    } else if (studentProfileId) {
      // 如果没有，从API获取课程资料详情
      historyApi.getStudentProfileDetail(parseInt(studentProfileId, 10))
        .then(studentProfile => {
          setStudentProfileText(studentProfile.studentProfileText);
          setLoading(false);
        })
        .catch(err => {
          console.error('获取课程资料文本失败', err);
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [studentProfileId, location.state]);

  if (!studentProfileId) {
    return <Navigate to="/history" replace />;
  }

  const handleBack = () => {
    // 尝试返回详情页，如果失败则返回历史列表
    navigate(`/history/${studentProfileId}`, { replace: false });
  };

  const handleTutoringComplete = () => {
    // 测验完成后跳转到测验记录页
    navigate('/tutorings');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-10 h-10 border-3 border-slate-200 border-t-primary-500 rounded-full mx-auto mb-4 animate-spin" />
          <p className="text-slate-500">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <Tutoring
      studentProfileText={studentProfileText}
      studentProfileId={parseInt(studentProfileId, 10)}
      onBack={handleBack}
      onTutoringComplete={handleTutoringComplete}
    />
  );
}

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="/" element={<Layout />}>
            {/* 默认重定向到上传页面 */}
            <Route index element={<Navigate to="/upload" replace />} />

            {/* 上传页面 */}
            <Route path="upload" element={<UploadPageWrapper />} />

            {/* 历史记录列表（资料库） */}
            <Route path="history" element={<HistoryListWrapper />} />

            {/* 课程资料详情 */}
            <Route path="history/:studentProfileId" element={<StudentProfileDetailWrapper />} />

            {/* 测验记录列表 */}
            <Route path="tutorings" element={<TutoringHistoryWrapper />} />

            {/* 课后测验 */}
            <Route path="tutoring/:studentProfileId" element={<TutoringWrapper />} />

            {/* 课程资料管理 */}
            <Route path="courseMaterial" element={<CourseMaterialManagePageWrapper />} />

            {/* 课程资料上传 */}
            <Route path="courseMaterial/upload" element={<CourseMaterialUploadPageWrapper />} />

            {/* 问答助手（课程资料聊天） */}
            <Route path="courseMaterial/chat" element={<CourseMaterialQueryPageWrapper />} />
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

// 测验记录页面包装器
function TutoringHistoryWrapper() {
  const navigate = useNavigate();

  const handleBack = () => {
    navigate('/upload');
  };

  const handleViewTutoring = async (sessionId: string, studentProfileId?: number) => {
    if (studentProfileId) {
      // 如果有资料ID，跳转到资料详情页的测验详情
      navigate(`/history/${studentProfileId}`, {
        state: { viewTutoring: sessionId }
      });
    } else {
      // 否则尝试从测验详情中获取资料ID
      try {
        await historyApi.getTutoringDetail(sessionId);
        // 测验详情中没有资料ID，需要从其他地方获取
        // 暂时跳转到历史记录列表
        navigate('/history');
      } catch {
        navigate('/history');
      }
    }
  };

  return <TutoringHistoryPage onBack={handleBack} onViewTutoring={handleViewTutoring} />;
}

// 课程资料管理页面包装器
function CourseMaterialManagePageWrapper() {
  const navigate = useNavigate();

  const handleUpload = () => {
    navigate('/courseMaterial/upload');
  };

  const handleChat = () => {
    navigate('/courseMaterial/chat');
  };

  return <CourseMaterialManagePage onUpload={handleUpload} onChat={handleChat} />;
}

// 课程资料问答页面包装器
function CourseMaterialQueryPageWrapper() {
  const navigate = useNavigate();
  const location = useLocation();
  const isChatMode = location.pathname === '/courseMaterial/chat';

  const handleBack = () => {
    if (isChatMode) {
      navigate('/courseMaterial');
    } else {
      navigate('/upload');
    }
  };

  const handleUpload = () => {
    navigate('/courseMaterial/upload');
  };

  return <CourseMaterialQueryPage onBack={handleBack} onUpload={handleUpload} />;
}

// 课程资料上传页面包装器
function CourseMaterialUploadPageWrapper() {
  const navigate = useNavigate();

  const handleUploadComplete = (_result: UploadCourseMaterialResponse) => {
    // 上传完成后返回管理页面
    navigate('/courseMaterial');
  };

  const handleBack = () => {
    navigate('/courseMaterial');
  };

  return <CourseMaterialUploadPage onUploadComplete={handleUploadComplete} onBack={handleBack} />;
}

export default App;
