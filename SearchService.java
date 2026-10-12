package com.suffixindex.service;

import com.suffixindex.algorithm.NaiveSearcher;
import com.suffixindex.algorithm.SuffixSearcher;
import com.suffixindex.model.*;
import com.suffixindex.repository.DocumentRepository;
import com.suffixindex.repository.IndexRepository;
import com.suffixindex.repository.MetadataRepository;
import com.suffixindex.repository.SearchHistoryRepository;
import com.suffixindex.util.DateUtils;
import com.suffixindex.util.FileUtils;
import com.suffixindex.util.ValidationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service managing search queries, snippet generation, and performance comparisons.
 */
public class SearchService {

    public static class OutdatedIndexException extends Exception {
        public OutdatedIndexException(String message) {
            super(message);
        }
    }

    public static class SearchComparison {
        private final SearchResult suffixSearchResult;
        private final NaiveSearcher.NaiveSearchOutcome naiveOutcome;

        public SearchComparison(SearchResult suffixSearchResult, NaiveSearcher.NaiveSearchOutcome naiveOutcome) {
            this.suffixSearchResult = suffixSearchResult;
            this.naiveOutcome = naiveOutcome;
        }

        public SearchResult getSuffixSearchResult() {
            return suffixSearchResult;
        }

        public NaiveSearcher.NaiveSearchOutcome getNaiveOutcome() {
            return naiveOutcome;
        }

        public double getSpeedupFactor() {
            if (suffixSearchResult.getSearchTimeNs() == 0) return 1.0;
            return (double) naiveOutcome.getExecutionTimeNs() / suffixSearchResult.getSearchTimeNs();
        }
    }

    private final DocumentRepository documentRepository;
    private final MetadataRepository metadataRepository;
    private final IndexRepository indexRepository;
    private final SearchHistoryRepository historyRepository;

    public SearchService() {
        this(new DocumentRepository(), new MetadataRepository(), new IndexRepository(), new SearchHistoryRepository());
    }

    public SearchService(DocumentRepository documentRepository,
                         MetadataRepository metadataRepository,
                         IndexRepository indexRepository,
                         SearchHistoryRepository historyRepository) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.indexRepository = indexRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Executes binary search on the document's suffix index with standard parameters.
     */
    public SearchComparison executeSearch(String documentId, String pattern, boolean caseSensitive, int contextLength)
            throws IOException, OutdatedIndexException {
        return executeSearch(documentId, pattern, caseSensitive, false, false, contextLength);
    }

    /**
     * Executes binary search on the document's suffix index with advanced filter options.
     */
    public SearchComparison executeSearch(String documentId, String pattern, boolean caseSensitive,
                                         boolean wholeWord, boolean prefixOnly, int contextLength)
            throws IOException, OutdatedIndexException {
        ValidationUtils.requireNotBlank(documentId, "Document ID");
        ValidationUtils.requireNotBlank(pattern, "Search Pattern");

        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        DocumentMetadata meta = metaMap.get(documentId);
        if (meta == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        SuffixIndex index = indexRepository.loadIndex(documentId);
        if (index == null) {
            throw new OutdatedIndexException("No index exists for document '" + meta.getDocumentName() + "'. Please build the index first.");
        }

        String content = documentRepository.readDocumentContent(documentId);
        long currentHash = FileUtils.computeContentHash(content);

        if (index.getContentHash() != currentHash || index.getTextLength() != content.length()) {
            throw new OutdatedIndexException("The index for document '" + meta.getDocumentName() + "' is OUTDATED because the file was modified since indexing. Please rebuild the index.");
        }

        // 1. Suffix Array Binary Search
        SuffixSearcher.SearchOutcome saOutcome = SuffixSearcher.search(content, pattern, index.getSuffixArray(), caseSensitive);

        // 2. Naive Brute Force Search for direct benchmark comparison
        NaiveSearcher.NaiveSearchOutcome naiveOutcome = NaiveSearcher.search(content, pattern, caseSensitive);

        List<Integer> filteredPositions = new ArrayList<>(saOutcome.getPositions());
        if (wholeWord) {
            filteredPositions.removeIf(pos -> !isWholeWordMatch(content, pos, pattern.length()));
        }
        if (prefixOnly) {
            filteredPositions.removeIf(pos -> !isPrefixMatch(content, pos));
        }

        // 3. Generate snippets with surrounding context
        List<SearchResult.MatchSnippet> snippets = generateSnippets(content, pattern, filteredPositions, contextLength);

        SearchResult result = new SearchResult(
                pattern,
                documentId,
                meta.getDocumentName(),
                filteredPositions,
                saOutcome.getExecutionTimeNs(),
                saOutcome.getComparisonCount(),
                "Suffix Array Binary Search",
                snippets,
                caseSensitive
        );

        // 4. Log search to search_history.txt
        SearchHistoryEntry historyEntry = new SearchHistoryEntry(
                DateUtils.currentDateTimeString(),
                documentId,
                meta.getDocumentName(),
                pattern,
                filteredPositions.size(),
                saOutcome.getExecutionTimeNs()
        );
        historyRepository.appendEntry(historyEntry);

        return new SearchComparison(result, naiveOutcome);
    }

    private boolean isWholeWordMatch(String text, int pos, int patLen) {
        boolean startBoundary = (pos == 0) || !Character.isLetterOrDigit(text.charAt(pos - 1));
        int endPos = pos + patLen;
        boolean endBoundary = (endPos >= text.length()) || !Character.isLetterOrDigit(text.charAt(endPos));
        return startBoundary && endBoundary;
    }

    private boolean isPrefixMatch(String text, int pos) {
        return (pos == 0) || Character.isWhitespace(text.charAt(pos - 1));
    }

    private List<SearchResult.MatchSnippet> generateSnippets(String text, String pattern, List<Integer> positions, int contextLength) {
        List<SearchResult.MatchSnippet> list = new ArrayList<>();
        int textLen = text.length();
        int patLen = pattern.length();

        for (int pos : positions) {
            int startPrefix = Math.max(0, pos - contextLength);
            String prefix = text.substring(startPrefix, pos);

            int endMatch = Math.min(textLen, pos + patLen);
            String match = text.substring(pos, endMatch);

            int endSuffix = Math.min(textLen, endMatch + contextLength);
            String suffix = text.substring(endMatch, endSuffix);

            list.add(new SearchResult.MatchSnippet(pos, prefix, match, suffix));
        }

        return list;
    }
}
