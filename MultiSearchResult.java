package com.suffixindex.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates search results combined across multiple indexed documents.
 */
public class MultiSearchResult {

    public enum SortOrder {
        BY_DOCUMENT_NAME,
        BY_MATCH_COUNT,
        BY_FIRST_POSITION
    }

    public static class DocumentMatchSummary {
        private final String documentId;
        private final String documentName;
        private final int matchCount;
        private final int firstMatchPosition;
        private final long searchTimeNs;
        private final List<SearchResult.MatchSnippet> snippets;
        private final List<Integer> positions;

        public DocumentMatchSummary(String documentId, String documentName, int matchCount,
                                    int firstMatchPosition, long searchTimeNs,
                                    List<SearchResult.MatchSnippet> snippets, List<Integer> positions) {
            this.documentId = documentId;
            this.documentName = documentName;
            this.matchCount = matchCount;
            this.firstMatchPosition = firstMatchPosition;
            this.searchTimeNs = searchTimeNs;
            this.snippets = snippets != null ? snippets : Collections.emptyList();
            this.positions = positions != null ? positions : Collections.emptyList();
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getDocumentName() {
            return documentName;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public int getFirstMatchPosition() {
            return firstMatchPosition;
        }

        public long getSearchTimeNs() {
            return searchTimeNs;
        }

        public double getSearchTimeMs() {
            return searchTimeNs / 1_000_000.0;
        }

        public List<SearchResult.MatchSnippet> getSnippets() {
            return snippets;
        }

        public List<Integer> getPositions() {
            return positions;
        }
    }

    private final String pattern;
    private final List<DocumentMatchSummary> summaries;
    private final long totalSearchTimeNs;
    private final int totalMatches;

    public MultiSearchResult(String pattern, List<DocumentMatchSummary> summaries, long totalSearchTimeNs) {
        this.pattern = pattern;
        this.summaries = summaries != null ? new ArrayList<>(summaries) : new ArrayList<>();
        this.totalSearchTimeNs = totalSearchTimeNs;

        int count = 0;
        for (DocumentMatchSummary d : this.summaries) {
            count += d.getMatchCount();
        }
        this.totalMatches = count;
    }

    public String getPattern() {
        return pattern;
    }

    public List<DocumentMatchSummary> getSummaries() {
        return summaries;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public int getMatchingDocumentsCount() {
        int docs = 0;
        for (DocumentMatchSummary d : summaries) {
            if (d.getMatchCount() > 0) docs++;
        }
        return docs;
    }

    public long getTotalSearchTimeNs() {
        return totalSearchTimeNs;
    }

    public double getTotalSearchTimeMs() {
        return totalSearchTimeNs / 1_000_000.0;
    }

    public void sort(SortOrder order) {
        switch (order) {
            case BY_DOCUMENT_NAME:
                summaries.sort((a, b) -> a.getDocumentName().compareToIgnoreCase(b.getDocumentName()));
                break;
            case BY_MATCH_COUNT:
                summaries.sort((a, b) -> Integer.compare(b.getMatchCount(), a.getMatchCount()));
                break;
            case BY_FIRST_POSITION:
                summaries.sort((a, b) -> {
                    int posA = a.getMatchCount() > 0 ? a.getFirstMatchPosition() : Integer.MAX_VALUE;
                    int posB = b.getMatchCount() > 0 ? b.getFirstMatchPosition() : Integer.MAX_VALUE;
                    return Integer.compare(posA, posB);
                });
                break;
        }
    }
}
