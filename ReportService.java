package com.suffixindex.service;

import com.suffixindex.algorithm.RepeatedSubstringAnalyzer;
import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.DocumentStatistics;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.repository.ReportRepository;
import com.suffixindex.util.DateUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for generating, formatting, and saving comprehensive text/markdown reports
 * for documents and their suffix indexes.
 * Pure Java SE.
 */
public class ReportService {

    private final ReportRepository reportRepository;
    private final DocumentService documentService;
    private final IndexService indexService;
    private final AnalyticsService analyticsService;

    public ReportService(ReportRepository reportRepository, DocumentService documentService,
                         IndexService indexService, AnalyticsService analyticsService) {
        this.reportRepository = reportRepository;
        this.documentService = documentService;
        this.indexService = indexService;
        this.analyticsService = analyticsService;
    }

    public String generateReportContent(String documentId) throws IOException {
        DocumentMetadata meta = documentService.getDocumentMetadata(documentId);
        if (meta == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        Document doc = documentService.getDocument(documentId);
        SuffixIndex index = indexService.loadIndex(documentId);
        DocumentStatistics stats = analyticsService.computeDocumentStatistics(documentId);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                   SUFFIX INDEX BUILDER - COMPREHENSIVE REPORT                  \n");
        sb.append("================================================================================\n\n");

        // 1. Header
        sb.append("1. DOCUMENT & INTEGRITY HEADER\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("  Document ID:       %s\n", meta.getId()));
        sb.append(String.format("  Document Name:     %s\n", meta.getName()));
        sb.append(String.format("  Generated At:      %s\n", DateUtils.nowFormatted()));
        sb.append(String.format("  File Size:         %,d bytes\n", meta.getFileSizeBytes()));
        sb.append(String.format("  SHA-256 Hash:      %s\n", meta.getSha256Hash() != null ? meta.getSha256Hash() : "N/A"));
        sb.append(String.format("  Index Status:      %s\n", meta.getIndexStatus()));
        sb.append("\n");

        // 2. Document Metrics
        sb.append("2. DOCUMENT LEXICAL METRICS\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (stats != null) {
            sb.append(String.format("  Total Characters:  %,d\n", stats.getCharacterCount()));
            sb.append(String.format("  Total Words:       %,d\n", stats.getWordCount()));
            sb.append(String.format("  Total Lines:       %,d\n", stats.getLineCount()));
            sb.append(String.format("  Unique Vocabulary: %,d words\n", stats.getUniqueWordCount()));
            sb.append(String.format("  Avg Word Length:   %.2f characters\n", stats.getAverageWordLength()));
            sb.append(String.format("  Longest Word:      '%s' (%d chars)\n", stats.getLongestWord(), stats.getLongestWord().length()));
            sb.append(String.format("  Shortest Word:     '%s'\n", stats.getShortestWord()));
            double compressionEst = stats.getCharacterCount() > 0 ?
                    (double) stats.getUniqueWordCount() / stats.getWordCount() * 100.0 : 0.0;
            sb.append(String.format("  Lexical Diversity: %.2f%%\n", compressionEst));
        } else {
            sb.append("  (Lexical metrics unavailable)\n");
        }
        sb.append("\n");

        // 3. Index Metrics
        sb.append("3. SUFFIX INDEX & DATA STRUCTURE METRICS\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (index != null && index.isValid()) {
            sb.append(String.format("  Suffix Array Size: %,d suffixes\n", index.getSuffixCount()));
            sb.append(String.format("  LCP Array Size:    %,d entries\n", index.getLcpArray() != null ? index.getLcpArray().length : 0));
            sb.append(String.format("  Build Time:        %.3f ms (%,d ns)\n", index.getBuildTimeMs(), index.getBuildTimeNs()));
            sb.append(String.format("  Index Disk Size:   %,d bytes\n", meta.getIndexFileSizeBytes()));
            sb.append(String.format("  Algorithm Used:    Prefix Doubling (O(n log n)) + Kasai LCP (O(n))\n"));
        } else {
            sb.append("  Document is not currently indexed.\n");
        }
        sb.append("\n");

        // 4. Substring Analysis (LRS & Repeated Patterns)
        sb.append("4. ADVANCED SUBSTRING ANALYSIS (LRS & REPEATS)\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (index != null && index.isValid() && doc != null) {
            String text = doc.getContent();
            RepeatedSubstringAnalyzer.LRSResult lrs =
                    RepeatedSubstringAnalyzer.findLongestRepeatedSubstring(text, index.getSuffixArray(), index.getLcpArray());

            if (lrs != null && lrs.getLength() > 0) {
                sb.append(String.format("  Longest Repeated Substring (LRS): '%s'\n", escapeSpecial(lrs.getLongestRepeatedSubstring())));
                sb.append(String.format("  LRS Length:       %d characters\n", lrs.getLength()));
                sb.append(String.format("  LRS Occurrences:  %d times\n", lrs.getOccurrences()));
                sb.append(String.format("  Sample Positions: %s\n", lrs.getPositions().toString()));
            } else {
                sb.append("  No repeated substrings found (all characters or suffixes appear uniquely).\n");
            }

            sb.append("\n  Top Repeated Substrings:\n");
            List<RepeatedSubstringAnalyzer.RepeatedPattern> patterns =
                    RepeatedSubstringAnalyzer.findRepeatedPatterns(text, index.getSuffixArray(), index.getLcpArray(), 5);
            if (patterns.isEmpty()) {
                sb.append("    (None found)\n");
            } else {
                int rank = 1;
                for (RepeatedSubstringAnalyzer.RepeatedPattern pat : patterns) {
                    sb.append(String.format("    %d. '%s' (len: %d, count: %d, positions: %s)\n",
                            rank++, escapeSpecial(pat.getPattern()), pat.getLength(), pat.getOccurrences(), pat.getPositions()));
                }
            }
        } else {
            sb.append("  (Index required for repeated substring analysis)\n");
        }
        sb.append("\n");

        // 5. Suffix Array Sample
        sb.append("5. SUFFIX ARRAY & LCP SAMPLE\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (index != null && index.isValid() && doc != null) {
            int[] sa = index.getSuffixArray();
            int[] lcp = index.getLcpArray();
            String text = doc.getContent();
            int n = sa.length;

            sb.append(String.format("  %-6s | %-8s | %-5s | %s\n", "i", "SA[i]", "LCP[i]", "Suffix Preview"));
            sb.append("  -------+----------+-------+---------------------------------------------------\n");

            int topCount = Math.min(10, n);
            for (int i = 0; i < topCount; i++) {
                int pos = sa[i];
                int lcpVal = (lcp != null && i < lcp.length) ? lcp[i] : 0;
                String preview = text.substring(pos, Math.min(text.length(), pos + 40));
                preview = escapeSpecial(preview);
                sb.append(String.format("  %-6d | %-8d | %-5d | %s\n", i, pos, lcpVal, preview));
            }

            if (n > 15) {
                sb.append("  ...    | ...      | ...   | ... (middle entries omitted)\n");
                for (int i = Math.max(topCount, n - 5); i < n; i++) {
                    int pos = sa[i];
                    int lcpVal = (lcp != null && i < lcp.length) ? lcp[i] : 0;
                    String preview = text.substring(pos, Math.min(text.length(), pos + 40));
                    preview = escapeSpecial(preview);
                    sb.append(String.format("  %-6d | %-8d | %-5d | %s\n", i, pos, lcpVal, preview));
                }
            }
        } else {
            sb.append("  (Index not available)\n");
        }
        sb.append("\n");

        // 6. Character Distribution
        sb.append("6. CHARACTER FREQUENCY DISTRIBUTION\n");
        sb.append("--------------------------------------------------------------------------------\n");
        if (doc != null && doc.getContent() != null && !doc.getContent().isEmpty()) {
            Map<Character, Integer> freq = new HashMap<>();
            String content = doc.getContent();
            for (char c : content.toCharArray()) {
                freq.put(c, freq.getOrDefault(c, 0) + 1);
            }
            List<Map.Entry<Character, Integer>> entries = new ArrayList<>(freq.entrySet());
            entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            int topChars = Math.min(5, entries.size());
            for (int i = 0; i < topChars; i++) {
                Map.Entry<Character, Integer> e = entries.get(i);
                char c = e.getKey();
                String display = (c == ' ') ? "[Space]" : (c == '\n') ? "[Newline]" : (c == '\t') ? "[Tab]" : String.valueOf(c);
                double pct = (double) e.getValue() / content.length() * 100.0;
                sb.append(String.format("  %-10s : %,d occurrences (%.2f%%)\n", display, e.getValue(), pct));
            }
        }
        sb.append("\n");

        sb.append("================================================================================\n");
        sb.append("                            END OF DOCUMENT REPORT                              \n");
        sb.append("================================================================================\n");

        return sb.toString();
    }

    public Path generateAndSaveReport(String documentId) throws IOException {
        String content = generateReportContent(documentId);
        return reportRepository.saveReport(documentId, content);
    }

    public List<Path> listReports() throws IOException {
        return reportRepository.listReportFiles();
    }

    public String readReport(Path reportPath) throws IOException {
        return reportRepository.readReport(reportPath);
    }

    private String escapeSpecial(String s) {
        if (s == null) return "";
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
