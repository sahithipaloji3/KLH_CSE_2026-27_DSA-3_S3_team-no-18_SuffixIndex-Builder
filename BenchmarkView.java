package com.suffixindex.ui;

import com.suffixindex.model.BenchmarkResult;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.service.BenchmarkService;
import com.suffixindex.service.DocumentService;
import com.suffixindex.util.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Benchmark View: Runs multi-iteration empirical benchmarks and persists results
 * to data/benchmarks/ CSV and TXT log files.
 */
public class BenchmarkView extends JPanel {

    private final BenchmarkService benchmarkService;
    private final DocumentService documentService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();
    private final JTextField txtPattern = new JTextField();
    private final JComboBox<Integer> comboRuns = new JComboBox<>(new Integer[]{5, 10, 20, 50});
    private final ModernButton btnRunBenchmark = UITheme.createPrimaryButton("EXECUTE BENCHMARK",
            new ModernIcon(ModernIcon.IconType.BENCHMARK, 16, Color.WHITE));

    // Telemetry Cards
    private final JLabel lblSaBuildAvg = new JLabel("-");
    private final JLabel lblLcpBuildAvg = new JLabel("-");
    private final JLabel lblNaiveSearchAvg = new JLabel("-");
    private final JLabel lblSaSearchAvg = new JLabel("-");
    private final JLabel lblSpeedup = new JLabel("-");

    // History Table
    private final DefaultTableModel logTableModel = new DefaultTableModel(
            new String[]{"Timestamp", "Document", "Size (Chars)", "Pattern", "Runs", "SA Build (Avg)", "LCP Build (Avg)", "Naive Search", "SA Search", "Speedup"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable logTable = new JTable(logTableModel);

    private static class DocumentItem {
        final DocumentMetadata meta;

        DocumentItem(DocumentMetadata meta) {
            this.meta = meta;
        }

        @Override
        public String toString() {
            return meta.getDocumentName() + " (" + meta.getDocumentId() + ") - " + meta.getCharacterCount() + " chars";
        }
    }

    public BenchmarkView(BenchmarkService benchmarkService, DocumentService documentService, MainWindow mainWindow) {
        this.benchmarkService = benchmarkService;
        this.documentService = documentService;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
        loadBenchmarkHistory();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("System Benchmark Suite");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Multi-trial algorithmic profiling with automated CSV and TXT audit logging to data/benchmarks/");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);
        header.add(titleGroup, BorderLayout.WEST);

        // Control Card
        ModernCard controlCard = UITheme.createCardPanel();
        controlCard.setLayout(new BorderLayout(0, 12));

        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 10));
        grid.setOpaque(false);

        // Row 1: Doc & Runs
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JLabel lblDoc = new JLabel("Target Document:");
        lblDoc.setFont(UITheme.FONT_SUBHEADER);
        comboDocuments.setFont(UITheme.FONT_BODY);

        JPanel runsGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        runsGroup.setOpaque(false);
        JLabel lblRuns = new JLabel("Iterations / Runs:");
        lblRuns.setFont(UITheme.FONT_BODY);
        comboRuns.setFont(UITheme.FONT_BODY);
        comboRuns.setSelectedItem(10);
        runsGroup.add(lblRuns);
        runsGroup.add(comboRuns);

        row1.add(lblDoc, BorderLayout.WEST);
        row1.add(comboDocuments, BorderLayout.CENTER);
        row1.add(runsGroup, BorderLayout.EAST);

        // Row 2: Pattern & Run Button
        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        row2.setOpaque(false);

        JLabel lblPat = new JLabel("Search Pattern:");
        lblPat.setFont(UITheme.FONT_SUBHEADER);
        txtPattern.setFont(UITheme.FONT_MONO);

        btnRunBenchmark.setPreferredSize(new Dimension(190, 38));
        btnRunBenchmark.addActionListener(e -> runBenchmarkTrials());

        row2.add(lblPat, BorderLayout.WEST);
        row2.add(txtPattern, BorderLayout.CENTER);
        row2.add(btnRunBenchmark, BorderLayout.EAST);

        grid.add(row1);
        grid.add(row2);
        controlCard.add(grid, BorderLayout.CENTER);

        // Top Section
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(controlCard, BorderLayout.SOUTH);

        // Telemetry cards grid (5 boxes)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricBox("SA Construction (Avg)", lblSaBuildAvg, UITheme.PRIMARY));
        metricsGrid.add(createMetricBox("Kasai LCP (Avg)", lblLcpBuildAvg, UITheme.SUCCESS));
        metricsGrid.add(createMetricBox("Naive Search (Avg)", lblNaiveSearchAvg, new Color(239, 68, 68)));
        metricsGrid.add(createMetricBox("Suffix Search (Avg)", lblSaSearchAvg, new Color(16, 185, 129)));
        metricsGrid.add(createMetricBox("Speedup Factor", lblSpeedup, new Color(139, 92, 246)));

        // Table Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("Historical Benchmark Logs (from data/benchmarks/*.csv)");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        ModernButton btnRefreshHistory = UITheme.createSecondaryButton("Refresh Logs",
                new ModernIcon(ModernIcon.IconType.REFRESH, 14, UITheme.TEXT_SECONDARY));
        btnRefreshHistory.addActionListener(e -> loadBenchmarkHistory());

        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(btnRefreshHistory, BorderLayout.EAST);

