package com.suffixindex.repository;

import com.suffixindex.model.SuffixIndex;
import com.suffixindex.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Manages persistence of SuffixIndex files in data/indexes/.
 * Files are saved as .index text files containing headers, Suffix Array, and LCP Array.
 */
public class IndexRepository {

    private final Path indexesDirectory;

    public IndexRepository() {
        this(FileUtils.INDEXES_DIR);
    }

    public IndexRepository(Path indexesDirectory) {
        this.indexesDirectory = Objects.requireNonNull(indexesDirectory);
        try {
            Files.createDirectories(this.indexesDirectory);
        } catch (IOException e) {
            System.err.println("Warning: Could not create indexes directory: " + e.getMessage());
        }
    }

    public Path getIndexPath(String documentId) {
        return indexesDirectory.resolve(documentId + ".index");
    }

    public boolean indexExists(String documentId) {
        return Files.exists(getIndexPath(documentId));
    }

    public boolean deleteIndexFile(String documentId) {
        return FileUtils.deleteFileSafely(getIndexPath(documentId));
    }

    /**
     * Saves SuffixIndex to a .index file using buffered streaming.
     */
    public void saveIndex(SuffixIndex index) throws IOException {
        if (index == null || index.getDocumentId() == null) {
            throw new IllegalArgumentException("Index or document ID cannot be null");
        }

        Path target = getIndexPath(index.getDocumentId());
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            // Write Metadata Header
            writer.write("SUFFIX_INDEX_VERSION=" + index.getVersion() + "\n");
            writer.write("DOCUMENT_ID=" + index.getDocumentId() + "\n");
            writer.write("TEXT_LENGTH=" + index.getTextLength() + "\n");
            writer.write("SUFFIX_COUNT=" + index.getSuffixCount() + "\n");
            writer.write("CONTENT_HASH=" + index.getContentHash() + "\n");
            writer.write("DOCUMENT_SHA256=" + (index.getSha256Hash() != null ? index.getSha256Hash() : "") + "\n");
            writer.write("CONSTRUCTION_TIME_NS=" + index.getConstructionTimeNs() + "\n");
            writer.write("CREATED_AT=" + index.getCreatedAt() + "\n\n");

            // Write Suffix Array
            writer.write("SUFFIX_ARRAY\n");
            int[] sa = index.getSuffixArray();
            if (sa != null) {
                for (int pos : sa) {
                    writer.write(Integer.toString(pos));
                    writer.write("\n");
                }
            }
            writer.write("\n");

            // Write LCP Array
            writer.write("LCP_ARRAY\n");
            int[] lcp = index.getLcpArray();
            if (lcp != null) {
                for (int val : lcp) {
                    writer.write(Integer.toString(val));
                    writer.write("\n");
                }
            }
        }
    }

    /**
     * Loads SuffixIndex from a .index file.
     */
    public SuffixIndex loadIndex(String documentId) throws IOException {
        Path target = getIndexPath(documentId);
        if (!Files.exists(target)) {
            return null;
        }

        SuffixIndex index = new SuffixIndex();
        index.setDocumentId(documentId);

        try (BufferedReader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            String line;
            int section = 0; // 0 = header, 1 = SA, 2 = LCP
            int saCount = 0;
            int lcpCount = 0;
            int[] sa = null;
            int[] lcp = null;
            int saIdx = 0;
            int lcpIdx = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("SUFFIX_ARRAY")) {
                    section = 1;
                    sa = new int[index.getSuffixCount()];
                    continue;
                } else if (line.equals("LCP_ARRAY")) {
                    section = 2;
                    lcp = new int[index.getSuffixCount()];
                    continue;
                }

                if (section == 0) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        switch (key) {
                            case "SUFFIX_INDEX_VERSION":
                                index.setVersion(Integer.parseInt(val));
                                break;
                            case "DOCUMENT_ID":
                                index.setDocumentId(val);
                                break;
                            case "TEXT_LENGTH":
                                index.setTextLength(Integer.parseInt(val));
                                break;
                            case "SUFFIX_COUNT":
                                saCount = Integer.parseInt(val);
                                index.setSuffixCount(saCount);
                                break;
                            case "CONTENT_HASH":
                                index.setContentHash(Long.parseLong(val));
                                break;
                            case "DOCUMENT_SHA256":
                                index.setSha256Hash(val);
                                break;
                            case "CONSTRUCTION_TIME_NS":
                                index.setConstructionTimeNs(Long.parseLong(val));
                                break;
                            case "CREATED_AT":
                                index.setCreatedAt(Long.parseLong(val));
                                break;
                        }
                    }
                } else if (section == 1) {
                    if (sa != null && saIdx < sa.length) {
                        sa[saIdx++] = Integer.parseInt(line);
                    }
                } else if (section == 2) {
                    if (lcp != null && lcpIdx < lcp.length) {
                        lcp[lcpIdx++] = Integer.parseInt(line);
                    }
                }
            }

            index.setSuffixArray(sa != null ? sa : new int[0]);
            index.setLcpArray(lcp != null ? lcp : new int[0]);
        }

        return index;
    }
}
