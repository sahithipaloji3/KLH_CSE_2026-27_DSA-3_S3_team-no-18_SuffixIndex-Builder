package com.suffixindex.model;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the results of a substring search query.
 */
public class SearchResult {

    public static class MatchSnippet {
        private final int position;
        private final String prefixContext;
        private final String matchText;
        private final String suffixContext;

        public MatchSnippet(int position, String prefixContext, String matchText, String suffixContext) {
            this.position = position;
            this.prefixContext = prefixContext;
            this.matchText = matchText;
            this.suffixContext = suffixContext;
        }

        public int getPosition() {
            return position;
        }

        public String getPrefixContext() {
            return prefixContext;
        }

        public String getMatchText() {
            return matchText;
        }

        public String getSuffixContext() {
            return suffixContext;
        }

        public String getFullPreview() {
            return (prefixContext.isEmpty() ? "" : "..." + prefixContext)
                    + "[" + matchText + "]"
                    + (suffixContext.isEmpty() ? "" : suffixContext + "...");
        }
    }

    private final String pattern;
    private final String documentId;
    private final String documentName;
    private final List<Integer> positions;
    private final long searchTimeNs;
    private final int comparisonCount;
    private final String algorithmUsed;
    private final List<MatchSnippet> snippets;
    private final boolean caseSensitive;

    public SearchResult(String pattern, String documentId, String documentName,
                        List<Integer> positions, long searchTimeNs, int comparisonCount,
                        String algorithmUsed, List<MatchSnippet> snippets, boolean caseSensitive) {
        this.pattern = pattern;
        this.documentId = documentId;
        this.documentName = documentName;
        this.positions = positions != null ? positions : Collections.emptyList();
        this.searchTimeNs = searchTimeNs;
        this.comparisonCount = comparisonCount;
        this.algorithmUsed = algorithmUsed;
        this.snippets = snippets != null ? snippets : Collections.emptyList();
        this.caseSensitive = caseSensitive;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public int getOccurrences() {
        return positions.size();
    }

    public long getSearchTimeNs() {
        return searchTimeNs;
    }

    public double getSearchTimeMs() {
        return searchTimeNs / 1_000_000.0;
    }

    public int getComparisonCount() {
        return comparisonCount;
    }

    public String getAlgorithmUsed() {
        return algorithmUsed;
    }

    public List<MatchSnippet> getSnippets() {
        return snippets;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }
}
