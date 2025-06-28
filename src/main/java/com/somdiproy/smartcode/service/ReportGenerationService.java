package com.somdiproy.smartcode.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating PDF reports
 * 
 * @author Somdip Roy
 */
@Service
public class ReportGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);
    
    // Brand colors
    private static final BaseColor PRIMARY_COLOR = new BaseColor(59, 130, 246); // Blue
    private static final BaseColor SECONDARY_COLOR = new BaseColor(99, 102, 241); // Indigo
    private static final BaseColor SUCCESS_COLOR = new BaseColor(34, 197, 94); // Green
    private static final BaseColor WARNING_COLOR = new BaseColor(251, 191, 36); // Yellow
    private static final BaseColor ERROR_COLOR = new BaseColor(239, 68, 68); // Red
    
    // Fonts
    private Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.WHITE);
    private Font headingFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private Font subheadingFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
    private Font smallFont = new Font(Font.FontFamily.HELVETICA, 9);
    
    /**
     * Generate PDF report from analysis response
     */
    public byte[] generatePDFReport(AnalysisResponse analysisResponse) throws DocumentException {
        CodeReviewResult result = analysisResponse.getResult();
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            // Add page events for header/footer
            writer.setPageEvent(new HeaderFooterPageEvent());
            
            document.open();
            
            // Add header
            addHeader(document, analysisResponse);
            
            // Add executive summary
            addExecutiveSummary(document, result);
            
            // Add key metrics
            addKeyMetrics(document, result);
            
            // Add detailed findings
            addDetailedFindings(document, result);
            
            // Add security analysis
            addSecurityAnalysis(document, result);
            
            // Add performance analysis
            addPerformanceAnalysis(document, result);
            
            // Add code quality metrics
            addCodeQualityMetrics(document, result);
            
            // Add recommendations
            addRecommendations(document, result);
            
            document.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating PDF report", e);
            throw new DocumentException("Failed to generate PDF report", e);
        }
    }
    
    /**
     * Add header section
     */
    private void addHeader(Document document, AnalysisResponse response) throws DocumentException {
        // Create header table
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(20);
        
        // Header background
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setPadding(20);
        headerCell.setBorder(Rectangle.NO_BORDER);
        
        // Title
        Paragraph title = new Paragraph("Smart Code Review", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(title);
        
        // Subtitle
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.WHITE);
        Paragraph subtitle = new Paragraph("AI-Powered Code Analysis Report", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(subtitle);
        
        headerTable.addCell(headerCell);
        document.add(headerTable);
        
        // Metadata
        Paragraph metadata = new Paragraph();
        metadata.add(new Chunk("Generated: ", normalFont));
        metadata.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), normalFont));
        metadata.add(Chunk.NEWLINE);
        metadata.add(new Chunk("Analysis ID: ", normalFont));
        metadata.add(new Chunk(response.getAnalysisId(), normalFont));
        metadata.setSpacingAfter(20);
        document.add(metadata);
    }
    
    /**
     * Add executive summary
     */
    private void addExecutiveSummary(Document document, CodeReviewResult result) throws DocumentException {
        document.add(new Paragraph("Executive Summary", headingFont));
        
        // Overall score box
        PdfPTable scoreTable = new PdfPTable(2);
        scoreTable.setWidthPercentage(100);
        scoreTable.setWidths(new float[]{1, 3});
        scoreTable.setSpacingAfter(15);
        
        // Score cell
        PdfPCell scoreCell = new PdfPCell();
        scoreCell.setBackgroundColor(getScoreColor(result.getOverallScore()));
        scoreCell.setPadding(15);
        scoreCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        scoreCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Font scoreFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.WHITE);
        Paragraph scorePara = new Paragraph(String.format("%.1f/10", result.getOverallScore()), scoreFont);
        scorePara.setAlignment(Element.ALIGN_CENTER);
        scoreCell.addElement(scorePara);
        
        // Summary cell
        PdfPCell summaryCell = new PdfPCell();
        summaryCell.setPadding(10);
        summaryCell.addElement(new Paragraph(result.getSummary(), normalFont));
        
        scoreTable.addCell(scoreCell);
        scoreTable.addCell(summaryCell);
        document.add(scoreTable);
    }
    
    /**
     * Add key metrics section
     */
    private void addKeyMetrics(Document document, CodeReviewResult result) throws DocumentException {
        document.add(new Paragraph("Key Metrics", headingFont));
        
        PdfPTable metricsTable = new PdfPTable(3);
        metricsTable.setWidthPercentage(100);
        metricsTable.setSpacingBefore(10);
        metricsTable.setSpacingAfter(20);
        
        // Security metric
        addMetricCell(metricsTable, "Security Score", 
                     String.format("%.1f/10", result.getSecurity().getSecurityScore()),
                     ERROR_COLOR);
        
        // Performance metric
        addMetricCell(metricsTable, "Performance Score", 
                     String.format("%.1f/10", result.getPerformance().getPerformanceScore()),
                     PRIMARY_COLOR);
        
        // Quality metric
        addMetricCell(metricsTable, "Quality Score", 
                     String.format("%.1f/10", result.getQuality().getMaintainabilityScore()),
                     SUCCESS_COLOR);
        
        document.add(metricsTable);
    }
    
    /**
     * Add detailed findings section
     */
    private void addDetailedFindings(Document document, CodeReviewResult result) throws DocumentException {
        if (result.getIssues() == null || result.getIssues().isEmpty()) {
            return;
        }
        
        document.add(new Paragraph("Detailed Findings", headingFont));
        
        // Group issues by severity
        Map<String, List<Issue>> issuesBySeverity = result.getIssues().stream()
                .collect(Collectors.groupingBy(issue -> issue.getSeverity() != null ? issue.getSeverity() : "MEDIUM"));
        
        // Add issues by severity
        for (String severity : List.of("CRITICAL", "HIGH", "MEDIUM", "LOW")) {
            List<Issue> severityIssues = issuesBySeverity.get(severity);
            if (severityIssues != null && !severityIssues.isEmpty()) {
                addSeveritySection(document, severity, severityIssues);
            }
        }
    }
    
    /**
     * Add severity section
     */
    private void addSeveritySection(Document document, String severity, List<Issue> issues) throws DocumentException {
        Font severityFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, getSeverityColor(severity));
        Paragraph severityPara = new Paragraph(severity + " (" + issues.size() + ")", severityFont);
        severityPara.setSpacingBefore(10);
        severityPara.setSpacingAfter(5);
        document.add(severityPara);
        
        com.itextpdf.text.List issueList = new com.itextpdf.text.List(com.itextpdf.text.List.ORDERED);
        
        for (Issue issue : issues) {
            ListItem item = new ListItem();
            
            // Title
            item.add(new Chunk(issue.getTitle() != null ? issue.getTitle() : issue.getCategory(), 
                             new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
            item.add(Chunk.NEWLINE);
            
            // Description
            item.add(new Chunk(issue.getDescription(), smallFont));
            
            // File info
            if (issue.getFileName() != null) {
                item.add(Chunk.NEWLINE);
                item.add(new Chunk("File: " + issue.getFileName() + 
                                 (issue.getLineNumber() > 0 ? ", Line: " + issue.getLineNumber() : ""), 
                                 new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC)));
            }
            
            issueList.add(item);
        }
        
        document.add(issueList);
    }
    
    /**
     * Add security analysis section
     */
    private void addSecurityAnalysis(Document document, CodeReviewResult result) throws DocumentException {
        SecurityAnalysis security = result.getSecurity();
        if (security == null || security.getDetailedVulnerabilities() == null || 
            security.getDetailedVulnerabilities().isEmpty()) {
            return;
        }
        
        document.newPage();
        document.add(new Paragraph("Security Analysis", headingFont));
        
        // Security summary
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);
        summaryTable.setSpacingAfter(15);
        
        addSecurityCountCell(summaryTable, "Critical", security.getCriticalIssuesCount(), ERROR_COLOR);
        addSecurityCountCell(summaryTable, "High", security.getHighIssuesCount(), new BaseColor(249, 115, 22));
        addSecurityCountCell(summaryTable, "Medium", security.getMediumIssuesCount(), WARNING_COLOR);
        addSecurityCountCell(summaryTable, "Low", security.getLowIssuesCount(), SUCCESS_COLOR);
        
        document.add(summaryTable);
        
        // Detailed vulnerabilities
        for (SecurityAnalysis.SecurityVulnerability vuln : security.getDetailedVulnerabilities()) {
            PdfPTable vulnTable = new PdfPTable(1);
            vulnTable.setWidthPercentage(100);
            vulnTable.setSpacingBefore(10);
            
            PdfPCell vulnCell = new PdfPCell();
            vulnCell.setBorderColor(getSeverityColor(vuln.getSeverity()));
            vulnCell.setBorderWidth(2);
            vulnCell.setPadding(10);
            
            // Type
            Paragraph typePara = new Paragraph(vuln.getType(), subheadingFont);
            vulnCell.addElement(typePara);
            
            // Description
            Paragraph descPara = new Paragraph(vuln.getDescription(), normalFont);
            vulnCell.addElement(descPara);
            
            // Location
            if (vuln.getLocation() != null) {
                Paragraph locPara = new Paragraph("Location: " + vuln.getLocation(), smallFont);
                locPara.setIndentationLeft(10);
                vulnCell.addElement(locPara);
            }
            
            // Remediation
            if (vuln.getRemediation() != null) {
                Paragraph remPara = new Paragraph("Remediation: " + vuln.getRemediation(), 
                                                 new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC));
                remPara.setIndentationLeft(10);
                vulnCell.addElement(remPara);
            }
            
            vulnTable.addCell(vulnCell);
            document.add(vulnTable);
        }
    }
    
    /**
     * Add performance analysis section
     */
    private void addPerformanceAnalysis(Document document, CodeReviewResult result) throws DocumentException {
        PerformanceAnalysis performance = result.getPerformance();
        if (performance == null) {
            return;
        }
        
        document.add(new Paragraph("Performance Analysis", headingFont));
        
        // Add bottlenecks if available
        if (performance.getBottlenecks() != null && !performance.getBottlenecks().isEmpty()) {
            Paragraph bottlenecksPara = new Paragraph("Performance Bottlenecks", subheadingFont);
            bottlenecksPara.setSpacingBefore(10);
            bottlenecksPara.setSpacingAfter(5);
            document.add(bottlenecksPara);
            
            com.itextpdf.text.List bottleneckList = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
            for (String bottleneck : performance.getBottlenecks()) {
                bottleneckList.add(new ListItem(bottleneck, normalFont));
            }
            document.add(bottleneckList);
        }
        
        // Add performance issues table if available
        if (performance.getIssues() != null && !performance.getIssues().isEmpty()) {
            PdfPTable perfTable = new PdfPTable(5);
            perfTable.setWidthPercentage(100);
            perfTable.setWidths(new float[]{2, 1, 2, 1.5f, 2.5f});
            perfTable.setSpacingBefore(10);
            
            // Headers
            String[] headers = {"Type", "Severity", "Location", "Impact", "Solution"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, 
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE)));
                headerCell.setBackgroundColor(PRIMARY_COLOR);
                headerCell.setPadding(5);
                perfTable.addCell(headerCell);
            }
            
            // Data rows
            for (PerformanceAnalysis.PerformanceIssue issue : performance.getIssues()) {
                perfTable.addCell(new Phrase(issue.getType(), smallFont));
                
                PdfPCell severityCell = new PdfPCell(new Phrase(issue.getSeverity(), smallFont));
                severityCell.setBackgroundColor(issue.getSeverity().equals("HIGH") ? 
                    new BaseColor(254, 226, 226) : new BaseColor(254, 249, 195));
                perfTable.addCell(severityCell);
                
                perfTable.addCell(new Phrase(issue.getLocation(), smallFont));
                perfTable.addCell(new Phrase(issue.getEstimatedImpact(), smallFont));
                perfTable.addCell(new Phrase(issue.getSolution(), smallFont));
            }
            
            document.add(perfTable);
        }
        
        // Add optimizations if available
        if (performance.getOptimizations() != null && !performance.getOptimizations().isEmpty()) {
            Paragraph optPara = new Paragraph("Recommended Optimizations", subheadingFont);
            optPara.setSpacingBefore(10);
            optPara.setSpacingAfter(5);
            document.add(optPara);
            
            com.itextpdf.text.List optList = new com.itextpdf.text.List(com.itextpdf.text.List.ORDERED);
            for (String optimization : performance.getOptimizations()) {
                optList.add(new ListItem(optimization, normalFont));
            }
            document.add(optList);
        }
    }
    
    /**
     * Add code quality metrics section
     */
    private void addCodeQualityMetrics(Document document, CodeReviewResult result) throws DocumentException {
        QualityMetrics quality = result.getQuality();
        if (quality == null) {
            return;
        }
        
        document.add(new Paragraph("Code Quality Metrics", headingFont));
        
        PdfPTable qualityTable = new PdfPTable(2);
        qualityTable.setWidthPercentage(100);
        qualityTable.setSpacingBefore(10);
        
        addQualityMetric(qualityTable, "Lines of Code", String.valueOf(quality.getLinesOfCode()));
        addQualityMetric(qualityTable, "Cyclomatic Complexity", String.valueOf(quality.getComplexityScore()));
        addQualityMetric(qualityTable, "Maintainability Index", String.format("%.1f", quality.getMaintainabilityScore()));
        addQualityMetric(qualityTable, "Test Coverage", quality.getTestCoverage() + "%");
        addQualityMetric(qualityTable, "Duplicate Lines", String.valueOf(quality.getDuplicateLines()));
        addQualityMetric(qualityTable, "Readability Score", String.format("%.1f", quality.getReadabilityScore()));
        
        document.add(qualityTable);
    }
    
    /**
     * Add recommendations section
     */
    private void addRecommendations(Document document, CodeReviewResult result) throws DocumentException {
        if (result.getSuggestions() == null || result.getSuggestions().isEmpty()) {
            return;
        }
        
        document.newPage();
        document.add(new Paragraph("Recommendations", headingFont));
        
        com.itextpdf.text.List recList = new com.itextpdf.text.List(com.itextpdf.text.List.ORDERED);
        recList.setIndentationLeft(20);
        
        for (Suggestion suggestion : result.getSuggestions()) {
            // Create a comprehensive recommendation entry
            ListItem item = new ListItem();
            
            // Title
            if (suggestion.getTitle() != null) {
                item.add(new Chunk(suggestion.getTitle(), subheadingFont));
                item.add(Chunk.NEWLINE);
            }
            
            // Description
            item.add(new Chunk(suggestion.getDescription(), normalFont));
            
            // Category and Impact
            if (suggestion.getCategory() != null || suggestion.getImpact() != null) {
                item.add(Chunk.NEWLINE);
                Font metaFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC);
                if (suggestion.getCategory() != null) {
                    item.add(new Chunk("Category: " + suggestion.getCategory() + "  ", metaFont));
                }
                if (suggestion.getImpact() != null) {
                    item.add(new Chunk("Impact: " + suggestion.getImpact(), metaFont));
                }
            }
            
            // Implementation details
            if (suggestion.getImplementation() != null) {
                item.add(Chunk.NEWLINE);
                item.add(new Chunk("Implementation: ", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD)));
                item.add(new Chunk(suggestion.getImplementation(), smallFont));
            }
            
            item.setSpacingAfter(10);
            recList.add(item);
        }
        
        document.add(recList);
    }
    
    // Helper methods
    
    private BaseColor getScoreColor(double score) {
        if (score >= 8) return SUCCESS_COLOR;
        if (score >= 6) return WARNING_COLOR;
        return ERROR_COLOR;
    }
    
    private BaseColor getSeverityColor(String severity) {
        switch (severity) {
            case "CRITICAL": return new BaseColor(127, 29, 29);
            case "HIGH": return new BaseColor(194, 65, 12);
            case "MEDIUM": return new BaseColor(202, 138, 4);
            case "LOW": return new BaseColor(21, 128, 61);
            default: return BaseColor.GRAY;
        }
    }
    
    private void addMetricCell(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorderColor(color);
        cell.setBorderWidth(2);
        
        Paragraph labelPara = new Paragraph(label, smallFont);
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);
        
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, color);
        Paragraph valuePara = new Paragraph(value, valueFont);
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);
        
        table.addCell(cell);
    }
    
    private void addSecurityCountCell(PdfPTable table, String label, int count, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new BaseColor(color.getRed(), color.getGreen(), color.getBlue(), 50));
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        
        Paragraph labelPara = new Paragraph(label, smallFont);
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);
        
        Font countFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, color);
        Paragraph countPara = new Paragraph(String.valueOf(count), countFont);
        countPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(countPara);
        
        table.addCell(cell);
    }
    
    private void addQualityMetric(PdfPTable table, String metric, String value) {
        PdfPCell metricCell = new PdfPCell(new Phrase(metric, normalFont));
        metricCell.setPadding(8);
        metricCell.setBackgroundColor(new BaseColor(243, 244, 246));
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, 
            new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        valueCell.setPadding(8);
        
        table.addCell(metricCell);
        table.addCell(valueCell);
    }
    
    /**
     * Header/Footer page event handler
     */
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            
            // Footer
            Phrase footer = new Phrase(String.format("Page %d", writer.getPageNumber()), 
                                     new Font(Font.FontFamily.HELVETICA, 8));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                                     (document.right() - document.left()) / 2 + document.leftMargin(),
                                     document.bottom() - 10, 0);
            
            // Copyright
            Phrase copyright = new Phrase("© 2024 Smart Code Review - Powered by AWS Bedrock", 
                                        new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, copyright,
                                     (document.right() - document.left()) / 2 + document.leftMargin(),
                                     document.bottom() - 20, 0);
        }
    }
}