package com.suffixindex.ui;

import com.suffixindex.algorithm.LCPArrayBuilder;
import com.suffixindex.algorithm.NaiveSearcher;
import com.suffixindex.algorithm.SuffixArrayBuilder;
import com.suffixindex.algorithm.SuffixSearcher;
import com.suffixindex.util.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Statistics & Algorithmic Performance View: analyzes disk utilization and provides a live DSA benchmark runner.
 */
public class StatisticsView extends JPanel {

    private final MainWindow mainWindow;

    // Storage metric labels
    private final JLabel lblDocsStorage = new JLabel("-");
    private final JLabel lblIndexesStorage = new JLabel("-");
    private final JLabel lblMetaStorage = new JLabel("-");
    private final JLabel lblSearchesStorage = new JLabel("-");

    // Live benchmark table
    private final DefaultTableModel benchModel = new DefaultTableModel(
            new String[]{"Text Size", "Suffix Array Build", "Kasai LCP Build", "Binary Search (O(m log n))", "Naive Search (O(nm))", "Speedup"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable benchTable = new JTable(benchModel);
    private final ModernButton btnRunBench = UITheme.createPrimaryButton("Run Live Benchmark",
            new ModernIcon(ModernIcon.IconType.PLAY, 16, Color.WHITE));
    private final JProgressBar benchProgress = new JProgressBar();

    public StatisticsView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Performance & Storage Analytics");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Inspect file storage footprints and evaluate empirical DSA performance");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        ModernButton btnRefresh = UITheme.createSecondaryButton("Refresh Disk Stats",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnRefresh.addActionListener(e -> refreshData());

        header.add(titleGroup, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);

        // Disk Storage Metrics Grid
        JPanel diskGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        diskGrid.setOpaque(false);
        diskGrid.add(createDiskCard("Documents Storage", lblDocsStorage, new Color(59, 130, 246)));
        diskGrid.add(createDiskCard("Compiled Indexes", lblIndexesStorage, UITheme.SUCCESS));
        diskGrid.add(createDiskCard("Metadata File", lblMetaStorage, UITheme.PRIMARY));
        diskGrid.add(createDiskCard("Search History", lblSearchesStorage, new Color(236, 72, 153)));

        // Center Content: Complexity Table + Live Benchmark
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 16));
        centerPanel.setOpaque(false);

        // Card 1: Theoretical Complexity Matrix
        ModernCard matrixCard = UITheme.createCardPanel();
        matrixCard.setLayout(new BorderLayout(0, 10));

        JLabel matrixTitle = new JLabel("Algorithmic Complexity Matrix");
        matrixTitle.setFont(UITheme.FONT_SUBHEADER);
        matrixTitle.setForeground(UITheme.TEXT_PRIMARY);

