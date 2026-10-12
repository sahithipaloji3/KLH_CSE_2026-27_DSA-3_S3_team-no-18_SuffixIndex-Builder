package com.suffixindex;

import com.suffixindex.repository.*;
import com.suffixindex.service.*;
import com.suffixindex.ui.MainWindow;
import com.suffixindex.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Main application launcher for Suffix Index Builder.
 * A 100% pure Java native desktop application with modern Java 2D UI rendering.
 */
public class Main {

    public static void main(String[] args) {
        // Enable high-DPI font antialiasing across all Swing components
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global Swing defaults for consistent modern feel
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        // Initialize file directories
        try {
            FileUtils.initializeDirectories();
        } catch (IOException e) {
            System.err.println("Fatal: Could not initialize file storage directories: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Instantiate Repositories (Pure Java File I/O)
        DocumentRepository documentRepository = new DocumentRepository();
        MetadataRepository metadataRepository = new MetadataRepository();
        IndexRepository indexRepository = new IndexRepository();
        SearchHistoryRepository searchHistoryRepository = new SearchHistoryRepository();
        SettingsRepository settingsRepository = new SettingsRepository();
        BenchmarkRepository benchmarkRepository = new BenchmarkRepository();
        ReportRepository reportRepository = new ReportRepository();

        // Instantiate Services
        DocumentService documentService = new DocumentService(documentRepository, metadataRepository, indexRepository);
        IndexService indexService = new IndexService(documentRepository, metadataRepository, indexRepository);
        SearchService searchService = new SearchService(documentRepository, metadataRepository, indexRepository, searchHistoryRepository);
        AnalyticsService analyticsService = new AnalyticsService(documentRepository, metadataRepository, indexRepository, searchHistoryRepository);
        MultiDocumentSearchService multiSearchService = new MultiDocumentSearchService(documentService, indexService, searchService);
        AutocompleteService autocompleteService = new AutocompleteService(documentService, documentRepository);
        ReportService reportService = new ReportService(reportRepository, documentService, indexService, analyticsService);
        BenchmarkService benchmarkService = new BenchmarkService(benchmarkRepository, documentService);

        // Launch Desktop UI on Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow(
                        documentService,
                        indexService,
                        searchService,
                        searchHistoryRepository,
                        settingsRepository,
                        multiSearchService,
                        autocompleteService,
                        reportService,
                        benchmarkService
                );
                window.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error starting Suffix Index Builder UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
