package com.suffixindex.repository;

import com.suffixindex.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Handles raw document text file persistence in data/documents/.
 * Operates purely via Java File I/O.
 */
public class DocumentRepository {

    private final Path documentsDirectory;

    public DocumentRepository() {
        this(FileUtils.DOCUMENTS_DIR);
    }

    public DocumentRepository(Path documentsDirectory) {
        this.documentsDirectory = Objects.requireNonNull(documentsDirectory);
        try {
            Files.createDirectories(this.documentsDirectory);
        } catch (IOException e) {
            System.err.println("Warning: Could not create documents directory: " + e.getMessage());
        }
    }

    public Path getDocumentPath(String documentId) {
        return documentsDirectory.resolve(documentId + ".txt");
    }

    public void saveDocumentContent(String documentId, String content) throws IOException {
        Path target = getDocumentPath(documentId);
        FileUtils.writeStringToFile(target, content);
    }

    public String readDocumentContent(String documentId) throws IOException {
        Path target = getDocumentPath(documentId);
        if (!Files.exists(target)) {
            return "";
        }
        return FileUtils.readFileToString(target);
    }

    public boolean deleteDocumentFile(String documentId) {
        Path target = getDocumentPath(documentId);
        return FileUtils.deleteFileSafely(target);
    }

    public boolean documentFileExists(String documentId) {
        return Files.exists(getDocumentPath(documentId));
    }

    public long getFileLastModified(String documentId) {
        Path target = getDocumentPath(documentId);
        try {
            if (Files.exists(target)) {
                return Files.getLastModifiedTime(target).toMillis();
            }
        } catch (IOException ignored) {}
        return 0L;
    }
}
