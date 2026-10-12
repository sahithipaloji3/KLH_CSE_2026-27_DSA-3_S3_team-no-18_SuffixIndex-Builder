package com.suffixindex.ui;

import com.suffixindex.algorithm.NaiveSearcher;
import com.suffixindex.algorithm.SuffixSearcher;
import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.IndexService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.List;

/**
 * Dedicated Algorithm Comparison View:
 * Evaluates empirical speedup and comparison counts between Naive Brute Force Search
 * and Suffix Array Binary Search with Java 2D visual comparison bars.
 */
public class AlgorithmComparisonView extends JPanel {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();
    private final JTextField txtPattern = new JTextField();
    private final JComboBox<Integer> comboRuns = new JComboBox<>(new Integer[]{1, 5, 10, 25, 50, 100});
    private final ModernButton btnRunComparison = UITheme.createPrimaryButton("RUN BENCHMARK",
            new ModernIcon(ModernIcon.IconType.ALGORITHM, 16, Color.WHITE));

    // Summary Metrics
    private final JPanel resultsPanel = new JPanel(new BorderLayout(0, 16));
    private final JLabel lblSpeedup = new JLabel("-");
    private final JLabel lblSaTime = new JLabel("-");
    private final JLabel lblNaiveTime = new JLabel("-");
    private final JLabel lblMatches = new JLabel("-");

