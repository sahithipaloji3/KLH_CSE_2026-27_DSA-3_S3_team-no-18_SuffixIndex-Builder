package com.suffixindex.model;

/**
 * Record for historical search queries.
 * Persisted in data/searches/search_history.txt.
 */
public class SearchHistoryEntry {
    private final String timestamp;
    private final String documentId;
    private final String documentName;
    private final String pattern;
    private final int matchCount;
    private final long searchTimeNs;

    public SearchHistoryEntry(String timestamp, String documentId, String documentName,
                              String pattern, int matchCount, long searchTimeNs) {
        this.timestamp = timestamp;
        this.documentId = documentId;
        this.documentName = documentName;
        this.pattern = pattern;
        this.matchCount = matchCount;
        this.searchTimeNs = searchTimeNs;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getPattern() {
        return pattern;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public long getSearchTimeNs() {
        return searchTimeNs;
    }

    public double getSearchTimeMs() {
        return searchTimeNs / 1_000_000.0;
    }

    /**
     * Serializes this entry to the file format:
     * TIMESTAMP | DOCUMENT_ID | DOCUMENT_NAME | PATTERN | MATCH_COUNT | SEARCH_TIME_NS
     */
    public String toFileString() {
        return timestamp + " | " + documentId + " | " + documentName + " | " + pattern + " | " + matchCount + " | " + searchTimeNs;
    }

    public static SearchHistoryEntry fromFileString(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split("\\s*\\|\\s*");
        if (parts.length < 5) return null;

        String ts = parts[0];
        String docId = parts[1];
        String docName = (parts.length >= 6) ? parts[2] : docId;
        String pat = (parts.length >= 6) ? parts[3] : parts[2];
        int count = 0;
        try {
            String countStr = (parts.length >= 6) ? parts[4] : parts[3];
            // Might be "2 matches" or "2"
            count = Integer.parseInt(countStr.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {}

        long timeNs = 0;
        if (parts.length >= 6) {
            try {
                timeNs = Long.parseLong(parts[5].replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }

        return new SearchHistoryEntry(ts, docId, docName, pat, count, timeNs);
    }
}
