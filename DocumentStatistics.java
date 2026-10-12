package com.suffixindex.model;

/**
 * Detailed statistical metrics for a text document and its index.
 */
public class DocumentStatistics {
    private final String documentName;
    private final long characterCount;
    private final int wordCount;
    private final int lineCount;
    private final int uniqueWordCount;
    private final double averageWordLength;
    private final String longestWord;
    private final String shortestWord;

    // Index metrics (0 / empty if not indexed)
    private final int suffixCount;
    private final long indexFileSizeBytes;
    private final int lcpEntries;
    private final long indexBuildTimeNs;
    private final boolean indexed;
    private final String sha256Hash;

    public DocumentStatistics(String documentName, long characterCount, int wordCount, int lineCount,
                              int uniqueWordCount, double averageWordLength, String longestWord, String shortestWord,
                              int suffixCount, long indexFileSizeBytes, int lcpEntries, long indexBuildTimeNs,
                              boolean indexed, String sha256Hash) {
        this.documentName = documentName;
        this.characterCount = characterCount;
        this.wordCount = wordCount;
        this.lineCount = lineCount;
        this.uniqueWordCount = uniqueWordCount;
        this.averageWordLength = averageWordLength;
        this.longestWord = longestWord;
        this.shortestWord = shortestWord;
        this.suffixCount = suffixCount;
        this.indexFileSizeBytes = indexFileSizeBytes;
        this.lcpEntries = lcpEntries;
        this.indexBuildTimeNs = indexBuildTimeNs;
        this.indexed = indexed;
        this.sha256Hash = sha256Hash;
    }

    public String getDocumentName() {
        return documentName;
    }

    public long getCharacterCount() {
        return characterCount;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getLineCount() {
        return lineCount;
    }

    public int getUniqueWordCount() {
        return uniqueWordCount;
    }

    public double getAverageWordLength() {
        return averageWordLength;
    }

    public String getLongestWord() {
        return longestWord;
    }

    public String getShortestWord() {
        return shortestWord;
    }

    public int getSuffixCount() {
        return suffixCount;
    }

    public long getIndexFileSizeBytes() {
        return indexFileSizeBytes;
    }

    public int getLcpEntries() {
        return lcpEntries;
    }

    public long getIndexBuildTimeNs() {
        return indexBuildTimeNs;
    }

    public double getIndexBuildTimeMs() {
        return indexBuildTimeNs / 1_000_000.0;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }
}