    // Breakdown Table
    private final DefaultTableModel comparisonTableModel = new DefaultTableModel(
            new String[]{"Metric / Dimension", "Naive Brute Force Search", "Suffix Array Binary Search", "DSA Theoretical Advantage"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable comparisonTable = new JTable(comparisonTableModel);

    // Custom Java 2D Comparison Bar Chart
    private final ComparisonBarPanel chartPanel = new ComparisonBarPanel();

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

    public AlgorithmComparisonView(DocumentService documentService, IndexService indexService, MainWindow mainWindow) {
        this.documentService = documentService;
        this.indexService = indexService;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Algorithm Performance Benchmark & Comparison");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Head-to-head empirical testing: Naive Brute Force O(n*m) vs Suffix Array Binary Search O(m log n)");
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

        // Row 1: Document & Runs
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JLabel lblDoc = new JLabel("Select Document:");
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

        btnRunComparison.setPreferredSize(new Dimension(170, 38));
        btnRunComparison.addActionListener(e -> runComparison());

        row2.add(lblPat, BorderLayout.WEST);
        row2.add(txtPattern, BorderLayout.CENTER);
        row2.add(btnRunComparison, BorderLayout.EAST);

        grid.add(row1);
        grid.add(row2);
        controlCard.add(grid, BorderLayout.CENTER);

        // Top Section
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(controlCard, BorderLayout.SOUTH);

        // Results Section
        resultsPanel.setOpaque(false);
        resultsPanel.setVisible(false);

        // 4 Telemetry Metrics
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricBox("Speedup Factor", lblSpeedup, new Color(139, 92, 246)));
        metricsGrid.add(createMetricBox("Suffix Array Search (Avg)", lblSaTime, UITheme.SUCCESS));
        metricsGrid.add(createMetricBox("Naive Search (Avg)", lblNaiveTime, new Color(239, 68, 68)));
        metricsGrid.add(createMetricBox("Total Matches Found", lblMatches, UITheme.PRIMARY));

        // Center Split: Chart on top, Table below
        JPanel centerContainer = new JPanel(new BorderLayout(0, 16));
        centerContainer.setOpaque(false);

        chartPanel.setPreferredSize(new Dimension(800, 110));

        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel tableTitle = new JLabel("Comparative Metric Breakdown");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        UITheme.styleTable(comparisonTable);
        comparisonTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        comparisonTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        comparisonTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        comparisonTable.getColumnModel().getColumn(3).setPreferredWidth(220);

        JScrollPane tableScroll = new JScrollPane(comparisonTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(tableScroll, BorderLayout.CENTER);

        centerContainer.add(chartPanel, BorderLayout.NORTH);
        centerContainer.add(tableCard, BorderLayout.CENTER);

        resultsPanel.add(metricsGrid, BorderLayout.NORTH);
        resultsPanel.add(centerContainer, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricBox(String title, JLabel valLabel, Color color) {
        ModernCard p = UITheme.createCardPanel();
        p.setLayout(new BorderLayout(0, 4));

        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_SECONDARY);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
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

    private void runComparison() {
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
            Document doc = documentService.getDocument(item.meta.getDocumentId());
            SuffixIndex index = indexService.loadIndex(item.meta.getDocumentId());

            if (index == null || !index.isValid()) {
                index = indexService.buildIndex(item.meta.getDocumentId());
            }

            String text = doc.getContent();
            int[] sa = index.getSuffixArray();

            // Warmup
            SuffixSearcher.search(text, pattern, sa, true);
            NaiveSearcher.search(text, pattern, true);

            long saTotalNs = 0;
            long naiveTotalNs = 0;
            long saComparisons = 0;
            long naiveComparisons = 0;
            int matches = 0;

            for (int i = 0; i < runs; i++) {
                SuffixSearcher.SearchOutcome saOutcome = SuffixSearcher.search(text, pattern, sa, true);
                saTotalNs += saOutcome.getExecutionTimeNs();
                saComparisons = saOutcome.getComparisonCount();
                matches = saOutcome.getMatchCount();

                NaiveSearcher.NaiveSearchOutcome naiveOutcome = NaiveSearcher.search(text, pattern, true);
                naiveTotalNs += naiveOutcome.getExecutionTimeNs();
                naiveComparisons = naiveOutcome.getComparisonCount();
            }

            long saAvgNs = saTotalNs / runs;
            long naiveAvgNs = naiveTotalNs / runs;
            double speedup = saAvgNs > 0 ? (double) naiveAvgNs / saAvgNs : 1.0;

            lblSpeedup.setText(String.format("%.1fx Faster", speedup));
            lblSaTime.setText(String.format("%.3f ms (%,d ns)", saAvgNs / 1_000_000.0, saAvgNs));
            lblNaiveTime.setText(String.format("%.3f ms (%,d ns)", naiveAvgNs / 1_000_000.0, naiveAvgNs));
            lblMatches.setText(String.format("%,d matches", matches));

            // Update Breakdown Table
            comparisonTableModel.setRowCount(0);
            comparisonTableModel.addRow(new Object[]{
                    "Theoretical Time Complexity",
                    "O(n * m) worst case",
                    "O(m * log n) with Binary Search",
                    "Exponentially scales better with large text"
            });
            comparisonTableModel.addRow(new Object[]{
                    "Empirical Search Time (Avg)",
                    String.format("%.3f ms (%,d ns)", naiveAvgNs / 1_000_000.0, naiveAvgNs),
                    String.format("%.3f ms (%,d ns)", saAvgNs / 1_000_000.0, saAvgNs),
                    String.format("%.1fx faster retrieval speed", speedup)
            });
            comparisonTableModel.addRow(new Object[]{
                    "Character Comparisons",
                    String.format("%,d character checks", naiveComparisons),
                    String.format("%,d character checks", saComparisons),
                    String.format("%,d fewer checks (%.1f%% reduction)",
                            Math.max(0, naiveComparisons - saComparisons),
                            naiveComparisons > 0 ? (double)(naiveComparisons - saComparisons)/naiveComparisons * 100.0 : 0)
            });
            comparisonTableModel.addRow(new Object[]{
                    "Precomputation Index Cost",
                    "0 ms (Linear scan each search)",
                    String.format("%.3f ms (One-time cost)", index.getConstructionTimeMs()),
                    "Amortized over multiple recurring queries"
            });

            // Update visual chart
            chartPanel.setMetrics(naiveAvgNs, saAvgNs);

            resultsPanel.setVisible(true);
            mainWindow.showNotification(String.format("Comparison complete: Suffix Array is %.1fx faster than Naive search.", speedup));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Benchmark error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Pure Java 2D Comparison Bar Chart
     */
    private static class ComparisonBarPanel extends JPanel {
        private long naiveNs = 0;
        private long saNs = 0;

        public ComparisonBarPanel() {
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                    new EmptyBorder(12, 16, 12, 16)
            ));
        }

        public void setMetrics(long naiveNs, long saNs) {
            this.naiveNs = Math.max(1, naiveNs);
            this.saNs = Math.max(1, saNs);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 32;
            int startX = 180;
            int maxBarW = Math.max(50, w - startX - 20);

            long maxVal = Math.max(naiveNs, saNs);
            int naiveW = (int) (((double) naiveNs / maxVal) * maxBarW);
            int saW = Math.max(6, (int) (((double) saNs / maxVal) * maxBarW));

            g2.setFont(UITheme.FONT_BODY_BOLD);

            // Bar 1: Naive Search
            g2.setColor(UITheme.TEXT_PRIMARY);
            g2.drawString("Naive Brute Force:", 16, 32);
            g2.setColor(new Color(239, 68, 68));
            g2.fill(new RoundRectangle2D.Float(startX, 18, naiveW, 20, 6, 6));
            g2.drawString(String.format("%,d ns", naiveNs), startX + naiveW + 10, 34);

            // Bar 2: Suffix Array Search
            g2.setColor(UITheme.TEXT_PRIMARY);
            g2.drawString("Suffix Array Search:", 16, 72);
            g2.setColor(UITheme.SUCCESS);
            g2.fill(new RoundRectangle2D.Float(startX, 58, saW, 20, 6, 6));
            g2.drawString(String.format("%,d ns", saNs), startX + saW + 10, 74);

            g2.dispose();
        }
    }
}