        DefaultTableModel matrixModel = new DefaultTableModel(
                new String[]{"Algorithm", "Approach", "Time Complexity", "Space Complexity", "Purpose"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        matrixModel.addRow(new Object[]{"Suffix Array", "Prefix Doubling (k=1,2,4...)", "O(n log² n)", "O(n)", "Full text suffix sorting"});
        matrixModel.addRow(new Object[]{"LCP Array", "Kasai's Algorithm", "O(n) Linear", "O(n)", "Adjacent suffix common prefix lengths"});
        matrixModel.addRow(new Object[]{"Substring Search", "Binary Search on SA", "O(m · log n)", "O(1) Auxiliary", "Sub-millisecond pattern matching"});
        matrixModel.addRow(new Object[]{"Brute Force Search", "Naive Window Scan", "O((n-m+1) · m)", "O(1) Auxiliary", "Direct performance comparison"});

        JTable matrixTable = new JTable(matrixModel);
        UITheme.styleTable(matrixTable);
        JScrollPane matrixScroll = new JScrollPane(matrixTable);
        matrixScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        matrixCard.add(matrixTitle, BorderLayout.NORTH);
        matrixCard.add(matrixScroll, BorderLayout.CENTER);

        // Card 2: Live Benchmark Runner
        ModernCard benchCard = UITheme.createCardPanel();
        benchCard.setLayout(new BorderLayout(0, 10));

        JPanel benchTop = new JPanel(new BorderLayout());
        benchTop.setOpaque(false);

        JLabel benchTitle = new JLabel("Empirical Live Benchmark (Real nanoTime Measurements)");
        benchTitle.setFont(UITheme.FONT_SUBHEADER);
        benchTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel benchControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        benchControls.setOpaque(false);

        benchProgress.setIndeterminate(true);
        benchProgress.setVisible(false);
        benchProgress.setPreferredSize(new Dimension(140, 24));

        btnRunBench.addActionListener(e -> executeLiveBenchmark());

        benchControls.add(benchProgress);
        benchControls.add(btnRunBench);

        benchTop.add(benchTitle, BorderLayout.WEST);
        benchTop.add(benchControls, BorderLayout.EAST);

        UITheme.styleTable(benchTable);
        JScrollPane benchScroll = new JScrollPane(benchTable);
        benchScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        benchCard.add(benchTop, BorderLayout.NORTH);
        benchCard.add(benchScroll, BorderLayout.CENTER);

        centerPanel.add(matrixCard);
        centerPanel.add(benchCard);

        // Top assembly
        JPanel topGroup = new JPanel(new BorderLayout(0, 12));
        topGroup.setOpaque(false);
        topGroup.add(header, BorderLayout.NORTH);
        topGroup.add(diskGrid, BorderLayout.SOUTH);

        add(topGroup, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createDiskCard(String title, JLabel valLabel, Color accent) {
        ModernCard card = UITheme.createCardPanelWithAccent(accent);
        card.setLayout(new BorderLayout(0, 4));

        JLabel l = new JLabel(title);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valLabel.setForeground(accent);

        card.add(l, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        try {
            long docBytes = getDirSize(FileUtils.DOCUMENTS_DIR);
            int docFiles = getFileCount(FileUtils.DOCUMENTS_DIR);
            lblDocsStorage.setText(String.format("%,d bytes (%d files)", docBytes, docFiles));

            long idxBytes = getDirSize(FileUtils.INDEXES_DIR);
            int idxFiles = getFileCount(FileUtils.INDEXES_DIR);
            lblIndexesStorage.setText(String.format("%,d bytes (%d files)", idxBytes, idxFiles));

            Path metaPath = FileUtils.METADATA_DIR.resolve("documents.meta");
            long metaBytes = Files.exists(metaPath) ? Files.size(metaPath) : 0;
            lblMetaStorage.setText(String.format("%,d bytes", metaBytes));

            Path searchPath = FileUtils.SEARCHES_DIR.resolve("search_history.txt");
            long searchBytes = Files.exists(searchPath) ? Files.size(searchPath) : 0;
            lblSearchesStorage.setText(String.format("%,d bytes", searchBytes));

        } catch (IOException e) {
            System.err.println("Error reading storage stats: " + e.getMessage());
        }
    }

    private long getDirSize(Path dir) throws IOException {
        if (!Files.exists(dir)) return 0;
        try (var stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (Exception e) { return 0; }
                    }).sum();
        }
    }

    private int getFileCount(Path dir) throws IOException {
        if (!Files.exists(dir)) return 0;
        try (var stream = Files.list(dir)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    private void executeLiveBenchmark() {
        btnRunBench.setEnabled(false);
        benchProgress.setVisible(true);
        benchModel.setRowCount(0);

        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int[] sizes = {1024, 10240, 51200}; // 1 KB, 10 KB, 50 KB
                String[] labels = {"1 KB", "10 KB", "50 KB"};
                String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 \n";
                Random rng = new Random(42);

                for (int i = 0; i < sizes.length; i++) {
                    int size = sizes[i];
                    StringBuilder sb = new StringBuilder(size);
                    for (int c = 0; c < size; c++) {
                        sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
                    }
                    String text = sb.toString();
                    String pattern = text.substring(size / 2, Math.min(size, size / 2 + 6));

                    // Benchmark Prefix Doubling Suffix Array
                    long t0 = System.nanoTime();
                    int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
                    long saTimeNs = System.nanoTime() - t0;

                    // Benchmark Kasai LCP
                    long t1 = System.nanoTime();
                    int[] lcp = LCPArrayBuilder.buildLCPArray(text, sa);
                    long lcpTimeNs = System.nanoTime() - t1;

                    // Benchmark Binary Search on SA
                    long t2 = System.nanoTime();
                    var saOutcome = SuffixSearcher.search(text, pattern, sa, true);
                    long binSearchNs = System.nanoTime() - t2;

                    // Benchmark Naive Search
                    long t3 = System.nanoTime();
                    var naiveOutcome = NaiveSearcher.search(text, pattern, true);
                    long naiveSearchNs = System.nanoTime() - t3;

                    double speedup = (binSearchNs > 0) ? (double) naiveSearchNs / binSearchNs : 1.0;

                    Object[] row = new Object[]{
                            labels[i] + " (" + String.format("%,d", size) + " chars)",
                            String.format("%.3f ms", saTimeNs / 1_000_000.0),
                            String.format("%.3f ms", lcpTimeNs / 1_000_000.0),
                            String.format("%.3f ms (%,d ns)", binSearchNs / 1_000_000.0, binSearchNs),
                            String.format("%.3f ms (%,d ns)", naiveSearchNs / 1_000_000.0, naiveSearchNs),
                            String.format("%.1fx faster", speedup)
                    };
                    publish(row);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] r : chunks) {
                    benchModel.addRow(r);
                }
            }

            @Override
            protected void done() {
                btnRunBench.setEnabled(true);
                benchProgress.setVisible(false);
                mainWindow.showNotification("Live benchmark completed successfully.");
            }
        };

        worker.execute();
    }
}
