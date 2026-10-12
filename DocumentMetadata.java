package com.suffixindex.model;

/**
 * Metadata record for a managed text document.
 * Stored in data/metadata/documents.meta.
 */
public class DocumentMetadata {
    private String documentId;
    private String documentName;
    private String filePath;
    private long characterCount;
    private IndexStatus indexStatus;
    private long lastModified;
    private long lastIndexed;
    private String indexFileName;
    private long contentHash;
    private String sha256Hash;

    public DocumentMetadata() {
        this.indexStatus = IndexStatus.NOT_INDEXED;
        this.sha256Hash = "";
    }

    public DocumentMetadata(String documentId, String documentName, String filePath,
                            long characterCount, IndexStatus indexStatus,
                            long lastModified, long lastIndexed,
                            String indexFileName, long contentHash) {
        this(documentId, documentName, filePath, characterCount, indexStatus, lastModified, lastIndexed, indexFileName, contentHash, "");
    }

    public DocumentMetadata(String documentId, String documentName, String filePath,
                            long characterCount, IndexStatus indexStatus,
                            long lastModified, long lastIndexed,
                            String indexFileName, long contentHash, String sha256Hash) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.filePath = filePath;
        this.characterCount = characterCount;
        this.indexStatus = indexStatus;
        this.lastModified = lastModified;
        this.lastIndexed = lastIndexed;
        this.indexFileName = indexFileName;
        this.contentHash = contentHash;
        this.sha256Hash = sha256Hash != null ? sha256Hash : "";
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getName() {
        return documentName;
    }

    public long getFileSizeBytes() {
        return characterCount;
    }

    public long getIndexFileSizeBytes() {
        return characterCount * 8L; // approximate / standard memory footprint
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(long characterCount) {
        this.characterCount = characterCount;
    }

    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(IndexStatus indexStatus) {
        this.indexStatus = indexStatus;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getLastIndexed() {
        return lastIndexed;
    }

    public void setLastIndexed(long lastIndexed) {
        this.lastIndexed = lastIndexed;
    }

    public String getIndexFileName() {
        return indexFileName;
    }

    public void setIndexFileName(String indexFileName) {
        this.indexFileName = indexFileName;
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

    @Override
    public String toString() {
        return documentName + " (" + documentId + ", " + characterCount + " chars, " + indexStatus + ")";
    }
}
