package com.suffixindex.service;

import com.suffixindex.algorithm.LCPArrayBuilder;
import com.suffixindex.algorithm.SuffixArrayBuilder;
import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.repository.DocumentRepository;
import com.suffixindex.repository.IndexRepository;
import com.suffixindex.repository.MetadataRepository;
import com.suffixindex.util.FileUtils;
import com.suffixindex.util.ValidationUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Service managing index construction, verification, and persistence.
 */
public class IndexService {

    private final DocumentRepository documentRepository;
    private final MetadataRepository metadataRepository;
    private final IndexRepository indexRepository;

    public IndexService() {
        this(new DocumentRepository(), new MetadataRepository(), new IndexRepository());
    }

    public IndexService(DocumentRepository documentRepository,
                        MetadataRepository metadataRepository,
                        IndexRepository indexRepository) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.indexRepository = indexRepository;
    }

    /**
     * Builds Suffix Array (Prefix Doubling) and LCP Array (Kasai) for the document,
     * measures exact nanoTime, and writes index to disk.
     */
    public synchronized SuffixIndex buildIndex(String documentId) throws IOException {
        ValidationUtils.requireNotBlank(documentId, "Document ID");

        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        DocumentMetadata meta = metaMap.get(documentId);
        if (meta == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        String content = documentRepository.readDocumentContent(documentId);
        long contentHash = FileUtils.computeContentHash(content);
        String sha256Hash = FileUtils.computeSHA256(content);

        // Precise nanoTime profiling of algorithm execution
        long startNs = System.nanoTime();

        // 1. Suffix Array Construction (Prefix Doubling)
        int[] suffixArray = SuffixArrayBuilder.buildSuffixArray(content);

        // 2. LCP Array Construction (Kasai's Algorithm)
        int[] lcpArray = LCPArrayBuilder.buildLCPArray(content, suffixArray);

        long durationNs = System.nanoTime() - startNs;

        // Build domain model
        SuffixIndex index = new SuffixIndex(
                documentId,
                content.length(),
                contentHash,
                sha256Hash,
                durationNs,
                suffixArray,
                lcpArray
        );

        // Persist index to data/indexes/<doc_id>.index
        indexRepository.saveIndex(index);

        // Update metadata
        long now = System.currentTimeMillis();
        meta.setIndexStatus(IndexStatus.INDEXED);
        meta.setLastIndexed(now);
        meta.setContentHash(contentHash);
        meta.setSha256Hash(sha256Hash);
        meta.setIndexFileName(documentId + ".index");
        meta.setCharacterCount(content.length());
        metaMap.put(documentId, meta);
        metadataRepository.saveAll(metaMap.values());

        return index;
    }

    /**
     * Retrieves the SuffixIndex from disk.
     */
    public SuffixIndex getIndex(String documentId) throws IOException {
        ValidationUtils.requireNotBlank(documentId, "Document ID");
        return indexRepository.loadIndex(documentId);
    }

    public SuffixIndex loadIndex(String documentId) throws IOException {
        return getIndex(documentId);
    }

    /**
     * Cryptographically verifies index integrity against document text.
     */
    public boolean verifyIndexIntegrity(String documentId) throws IOException {
        if (!indexRepository.indexExists(documentId)) {
            return false;
        }
        SuffixIndex index = indexRepository.loadIndex(documentId);
        if (index == null || !index.isValid()) {
            return false;
        }
        String currentContent = documentRepository.readDocumentContent(documentId);
        String currentSha256 = FileUtils.computeSHA256(currentContent);
        return currentSha256.equalsIgnoreCase(index.getSha256Hash());
    }

    /**
     * Checks if the index is missing or outdated compared to current document content.
     */
    public boolean isIndexOutdated(String documentId) throws IOException {
        if (!indexRepository.indexExists(documentId)) {
            return true;
        }

        Map<String, DocumentMetadata> metaMap = metadataRepository.loadAll();
        DocumentMetadata meta = metaMap.get(documentId);
        if (meta == null) {
            return true;
        }

        SuffixIndex index = indexRepository.loadIndex(documentId);
        if (index == null) {
            return true;
        }

        String currentContent = documentRepository.readDocumentContent(documentId);
        long currentHash = FileUtils.computeContentHash(currentContent);

        return index.getContentHash() != currentHash || index.getTextLength() != currentContent.length();
    }
}
