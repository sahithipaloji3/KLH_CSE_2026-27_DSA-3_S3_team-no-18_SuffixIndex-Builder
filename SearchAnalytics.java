package com.suffixindex.model;

import java.util.List;

/**
 * Historical search telemetry computed dynamically from data/searches/search_history.txt.
 */
public class SearchAnalytics {
    private final int totalSearches;
    private final int successfulSearches;
    private final int unsuccessfulSearches;
    private final String mostSearchedPattern;
    private final int mostSearchedPatternCount;
    private final double averageSearchTimeMs;
    private final String mostSearchedDocument;
    private final List<SearchHistoryEntry> recentEntries;

    public SearchAnalytics(int totalSearches, int successfulSearches, int unsuccessfulSearches,
                           String mostSearchedPattern, int mostSearchedPatternCount,
                           double averageSearchTimeMs, String mostSearchedDocument,
                           List<SearchHistoryEntry> recentEntries) {
        this.totalSearches = totalSearches;
        this.successfulSearches = successfulSearches;
        this.unsuccessfulSearches = unsuccessfulSearches;
        this.mostSearchedPattern = mostSearchedPattern;
        this.mostSearchedPatternCount = mostSearchedPatternCount;
        this.averageSearchTimeMs = averageSearchTimeMs;
        this.mostSearchedDocument = mostSearchedDocument;
        this.recentEntries = recentEntries;
    }

    public int getTotalSearches() {
        return totalSearches;
    }

    public int getSuccessfulSearches() {
        return successfulSearches;
    }

    public int getUnsuccessfulSearches() {
        return unsuccessfulSearches;
    }

    public String getMostSearchedPattern() {
        return mostSearchedPattern != null ? mostSearchedPattern : "N/A";
    }

    public int getMostSearchedPatternCount() {
        return mostSearchedPatternCount;
    }

    public double getAverageSearchTimeMs() {
        return averageSearchTimeMs;
    }

    public String getMostSearchedDocument() {
        return mostSearchedDocument != null ? mostSearchedDocument : "N/A";
    }

    public List<SearchHistoryEntry> getRecentEntries() {
        return recentEntries;
    }
}
