package com.suffixindex.service;

import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.repository.DocumentRepository;
import com.suffixindex.repository.IndexRepository;
import com.suffixindex.repository.MetadataRepository;
import com.suffixindex.util.FileUtils;
import com.suffixindex.util.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service managing document lifecycle, imports, exports, and status checks.
 */
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MetadataRepository metadataRepository;
    private final IndexRepository indexRepository;

    public DocumentService() {
        this(new DocumentRepository(), new MetadataRepository(), new IndexRepository());
    }

    public DocumentService(DocumentRepository documentRepository,
                           MetadataRepository metadataRepository,
                           IndexRepository indexRepository) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.indexRepository = indexRepository;
    }

    /**
     * Generates a unique document ID based on existing documents.
     */
    private synchronized String generateNextDocumentId(Map<String, DocumentMetadata> existing) {
        int max = 0;
        for (String id : existing.keySet()) {
            if (id.startsWith("document_")) {
                try {
                    int num = Integer.parseInt(id.substring("document_".length()));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("document_%03d", max + 1);
    }

    /**
     * Creates a new document and stores it in data/documents/ and data/metadata/documents.meta.
     */
    public synchronized DocumentMetadata createDocument(String name, String content) throws IOException {
        ValidationUtils.requireNotBlank(name, "Document Name");
        if (content == null) content = "";

        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        String docId = generateNextDocumentId(metaMap);
        String safeName = FileUtils.sanitizeFilename(name);

        long now = System.currentTimeMillis();
        long hash = FileUtils.computeContentHash(content);
        String sha256 = FileUtils.computeSHA256(content);

        // Save raw text file
        documentRepository.saveDocumentContent(docId, content);

        DocumentMetadata meta = new DocumentMetadata(
                docId,
                safeName,
                documentRepository.getDocumentPath(docId).toString(),
                content.length(),
                IndexStatus.NOT_INDEXED,
                now,
                0L,
                docId + ".index",
                hash,
                sha256
        );

        metaMap.put(docId, meta);
        metadataRepository.saveAll(metaMap.values());

        return meta;
    }

    /**
     * Imports an existing .txt file into the system.
     */
    public synchronized DocumentMetadata importDocument(Path sourceFile) throws IOException {
        ValidationUtils.requireNotNull(sourceFile, "Source File");
        if (!Files.exists(sourceFile)) {
            throw new IllegalArgumentException("Source file does not exist: " + sourceFile);
        }

        String content = FileUtils.readFileToString(sourceFile);
        String originalName = sourceFile.getFileName().toString();
        return createDocument(originalName, content);
    }

    /**
     * Reads a document with its metadata and content, synchronizing status.
     */
    public synchronized Document getDocument(String documentId) throws IOException {
        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        DocumentMetadata meta = metaMap.get(documentId);
        if (meta == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        String content = documentRepository.readDocumentContent(documentId);
        syncDocumentStatus(meta, content);
        return new Document(meta, content);
    }

    public synchronized DocumentMetadata getDocumentMetadata(String documentId) throws IOException {
        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        return metaMap.get(documentId);
    }

    /**
     * Updates an existing document's name and content.
     * If content changes, the index is marked OUTDATED.
     */
    public synchronized void updateDocument(String documentId, String newName, String newContent) throws IOException {
        ValidationUtils.requireNotBlank(documentId, "Document ID");
        ValidationUtils.requireNotBlank(newName, "Document Name");
        if (newContent == null) newContent = "";

        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        DocumentMetadata meta = metaMap.get(documentId);
        if (meta == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        long newHash = FileUtils.computeContentHash(newContent);
        String newSha256 = FileUtils.computeSHA256(newContent);
        boolean contentChanged = (newHash != meta.getContentHash()) || (newContent.length() != meta.getCharacterCount());

        meta.setDocumentName(FileUtils.sanitizeFilename(newName));
        meta.setCharacterCount(newContent.length());
        meta.setLastModified(System.currentTimeMillis());
        meta.setContentHash(newHash);
        meta.setSha256Hash(newSha256);

        if (contentChanged) {
            if (indexRepository.indexExists(documentId)) {
                meta.setIndexStatus(IndexStatus.OUTDATED);
            } else {
                meta.setIndexStatus(IndexStatus.NOT_INDEXED);
            }
        }

        documentRepository.saveDocumentContent(documentId, newContent);
        metaMap.put(documentId, meta);
        metadataRepository.saveAll(metaMap.values());
    }

    /**
     * Deletes a document, removing its .txt file, .index file, and metadata record.
     */
    public synchronized boolean deleteDocument(String documentId) throws IOException {
        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        if (!metaMap.containsKey(documentId)) {
            return false;
        }

        documentRepository.deleteDocumentFile(documentId);
        indexRepository.deleteIndexFile(documentId);

        metaMap.remove(documentId);
        metadataRepository.saveAll(metaMap.values());
        return true;
    }

    /**
     * Returns all documents, dynamically checking whether any indexes have become outdated.
     */
    public synchronized List<DocumentMetadata> getAllDocuments() throws IOException {
        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        boolean changed = false;

        for (DocumentMetadata meta : metaMap.values()) {
            if (documentRepository.documentFileExists(meta.getDocumentId())) {
                String content = documentRepository.readDocumentContent(meta.getDocumentId());
                if (syncDocumentStatus(meta, content)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            metadataRepository.saveAll(metaMap.values());
        }

        return new ArrayList<>(metaMap.values());
    }

    /**
     * Verifies if the document's index is currently valid, outdated, or missing.
     */
    private boolean syncDocumentStatus(DocumentMetadata meta, String content) {
        IndexStatus oldStatus = meta.getIndexStatus();
        long currentHash = FileUtils.computeContentHash(content);
        boolean changed = false;

        if (meta.getSha256Hash() == null || meta.getSha256Hash().isEmpty()) {
            meta.setSha256Hash(FileUtils.computeSHA256(content));
            changed = true;
        }

        boolean hasIndex = indexRepository.indexExists(meta.getDocumentId());

        if (!hasIndex) {
            meta.setIndexStatus(IndexStatus.NOT_INDEXED);
        } else if (meta.getLastIndexed() > 0 && currentHash != meta.getContentHash()) {
            meta.setIndexStatus(IndexStatus.OUTDATED);
        } else if (meta.getLastIndexed() > 0 && meta.getLastModified() > meta.getLastIndexed()) {
            meta.setIndexStatus(IndexStatus.OUTDATED);
        } else if (hasIndex) {
            meta.setIndexStatus(IndexStatus.INDEXED);
        }

        return (oldStatus != meta.getIndexStatus()) || changed;
    }
}
