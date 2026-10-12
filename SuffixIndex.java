package com.suffixindex.model;

/**
 * Represents a compiled Suffix Index (Suffix Array + LCP Array) for a document.
 */
public class SuffixIndex {
    public static final int CURRENT_VERSION = 1;

    private int version;
    private String documentId;
    private int textLength;
    private int suffixCount;
    private long contentHash;
    private String sha256Hash;
    private long constructionTimeNs;
    private long createdAt;
    private int[] suffixArray;
    private int[] lcpArray;

    public SuffixIndex() {
        this.version = CURRENT_VERSION;
        this.createdAt = System.currentTimeMillis();
        this.sha256Hash = "";
    }

    public SuffixIndex(String documentId, int textLength, long contentHash,
                       long constructionTimeNs, int[] suffixArray, int[] lcpArray) {
        this(documentId, textLength, contentHash, "", constructionTimeNs, suffixArray, lcpArray);
    }

    public SuffixIndex(String documentId, int textLength, long contentHash, String sha256Hash,
                       long constructionTimeNs, int[] suffixArray, int[] lcpArray) {
        this.version = CURRENT_VERSION;
        this.documentId = documentId;
        this.textLength = textLength;
        this.suffixCount = (suffixArray != null) ? suffixArray.length : 0;
        this.contentHash = contentHash;
        this.sha256Hash = sha256Hash != null ? sha256Hash : "";
        this.constructionTimeNs = constructionTimeNs;
        this.createdAt = System.currentTimeMillis();
        this.suffixArray = suffixArray;
        this.lcpArray = lcpArray;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public int getTextLength() {
        return textLength;
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

    public int getSuffixCount() {
        return suffixCount;
    }

    public void setSuffixCount(int suffixCount) {
        this.suffixCount = suffixCount;
    }

    public long getContentHash() {
        return contentHash;
    }

    public void setContentHash(long contentHash) {
        this.contentHash = contentHash;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash != null ? sha256Hash : "";
    }

    public long getConstructionTimeNs() {
        return constructionTimeNs;
    }

    public void setConstructionTimeNs(long constructionTimeNs) {
        this.constructionTimeNs = constructionTimeNs;
    }

    public double getConstructionTimeMs() {
        return constructionTimeNs / 1_000_000.0;
    }

    public double getBuildTimeMs() {
        return getConstructionTimeMs();
    }

    public long getBuildTimeNs() {
        return getConstructionTimeNs();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int[] getSuffixArray() {
        return suffixArray;
    }

    public void setSuffixArray(int[] suffixArray) {
        this.suffixArray = suffixArray;
        this.suffixCount = (suffixArray != null) ? suffixArray.length : 0;
    }

    public int[] getLcpArray() {
        return lcpArray;
    }

    public void setLcpArray(int[] lcpArray) {
        this.lcpArray = lcpArray;
    }

    public boolean isValid() {
        return suffixArray != null && suffixArray.length > 0;
    }
}
