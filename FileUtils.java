package com.suffixindex.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * File I/O, path safety, and SHA-256 / CRC32 integrity utilities.
 * Pure Java File I/O (java.nio.file.*, java.security.*).
 */
public class FileUtils {

    public static final Path DATA_DIR = Paths.get("data");
    public static final Path DOCUMENTS_DIR = DATA_DIR.resolve("documents");
    public static final Path INDEXES_DIR = DATA_DIR.resolve("indexes");
    public static final Path METADATA_DIR = DATA_DIR.resolve("metadata");
    public static final Path SEARCHES_DIR = DATA_DIR.resolve("searches");
    public static final Path SETTINGS_DIR = DATA_DIR.resolve("settings");
    public static final Path BENCHMARKS_DIR = DATA_DIR.resolve("benchmarks");
    public static final Path REPORTS_DIR = DATA_DIR.resolve("reports");
    public static final Path TRIE_DIR = DATA_DIR.resolve("trie");

    /**
     * Ensures all required storage directories exist.
     */
    public static void initializeDirectories() throws IOException {
        Files.createDirectories(DOCUMENTS_DIR);
        Files.createDirectories(INDEXES_DIR);
        Files.createDirectories(METADATA_DIR);
        Files.createDirectories(SEARCHES_DIR);
        Files.createDirectories(SETTINGS_DIR);
        Files.createDirectories(BENCHMARKS_DIR);
        Files.createDirectories(REPORTS_DIR);
        Files.createDirectories(TRIE_DIR);
    }

    /**
     * Sanitizes a user-provided document name to prevent path traversal and invalid characters.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "untitled_" + System.currentTimeMillis() + ".txt";
        }
        // Extract base name only (strip path separators)
        String clean = Paths.get(filename).getFileName().toString();
        // Remove unsafe filesystem characters
        clean = clean.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (clean.isEmpty()) {
            clean = "document_" + System.currentTimeMillis();
        }
        if (!clean.toLowerCase().endsWith(".txt")) {
            clean += ".txt";
        }
        return clean;
    }

    /**
     * Reads the entire contents of a file as a UTF-8 String.
     */
    public static String readFileToString(Path path) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Writes content to a file safely using UTF-8 encoding.
     */
    public static void writeStringToFile(Path path, String content) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content != null ? content : "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * Computes a cryptographic SHA-256 hash of the content for tamper-proof index integrity checking.
     */
    public static String computeSHA256(String content) {
        if (content == null) {
            content = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in JVM", e);
        }
    }

    /**
     * Computes a 64-bit CRC32 checksum of the document content.
     */
    public static long computeContentHash(String content) {
        if (content == null || content.isEmpty()) {
            return 0L;
        }
        CRC32 crc = new CRC32();
        crc.update(content.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }

    /**
     * Safely deletes a file if it exists.
     */
    public static boolean deleteFileSafely(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
}
