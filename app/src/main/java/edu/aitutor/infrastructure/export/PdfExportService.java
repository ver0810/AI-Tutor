package edu.aitutor.infrastructure.export;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.modules.tutoring.model.TutoringAnswerEntity;
import edu.aitutor.modules.tutoring.model.TutoringSessionEntity;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SECTION_COLOR = new DeviceRgb(52, 73, 94);
    
    private final ObjectMapper objectMapper;
    
    private PdfFont createChineseFont() {
        try {
            var fontStream = getClass().getClassLoader().getResourceAsStream("fonts/ZhuqueFangsong-Regular.ttf");
            if (fontStream != null) {
                byte[] fontBytes = fontStream.readAllBytes();
                fontStream.close();
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, EmbeddingStrategy.FORCE_EMBEDDED);
            }
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "字体文件缺失");
        } catch (Exception e) {
            log.error("创建中文字体失败", e);
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "创建字体失败");
        }
    }
    
    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{So}\\p{Cs}]", "").trim();
    }
    
    /**
     * 导出课程资料分析报告
     */
    public byte[] exportStudentProfileAnalysis(StudentProfileEntity profile, StudentProfileAnalysisResponse analysis) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        
        PdfFont font = createChineseFont();
        document.setFont(font);
        
        // 标题
        document.add(new Paragraph("资料核心分析报告")
            .setFontSize(24).setBold().setTextAlignment(TextAlignment.CENTER).setFontColor(HEADER_COLOR));
        
        // 基本信息
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("基本信息"));
        document.add(new Paragraph("文件名称: " + profile.getOriginalFilename()));
        document.add(new Paragraph("上传时间: " + (profile.getUploadedAt() != null ? DATE_FORMAT.format(profile.getUploadedAt()) : "未知")));
        
        // 综合评估
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("综合评估"));
        if (analysis.overallScore() != null) {
            document.add(new Paragraph("综合评分: " + analysis.overallScore() + " / 100")
                .setFontSize(16).setBold().setFontColor(getScoreColor(analysis.overallScore())));
        }
        document.add(new Paragraph("资料难度: " + (analysis.difficulty() != null ? analysis.difficulty() + " / 5" : "未评估")));
        
        // 内容总结
        if (analysis.summary() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("核心内容总结"));
            document.add(new Paragraph(sanitizeText(analysis.summary())));
        }
        
        // 知识标签
        if (analysis.tags() != null && !analysis.tags().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("核心知识标签"));
            document.add(new Paragraph(String.join(" | ", analysis.tags())));
        }
        
        // 学习路径
        if (analysis.learningPath() != null && !analysis.learningPath().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("推荐学习路径"));
            for (StudentProfileAnalysisResponse.LearningStep step : analysis.learningPath()) {
                document.add(new Paragraph("第 " + step.step() + " 阶段: " + sanitizeText(step.title()))
                    .setBold().setFontColor(HEADER_COLOR));
                document.add(new Paragraph("建议: " + sanitizeText(step.description())));
                document.add(new Paragraph("\n"));
            }
        }
        
        document.close();
        return baos.toByteArray();
    }
    
    /**
     * 导出测验报告
     */
    public byte[] exportTutoringReport(TutoringSessionEntity session) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        
        PdfFont font = createChineseFont();
        document.setFont(font);
        
        document.add(new Paragraph("学情测评报告")
            .setFontSize(24).setBold().setTextAlignment(TextAlignment.CENTER).setFontColor(HEADER_COLOR));
        
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("测评信息"));
        document.add(new Paragraph("会话ID: " + session.getSessionId()));
        document.add(new Paragraph("状态: " + getStatusText(session.getStatus())));
        
        if (session.getOverallScore() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("测评得分"));
            document.add(new Paragraph(session.getOverallScore() + " / 100")
                .setFontSize(18).setBold().setFontColor(getScoreColor(session.getOverallScore())));
        }
        
        List<TutoringAnswerEntity> answers = session.getAnswers();
        if (answers != null && !answers.isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("题目详情"));
            for (TutoringAnswerEntity answer : answers) {
                document.add(new Paragraph("Q: " + sanitizeText(answer.getQuestion())).setBold());
                document.add(new Paragraph("A: " + sanitizeText(answer.getUserAnswer())));
                document.add(new Paragraph("解析: " + sanitizeText(answer.getFeedback())).setItalic());
                document.add(new Paragraph("\n"));
            }
        }
        
        document.close();
        return baos.toByteArray();
    }
    
    private Paragraph createSectionTitle(String title) {
        return new Paragraph(title).setFontSize(14).setBold().setFontColor(SECTION_COLOR).setMarginTop(10);
    }
    
    private DeviceRgb getScoreColor(int score) {
        if (score >= 80) return new DeviceRgb(39, 174, 96);
        if (score >= 60) return new DeviceRgb(241, 196, 15);
        return new DeviceRgb(231, 76, 60);
    }
    
    private String getStatusText(TutoringSessionEntity.SessionStatus status) {
        return switch (status) {
            case CREATED -> "已创建";
            case IN_PROGRESS -> "进行中";
            case COMPLETED -> "已完成";
            case EVALUATED -> "已评估";
        };
    }
}
