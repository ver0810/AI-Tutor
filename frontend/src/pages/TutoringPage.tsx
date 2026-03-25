import {useEffect, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {tutoringApi} from '../api/tutoring';
import ConfirmDialog from '../components/ConfirmDialog';
import TutoringConfigPanel from '../components/TutoringConfigPanel';
import TutoringChatPanel from '../components/TutoringChatPanel';
import type {TutoringQuestion, TutoringSession} from '../types/tutoring';

type TutoringStage = 'config' | 'tutoring';

interface Message {
  type: 'tutoringer' | 'user';
  content: string;
  category?: string;
  questionIndex?: number;
}

interface TutoringProps {
  studentProfileText: string;
  studentProfileId?: number;
  onBack: () => void;
  onTutoringComplete: () => void;
}

export default function Tutoring({ studentProfileText, studentProfileId, onBack, onTutoringComplete }: TutoringProps) {
  const [stage, setStage] = useState<TutoringStage>('config');
  const [questionCount, setQuestionCount] = useState(8);
  const [session, setSession] = useState<TutoringSession | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState<TutoringQuestion | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [answer, setAnswer] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [checkingUnfinished, setCheckingUnfinished] = useState(false);
  const [unfinishedSession, setUnfinishedSession] = useState<TutoringSession | null>(null);
  const [showCompleteConfirm, setShowCompleteConfirm] = useState(false);
  const [forceCreateNew, setForceCreateNew] = useState(false);
  
  // 检查是否有未完成的测验（组件挂载时和studentProfileId变化时）
  useEffect(() => {
    if (studentProfileId) {
      checkUnfinishedSession();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studentProfileId]);
  
  const checkUnfinishedSession = async () => {
    if (!studentProfileId) return;
    
    setCheckingUnfinished(true);
    try {
      const foundSession = await tutoringApi.findUnfinishedSession(studentProfileId);
      if (foundSession) {
        setUnfinishedSession(foundSession);
      }
    } catch (err) {
      console.error('检查未完成测验失败', err);
    } finally {
      setCheckingUnfinished(false);
    }
  };
  
  const handleContinueUnfinished = () => {
    if (!unfinishedSession) return;
    setForceCreateNew(false);  // 重置强制创建标志
    restoreSession(unfinishedSession);
    setUnfinishedSession(null);
  };
  
  const handleStartNew = () => {
    setUnfinishedSession(null);
    setForceCreateNew(true);  // 标记需要强制创建新会话
  };
  
  const restoreSession = (sessionToRestore: TutoringSession) => {
    setSession(sessionToRestore);
    
    // 恢复当前问题
    const currentQ = sessionToRestore.questions[sessionToRestore.currentQuestionIndex];
    if (currentQ) {
      setCurrentQuestion(currentQ);
      
      // 如果当前问题已有答案，显示在输入框中
      if (currentQ.userAnswer) {
        setAnswer(currentQ.userAnswer);
      }
      
      // 恢复消息历史
      const restoredMessages: Message[] = [];
      for (let i = 0; i <= sessionToRestore.currentQuestionIndex; i++) {
        const q = sessionToRestore.questions[i];
        restoredMessages.push({
          type: 'tutoringer',
          content: q.question,
          category: q.category,
          questionIndex: i
        });
        if (q.userAnswer) {
          restoredMessages.push({
            type: 'user',
            content: q.userAnswer
          });
        }
      }
      setMessages(restoredMessages);
    }
    
    setStage('tutoring');
  };
  
  const startTutoring = async () => {
    setIsCreating(true);
    setError('');
    
    try {
      // 创建新测验（如果 forceCreateNew 为 true，则强制创建新会话）
      const newSession = await tutoringApi.createSession({
        studentProfileText,
        questionCount,
        studentProfileId,
        forceCreate: forceCreateNew
      });
      
      // 重置强制创建标志
      setForceCreateNew(false);
      
      // 如果返回的是未完成的会话（currentQuestionIndex > 0 或已有答案），恢复它
      const hasProgress = newSession.currentQuestionIndex > 0 || 
                          newSession.questions.some(q => q.userAnswer) ||
                          newSession.status === 'IN_PROGRESS';
      
      if (hasProgress) {
        // 这是恢复的会话
        restoreSession(newSession);
      } else {
        // 全新的会话
        setSession(newSession);
        
        if (newSession.questions.length > 0) {
          const firstQuestion = newSession.questions[0];
          setCurrentQuestion(firstQuestion);
          setMessages([{
            type: 'tutoringer',
            content: firstQuestion.question,
            category: firstQuestion.category,
            questionIndex: 0
          }]);
        }
        
        setStage('tutoring');
      }
    } catch (err) {
      setError('创建测验失败，请重试');
      console.error(err);
      setForceCreateNew(false);  // 出错时也重置标志
    } finally {
      setIsCreating(false);
    }
  };
  
  const handleSubmitAnswer = async () => {
    if (!answer.trim() || !session || !currentQuestion) return;

    setIsSubmitting(true);

    const userMessage: Message = {
      type: 'user',
      content: answer
    };
    setMessages(prev => [...prev, userMessage]);

    try {
      const response = await tutoringApi.submitAnswer({
        sessionId: session.sessionId,
        questionIndex: currentQuestion.questionIndex,
        answer: answer.trim()
      });

      setAnswer('');

      if (response.hasNextQuestion && response.nextQuestion) {
        setCurrentQuestion(response.nextQuestion);
        setMessages(prev => [...prev, {
          type: 'tutoringer',
          content: response.nextQuestion!.question,
          category: response.nextQuestion!.category,
          questionIndex: response.nextQuestion!.questionIndex
        }]);
      } else {
        // 测验已完成，评估将在后台进行，跳转到测验记录页
        onTutoringComplete();
      }
    } catch (err) {
      setError('提交答案失败，请重试');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCompleteEarly = async () => {
    if (!session) return;

    setIsSubmitting(true);
    try {
      await tutoringApi.completeTutoring(session.sessionId);
      setShowCompleteConfirm(false);
      // 测验已完成，评估将在后台进行，跳转到测验记录页
      onTutoringComplete();
    } catch (err) {
      setError('提前交卷失败，请重试');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };
  
  // 配置界面
  const renderConfig = () => {
    return (
      <TutoringConfigPanel
        questionCount={questionCount}
        onQuestionCountChange={setQuestionCount}
        onStart={startTutoring}
        isCreating={isCreating}
        checkingUnfinished={checkingUnfinished}
        unfinishedSession={unfinishedSession}
        onContinueUnfinished={handleContinueUnfinished}
        onStartNew={handleStartNew}
        studentProfileText={studentProfileText}
        onBack={onBack}
        error={error}
      />
    );
  };
  
  // 测验问答界面
  const renderTutoring = () => {
    if (!session || !currentQuestion) return null;

    return (
      <TutoringChatPanel
        session={session}
        currentQuestion={currentQuestion}
        messages={messages}
        answer={answer}
        onAnswerChange={setAnswer}
        onSubmit={handleSubmitAnswer}
        onCompleteEarly={handleCompleteEarly}
        isSubmitting={isSubmitting}
        showCompleteConfirm={showCompleteConfirm}
        onShowCompleteConfirm={setShowCompleteConfirm}
      />
    );
  };

  const stageSubtitles = {
    config: '配置您的测验参数',
    tutoring: '认真回答每个问题，展示您的学习成果'
  };
  
  return (
    <div className="pb-10">
      {/* 页面头部 */}
      <motion.div 
        className="text-center mb-10"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-3xl font-bold text-slate-900 mb-2 flex items-center justify-center gap-3">
          <div className="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center">
            <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <line x1="12" y1="19" x2="12" y2="23" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <line x1="8" y1="23" x2="16" y2="23" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          课后测验
        </h1>
        <p className="text-slate-500">{stageSubtitles[stage]}</p>
      </motion.div>
      
      <AnimatePresence mode="wait" initial={false}>
        {stage === 'config' && (
          <motion.div
            key="config"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
          >
            {renderConfig()}
          </motion.div>
        )}
        {stage === 'tutoring' && (
          <motion.div
            key="tutoring"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            {renderTutoring()}
          </motion.div>
        )}
      </AnimatePresence>
      
      {/* 提前交卷确认对话框 */}
      <ConfirmDialog
        open={showCompleteConfirm}
        title="提前交卷"
        message="确定要提前交卷吗？未回答的问题将按0分计算。"
        confirmText="确定交卷"
        cancelText="取消"
        confirmVariant="warning"
        loading={isSubmitting}
        onConfirm={handleCompleteEarly}
        onCancel={() => setShowCompleteConfirm(false)}
      />
    </div>
  );
}
