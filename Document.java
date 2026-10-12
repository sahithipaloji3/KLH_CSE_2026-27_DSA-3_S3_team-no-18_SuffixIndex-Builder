package com.suffixindex.model;

/**
 * In-memory representation of an active document with full content and metadata.
 */
public class Document {
    private final DocumentMetadata metadata;
    private String content;

    public Document(DocumentMetadata metadata, String content) {
        this.metadata = metadata;
        this.content = content != null ? content : "";
    }

    public DocumentMetadata getMetadata() {
        return metadata;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content : "";
        this.metadata.setCharacterCount(this.content.length());
        this.metadata.setLastModified(System.currentTimeMillis());
    }

    public String getId() {
        return metadata.getDocumentId();
    }

    public String getName() {
        return metadata.getDocumentName();
    }

    public int length() {
        return content.length();
    }
}
