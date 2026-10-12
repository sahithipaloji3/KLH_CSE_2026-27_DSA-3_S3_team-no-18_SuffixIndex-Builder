package com.suffixindex.repository;

import com.suffixindex.model.SearchHistoryEntry;
import com.suffixindex.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages search history in data/searches/search_history.txt.
 * Pure Java File I/O.
 */
public class SearchHistoryRepository {

    private final Path historyFile;

    public SearchHistoryRepository() {
        this(FileUtils.SEARCHES_DIR.resolve("search_history.txt"));
    }

    public SearchHistoryRepository(Path historyFile) {
        this.historyFile = historyFile;
        try {
            if (this.historyFile.getParent() != null) {
                Files.createDirectories(this.historyFile.getParent());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create search history directory: " + e.getMessage());
        }
    }

    /**
     * Appends a new search entry to search_history.txt.
     */
    public synchronized void appendEntry(SearchHistoryEntry entry) {
        if (entry == null) return;
        try {
            if (!Files.exists(historyFile)) {
                Files.createFile(historyFile);
            }
            String line = entry.toFileString() + System.lineSeparator();
            Files.writeString(historyFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error saving search history entry: " + e.getMessage());
        }
    }

    /**
     * Loads all search history entries in reverse chronological order (newest first).
     */
    public synchronized List<SearchHistoryEntry> loadAll() {
        List<SearchHistoryEntry> list = new ArrayList<>();
        if (!Files.exists(historyFile)) {
            return list;
        }

        try (BufferedReader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    SearchHistoryEntry entry = SearchHistoryEntry.fromFileString(line);
                    if (entry != null) {
                        list.add(entry);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading search history: " + e.getMessage());
        }

        Collections.reverse(list);
        return list;
    }

    /**
     * Clears all search history.
     */
    public synchronized void clearHistory() {
        try {
            if (Files.exists(historyFile)) {
                Files.writeString(historyFile, "", StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error clearing search history: " + e.getMessage());
        }
    }

    public synchronized int getCount() {
        if (!Files.exists(historyFile)) return 0;
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) {
                count++;
            }
        } catch (IOException ignored) {}
        return count;
    }
}
