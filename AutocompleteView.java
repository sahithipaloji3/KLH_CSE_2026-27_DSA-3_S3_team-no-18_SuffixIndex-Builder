package com.suffixindex.ui;

import com.suffixindex.service.AutocompleteService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Autocomplete View: Interactive Trie-based live word completion interface.
 * Updates suggestions in real time as the user types prefixes.
 */
public class AutocompleteView extends JPanel {

    private final AutocompleteService autocompleteService;
    private final MainWindow mainWindow;

    private final JTextField txtPrefix = new JTextField();
    private final JSpinner spinLimit = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
    private final ModernButton btnRebuildTrie = UITheme.createSecondaryButton("Rebuild Trie",
            new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));

    // Telemetry Cards
    private final JLabel lblWordCount = new JLabel("0");
    private final JLabel lblNodeCount = new JLabel("0");
    private final JLabel lblTrieMemory = new JLabel("0 KB");
    private final JLabel lblLookupLatency = new JLabel("-");

    // Suggestions Table
    private final DefaultTableModel suggestionsModel = new DefaultTableModel(
            new String[]{"#", "Word Suggestion", "Length", "Prefix Match"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable suggestionsTable = new JTable(suggestionsModel);

    public AutocompleteView(AutocompleteService autocompleteService, MainWindow mainWindow) {
        this.autocompleteService = autocompleteService;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
        updateTrieTelemetry();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Trie-Based Autocomplete & Word Completion");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Sub-millisecond prefix suggestion powered by an in-memory prefix tree data structure");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);
        header.add(titleGroup, BorderLayout.WEST);

        // Input Card
        ModernCard inputCard = UITheme.createCardPanel();
        inputCard.setLayout(new BorderLayout(0, 12));

        JPanel prefixRow = new JPanel(new BorderLayout(12, 0));
        prefixRow.setOpaque(false);

        JLabel lblPrefix = new JLabel("Type Prefix:");
        lblPrefix.setFont(UITheme.FONT_SUBHEADER);
        txtPrefix.setFont(UITheme.FONT_MONO);

        // Real-time live update listener
        txtPrefix.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
        });

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        JLabel lblLim = new JLabel("Max Results:");
        lblLim.setFont(UITheme.FONT_BODY);
        spinLimit.setFont(UITheme.FONT_BODY);
        spinLimit.addChangeListener(e -> updateSuggestions());

        btnRebuildTrie.addActionListener(e -> {
            autocompleteService.rebuildTrieFromDocuments();
            updateTrieTelemetry();
            updateSuggestions();
            mainWindow.showNotification("Trie index rebuilt successfully with " + autocompleteService.getWordCount() + " unique words.");
        });

        rightControls.add(lblLim);
        rightControls.add(spinLimit);
        rightControls.add(btnRebuildTrie);

        prefixRow.add(lblPrefix, BorderLayout.WEST);
        prefixRow.add(txtPrefix, BorderLayout.CENTER);
        prefixRow.add(rightControls, BorderLayout.EAST);

        inputCard.add(prefixRow, BorderLayout.CENTER);

        // Top Section
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(inputCard, BorderLayout.SOUTH);

        // Telemetry cards grid (4 boxes)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricBox("Indexed Words in Trie", lblWordCount, UITheme.PRIMARY));
        metricsGrid.add(createMetricBox("Total Trie Nodes", lblNodeCount, UITheme.SUCCESS));
        metricsGrid.add(createMetricBox("Est. Trie Memory", lblTrieMemory, new Color(139, 92, 246)));
        metricsGrid.add(createMetricBox("Prefix Lookup Latency", lblLookupLatency, UITheme.TEXT_SECONDARY));

        // Suggestions Table Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel tableTitle = new JLabel("Real-time Autocomplete Suggestions");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        UITheme.styleTable(suggestionsTable);
        suggestionsTable.getColumnModel().getColumn(0).setMaxWidth(60);
        suggestionsTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        suggestionsTable.getColumnModel().getColumn(2).setMaxWidth(100);
        suggestionsTable.getColumnModel().getColumn(3).setPreferredWidth(280);

        suggestionsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                l.setFont(UITheme.FONT_MONO);
                l.setBorder(new EmptyBorder(4, 10, 4, 10));
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableTitle, BorderLayout.NORTH);
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

    public void updateTrieTelemetry() {
        int words = autocompleteService.getWordCount();
        int nodes = autocompleteService.getNodeCount();
        long memBytes = (long) nodes * 48L; // estimated Java object overhead

        lblWordCount.setText(String.format("%,d words", words));
        lblNodeCount.setText(String.format("%,d nodes", nodes));
        lblTrieMemory.setText(String.format("%,d KB", memBytes / 1024));
    }

    private void updateSuggestions() {
        String prefix = txtPrefix.getText();
        if (prefix == null || prefix.trim().isEmpty()) {
            suggestionsModel.setRowCount(0);
            lblLookupLatency.setText("-");
            return;
        }

        int limit = (int) spinLimit.getValue();

        long startNs = System.nanoTime();
        List<String> suggestions = autocompleteService.getSuggestions(prefix, limit);
        long durationNs = System.nanoTime() - startNs;

        lblLookupLatency.setText(String.format("%,d ns (%.3f ms)", durationNs, durationNs / 1_000_000.0));

        suggestionsModel.setRowCount(0);
        int rank = 1;
        String cleanPrefix = prefix.trim().toLowerCase();

        for (String word : suggestions) {
            String htmlHighlight;
            if (word.toLowerCase().startsWith(cleanPrefix)) {
                String matchPart = word.substring(0, cleanPrefix.length());
                String restPart = word.substring(cleanPrefix.length());
                htmlHighlight = "<html><span style='background:#EEF2FF; color:#4F46E5; font-weight:bold; padding:2px 4px; border-radius:3px;'>"
                        + escapeHtml(matchPart) + "</span>" + escapeHtml(restPart) + "</html>";
            } else {
                htmlHighlight = "<html>" + escapeHtml(word) + "</html>";
            }

            suggestionsModel.addRow(new Object[]{
                    rank++,
                    word,
                    word.length() + " chars",
                    htmlHighlight
            });
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
