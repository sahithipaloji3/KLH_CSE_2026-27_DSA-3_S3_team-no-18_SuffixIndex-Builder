package com.suffixindex.service;

import com.suffixindex.algorithm.Trie;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.repository.DocumentRepository;

import java.io.IOException;
import java.util.List;

/**
 * Service managing Trie-based word indexing and autocomplete suggestions.
 */
public class AutocompleteService {

    private final Trie trie;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public AutocompleteService(DocumentService documentService, DocumentRepository documentRepository) {
        this.trie = new Trie();
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        rebuildTrieFromDocuments();
    }

    /**
     * Re-scans all managed document files and populates the Trie with their words.
     */
    public synchronized void rebuildTrieFromDocuments() {
        trie.clear();
        try {
            List<DocumentMetadata> docs = documentService.getAllDocuments();
            for (DocumentMetadata meta : docs) {
                if (documentRepository.documentFileExists(meta.getDocumentId())) {
                    String content = documentRepository.readDocumentContent(meta.getDocumentId());
                    addWordsFromContent(content);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Error populating Trie from documents: " + e.getMessage());
        }
    }

    /**
     * Tokenizes text and inserts words into the Trie.
     */
    public synchronized void addWordsFromContent(String content) {
        if (content == null || content.isEmpty()) return;

        // Split on non-alphanumeric boundaries
        String[] tokens = content.split("[^a-zA-Z0-9_-]+");
        for (String token : tokens) {
            String clean = token.trim().toLowerCase();
            if (clean.length() >= 2) {
                trie.insert(clean);
            }
        }
    }

    /**
     * Fetches up to limit autocomplete suggestions for the prefix.
     */
    public List<String> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }
        return trie.getAutocompleteSuggestions(prefix.trim().toLowerCase(), limit);
    }

    public int getWordCount() {
        return trie.getWordCount();
    }

    public int getNodeCount() {
        return trie.getNodeCount();
    }

    public Trie getTrie() {
        return trie;
    }
}