        UITheme.styleTable(logTable);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(2).setMaxWidth(90);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(4).setMaxWidth(60);

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);
        centerPanel.add(metricsGrid, BorderLayout.NORTH);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricBox(String title, JLabel valLabel, Color color) {
        ModernCard p = UITheme.createCardPanel();
        p.setLayout(new BorderLayout(0, 4));

        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_SECONDARY);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valLabel.setForeground(color);

        p.add(t, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    public void loadDocuments() {
        try {
            comboDocuments.removeAllItems();
            List<DocumentMetadata> docs = documentService.getAllDocuments();
            for (DocumentMetadata doc : docs) {
                comboDocuments.addItem(new DocumentItem(doc));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading document list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runBenchmarkTrials() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a document.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String pattern = txtPattern.getText();
        if (pattern == null || pattern.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search pattern.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int runs = (Integer) comboRuns.getSelectedItem();

        try {
            BenchmarkResult result = benchmarkService.runBenchmark(item.meta.getDocumentId(), pattern, runs);

            lblSaBuildAvg.setText(String.format("%.3f ms", result.getSaBuildAvgMs()));
            lblLcpBuildAvg.setText(String.format("%.3f ms", result.getLcpBuildAvgMs()));
            lblNaiveSearchAvg.setText(String.format("%.3f ms", result.getNaiveSearchAvgMs()));
            lblSaSearchAvg.setText(String.format("%.3f ms", result.getSaSearchAvgMs()));
            lblSpeedup.setText(String.format("%.1fx", result.getSpeedupFactor()));

            loadBenchmarkHistory();
            mainWindow.showNotification(String.format("Benchmark completed (%d runs). Suffix Array is %.1fx faster. Saved to CSV.",
                    runs, result.getSpeedupFactor()));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Benchmark error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBenchmarkHistory() {
        logTableModel.setRowCount(0);
        try {
            List<Path> files = benchmarkService.listBenchmarkFiles();
            for (Path file : files) {
                if (file.getFileName().toString().endsWith(".csv")) {
                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                        String line = reader.readLine(); // Header
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length >= 11) {
                                logTableModel.addRow(new Object[]{
                                        parts[0], parts[1], parts[2], parts[3], parts[4],
                                        parts[5] + " ms", parts[6] + " ms", parts[7] + " ms", parts[8] + " ms", parts[10]
                                });
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            System.err.println("Could not list benchmark files: " + e.getMessage());
        }
    }
}
