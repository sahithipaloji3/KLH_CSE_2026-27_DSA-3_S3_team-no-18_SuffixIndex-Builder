package com.suffixindex.service;

import com.suffixindex.algorithm.SuffixSearcher;
import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.MultiSearchResult;
import com.suffixindex.model.SearchResult;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.repository.DocumentRepository;
import com.suffixindex.repository.IndexRepository;
import com.suffixindex.repository.MetadataRepository;
import com.suffixindex.repository.SearchHistoryRepository;
import com.suffixindex.util.DateUtils;
import com.suffixindex.util.ValidationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for executing multi-document searches across all valid suffix indexes.
 * Pure Java, strictly file-based.
 */
public class MultiDocumentSearchService {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final DocumentRepository documentRepository;
    private final IndexRepository indexRepository;
    private final SearchHistoryRepository historyRepository;

    public MultiDocumentSearchService(DocumentService documentService,
                                      IndexService indexService,
                                      DocumentRepository documentRepository,
                                      IndexRepository indexRepository,
                                      SearchHistoryRepository historyRepository) {
        this.documentService = documentService;
        this.indexService = indexService;
        this.documentRepository = documentRepository;
        this.indexRepository = indexRepository;
        this.historyRepository = historyRepository;
    }

    public MultiDocumentSearchService(DocumentService documentService,
                                      IndexService indexService,
                                      SearchService searchService) {
        this(documentService, indexService, new DocumentRepository(), new IndexRepository(), new SearchHistoryRepository());
    }

    public MultiSearchResult searchAllDocuments(String pattern, boolean caseSensitive, int contextLength) throws IOException {
        return searchAllDocuments(pattern, caseSensitive, contextLength, MultiSearchResult.SortOrder.BY_MATCH_COUNT);
    }

    /**
     * Searches across all documents that currently possess a valid compiled suffix index.
     */
    public MultiSearchResult searchAllDocuments(String pattern, boolean caseSensitive,
                                                int contextLength, MultiSearchResult.SortOrder sortOrder) throws IOException {
        ValidationUtils.requireNotBlank(pattern, "Search Pattern");

        long startTotalNs = System.nanoTime();
        List<MultiSearchResult.DocumentMatchSummary> summaries = new ArrayList<>();
        List<DocumentMetadata> docs = documentService.getAllDocuments();
        int totalMatchesAcrossAll = 0;

        for (DocumentMetadata meta : docs) {
            String docId = meta.getDocumentId();

            // Only search documents that have an up-to-date index
            if (!indexRepository.indexExists(docId) || indexService.isIndexOutdated(docId)) {
                continue;
            }

            SuffixIndex index = indexRepository.loadIndex(docId);
            if (index == null || index.getSuffixArray() == null) {
                continue;
            }

            String content = documentRepository.readDocumentContent(docId);

            // Execute Suffix Array Binary Search
            SuffixSearcher.SearchOutcome outcome = SuffixSearcher.search(content, pattern, index.getSuffixArray(), caseSensitive);

            if (outcome.getMatchCount() > 0) {
                totalMatchesAcrossAll += outcome.getMatchCount();
                List<SearchResult.MatchSnippet> snippets = generateSnippets(content, pattern, outcome.getPositions(), contextLength);
                int firstPos = outcome.getPositions().get(0);

                summaries.add(new MultiSearchResult.DocumentMatchSummary(
                        docId,
                        meta.getDocumentName(),
                        outcome.getMatchCount(),
                        firstPos,
                        outcome.getExecutionTimeNs(),
                        snippets,
                        outcome.getPositions()
                ));
            }
        }

        long totalDurationNs = System.nanoTime() - startTotalNs;
        MultiSearchResult result = new MultiSearchResult(pattern, summaries, totalDurationNs);

        if (sortOrder != null) {
            result.sort(sortOrder);
        }

        // Log query
        historyRepository.appendEntry(new com.suffixindex.model.SearchHistoryEntry(
                DateUtils.currentDateTimeString(),
                "ALL_DOCS",
                "Multiple Documents (" + summaries.size() + " matches)",
                pattern,
                totalMatchesAcrossAll,
                totalDurationNs
        ));

        return result;
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
