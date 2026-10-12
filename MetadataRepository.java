package com.suffixindex.repository;

import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages persistence of DocumentMetadata in data/metadata/documents.meta.
 * 100% Java File I/O based (No database).
 */
public class MetadataRepository {

    private final Path metadataFile;

    public MetadataRepository() {
        this(FileUtils.METADATA_DIR.resolve("documents.meta"));
    }

    public MetadataRepository(Path metadataFile) {
        this.metadataFile = Objects.requireNonNull(metadataFile);
        try {
            if (this.metadataFile.getParent() != null) {
                Files.createDirectories(this.metadataFile.getParent());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create metadata directory: " + e.getMessage());
        }
    }

    /**
     * Loads all metadata records from the metadata file.
     */
    public synchronized Map<String, DocumentMetadata> loadAll() throws IOException {
        Map<String, DocumentMetadata> map = new LinkedHashMap<>();
        if (!Files.exists(metadataFile)) {
            return map;
        }

        try (BufferedReader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {
            String line;
            DocumentMetadata current = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("BEGIN_DOCUMENT")) {
                    current = new DocumentMetadata();
                } else if (line.equals("END_DOCUMENT")) {
                    if (current != null && current.getDocumentId() != null) {
                        map.put(current.getDocumentId(), current);
                    }
                    current = null;
                } else if (current != null) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        parseField(current, key, val);
                    }
                }
            }
        }
        return map;
    }

    private void parseField(DocumentMetadata meta, String key, String val) {
        switch (key) {
            case "id":
                meta.setDocumentId(val);
                break;
            case "name":
                meta.setDocumentName(val);
                break;
            case "path":
                meta.setFilePath(val);
                break;
            case "chars":
                try {
                    meta.setCharacterCount(Long.parseLong(val));
                } catch (NumberFormatException ignored) {}
                break;
            case "status":
                meta.setIndexStatus(IndexStatus.fromString(val));
                break;
            case "modified":
                try {
                    meta.setLastModified(Long.parseLong(val));
                } catch (NumberFormatException ignored) {}
                break;
            case "indexed":
                try {
                    meta.setLastIndexed(Long.parseLong(val));
                } catch (NumberFormatException ignored) {}
                break;
            case "indexFile":
                meta.setIndexFileName(val);
                break;
            case "hash":
                try {
                    meta.setContentHash(Long.parseLong(val));
                } catch (NumberFormatException ignored) {}
                break;
            case "sha256":
                meta.setSha256Hash(val);
                break;
        }
    }

    /**
     * Saves all metadata records to the metadata file using an atomic replace or direct write.
     */
    public synchronized void saveAll(Collection<DocumentMetadata> documents) throws IOException {
        if (metadataFile.getParent() != null) {
            Files.createDirectories(metadataFile.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(metadataFile, StandardCharsets.UTF_8)) {
            writer.write("# Suffix Index Builder - Document Metadata Storage\n");
            writer.write("# Version: 1.0\n");
            writer.write("# Format: Block-based key-value pairs\n\n");

            for (DocumentMetadata doc : documents) {
                writer.write("BEGIN_DOCUMENT\n");
                writer.write("id=" + doc.getDocumentId() + "\n");
                writer.write("name=" + (doc.getDocumentName() != null ? doc.getDocumentName() : "") + "\n");
                writer.write("path=" + (doc.getFilePath() != null ? doc.getFilePath() : "") + "\n");
                writer.write("chars=" + doc.getCharacterCount() + "\n");
                writer.write("status=" + doc.getIndexStatus().name() + "\n");
                writer.write("modified=" + doc.getLastModified() + "\n");
                writer.write("indexed=" + doc.getLastIndexed() + "\n");
                writer.write("indexFile=" + (doc.getIndexFileName() != null ? doc.getIndexFileName() : "") + "\n");
                writer.write("hash=" + doc.getContentHash() + "\n");
                writer.write("sha256=" + (doc.getSha256Hash() != null ? doc.getSha256Hash() : "") + "\n");
                writer.write("END_DOCUMENT\n\n");
            }
        }
    }
}
