package com.suffixindex.repository;

import com.suffixindex.model.AppSettings;
import com.suffixindex.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages configuration settings in data/settings/application.conf.
 * Pure Java File I/O.
 */
public class SettingsRepository {

    private final Path settingsFile;

    public SettingsRepository() {
        this(FileUtils.SETTINGS_DIR.resolve("application.conf"));
    }

    public SettingsRepository(Path settingsFile) {
        this.settingsFile = settingsFile;
        try {
            if (this.settingsFile.getParent() != null) {
                Files.createDirectories(this.settingsFile.getParent());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create settings directory: " + e.getMessage());
        }
    }

    public synchronized AppSettings loadSettings() {
        AppSettings settings = new AppSettings();
        if (!Files.exists(settingsFile)) {
            saveSettings(settings); // write defaults
            return settings;
        }

        try (BufferedReader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    switch (key) {
                        case "pageSize":
                            try {
                                settings.setPageSize(Integer.parseInt(val));
                            } catch (NumberFormatException ignored) {}
                            break;
                        case "caseSensitive":
                            settings.setCaseSensitive(Boolean.parseBoolean(val));
                            break;
                        case "previewContextLength":
                            try {
                                settings.setPreviewContextLength(Integer.parseInt(val));
                            } catch (NumberFormatException ignored) {}
                            break;
                        case "theme":
                            settings.setTheme(val);
                            break;
                        case "autoRebuildOutdated":
                            settings.setAutoRebuildOutdated(Boolean.parseBoolean(val));
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading settings: " + e.getMessage());
        }

        return settings;
    }

    public synchronized void saveSettings(AppSettings settings) {
        if (settings == null) return;
        try {
            if (settingsFile.getParent() != null) {
                Files.createDirectories(settingsFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(settingsFile, StandardCharsets.UTF_8)) {
                writer.write("# Suffix Index Builder Configuration\n");
                writer.write("pageSize=" + settings.getPageSize() + "\n");
                writer.write("caseSensitive=" + settings.isCaseSensitive() + "\n");
                writer.write("previewContextLength=" + settings.getPreviewContextLength() + "\n");
                writer.write("theme=" + settings.getTheme() + "\n");
                writer.write("autoRebuildOutdated=" + settings.isAutoRebuildOutdated() + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
}
