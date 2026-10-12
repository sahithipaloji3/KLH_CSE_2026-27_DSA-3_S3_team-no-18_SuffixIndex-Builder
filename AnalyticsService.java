package com.suffixindex.service;

import com.suffixindex.model.*;
import com.suffixindex.repository.DocumentRepository;
import com.suffixindex.repository.IndexRepository;
import com.suffixindex.repository.MetadataRepository;
import com.suffixindex.repository.SearchHistoryRepository;
import com.suffixindex.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for computing document lexical statistics and search history analytics.
 */
public class AnalyticsService {

    private final DocumentRepository documentRepository;
    private final MetadataRepository metadataRepository;
    private final IndexRepository indexRepository;
    private final SearchHistoryRepository historyRepository;

    public AnalyticsService(DocumentRepository documentRepository,
                            MetadataRepository metadataRepository,
                            IndexRepository indexRepository,
                            SearchHistoryRepository historyRepository) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.indexRepository = indexRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Computes lexical metrics and index properties for a given document.
     */
    public DocumentStatistics computeDocumentStatistics(String documentId) throws IOException {
        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        DocumentMetadata meta = metaMap.get(documentId);
        if (meta == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        String content = documentRepository.readDocumentContent(documentId);
        long chars = content.length();

        // Line count
        int lines = content.isEmpty() ? 0 : content.split("\r\n|\r|\n", -1).length;

        // Word count and vocabulary stats
        String[] tokens = content.trim().isEmpty() ? new String[0] : content.split("[^a-zA-Z0-9_-]+");
        int words = 0;
        Set<String> uniqueWords = new HashSet<>();
        long totalWordLen = 0;
        String longest = "";
        String shortest = null;

        for (String t : tokens) {
            String w = t.trim();
            if (!w.isEmpty()) {
                words++;
                uniqueWords.add(w.toLowerCase());
                totalWordLen += w.length();
                if (w.length() > longest.length()) {
                    longest = w;
                }
                if (shortest == null || w.length() < shortest.length()) {
                    shortest = w;
                }
            }
        }

        double avgWordLen = words > 0 ? (double) totalWordLen / words : 0.0;
        if (shortest == null) shortest = "";

        // Index metrics
        SuffixIndex index = indexRepository.loadIndex(documentId);
        boolean isIndexed = (index != null && index.getSuffixCount() > 0);
        int suffixCount = isIndexed ? index.getSuffixCount() : 0;
        int lcpEntries = (isIndexed && index.getLcpArray() != null) ? index.getLcpArray().length : 0;
        long buildTimeNs = isIndexed ? index.getConstructionTimeNs() : 0L;

        Path indexPath = indexRepository.getIndexPath(documentId);
        long indexFileSize = Files.exists(indexPath) ? Files.size(indexPath) : 0L;
        String sha256 = FileUtils.computeSHA256(content);

        return new DocumentStatistics(
                meta.getDocumentName(),
                chars,
                words,
                lines,
                uniqueWords.size(),
                avgWordLen,
                longest,
                shortest,
                suffixCount,
                indexFileSize,
                lcpEntries,
                buildTimeNs,
                isIndexed,
                sha256
        );
    }

    /**
     * Aggregates search history telemetry into summary analytics.
     */
    public SearchAnalytics computeSearchAnalytics() {
        List<SearchHistoryEntry> entries = historyRepository.loadAll();
        int total = entries.size();
        int successful = 0;
        int unsuccessful = 0;
        long totalTimeNs = 0;

        Map<String, Integer> patternFreq = new HashMap<>();
        Map<String, Integer> docFreq = new HashMap<>();

        for (SearchHistoryEntry e : entries) {
            if (e.getMatchCount() > 0) {
                successful++;
            } else {
                unsuccessful++;
            }
            totalTimeNs += e.getSearchTimeNs();

            String pat = e.getPattern().trim().toLowerCase();
            patternFreq.put(pat, patternFreq.getOrDefault(pat, 0) + 1);

            String doc = e.getDocumentName();
            docFreq.put(doc, docFreq.getOrDefault(doc, 0) + 1);
        }

        // Most searched pattern
        String mostPat = null;
        int maxPatCount = 0;
        for (Map.Entry<String, Integer> entry : patternFreq.entrySet()) {
            if (entry.getValue() > maxPatCount) {
                maxPatCount = entry.getValue();
                mostPat = entry.getKey();
            }
        }

        // Most searched document
        String mostDoc = null;
        int maxDocCount = 0;
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            if (entry.getValue() > maxDocCount) {
                maxDocCount = entry.getValue();
                mostDoc = entry.getKey();
            }
        }

        double avgTimeMs = total > 0 ? (totalTimeNs / 1_000_000.0) / total : 0.0;

        return new SearchAnalytics(
                total,
                successful,
                unsuccessful,
                mostPat,
                maxPatCount,
                avgTimeMs,
                mostDoc,
                entries
        );
    }
}
