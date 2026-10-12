package com.suffixindex.ui;

import com.suffixindex.model.MultiSearchResult;
import com.suffixindex.model.SearchResult;
import com.suffixindex.service.MultiDocumentSearchService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

/**
 * Multi-Document Search View: simultaneously queries all valid indexed documents
 * using Suffix Array binary search, with corpus-level aggregation and multiple sort criteria.
 */
public class MultiSearchView extends JPanel {

    private final MultiDocumentSearchService multiSearchService;
    private final MainWindow mainWindow;

    private final JTextField txtPattern = new JTextField();
    private final JCheckBox chkCaseSensitive = new JCheckBox("Case Sensitive", true);
    private final JComboBox<String> comboSort = new JComboBox<>(new String[]{
            "Sort by Match Count (High to Low)",
            "Sort by Document Name (A-Z)",
            "Sort by First Match Position"
    });
    private final ModernButton btnSearch = UITheme.createPrimaryButton("SEARCH ALL",
            new ModernIcon(ModernIcon.IconType.MULTI_SEARCH, 16, Color.WHITE));

    // Corpus Telemetry Cards
    private final JPanel resultsPanel = new JPanel(new BorderLayout(0, 16));
    private final JLabel lblTotalMatches = new JLabel("0");
    private final JLabel lblMatchedDocs = new JLabel("0");
    private final JLabel lblTotalTime = new JLabel("-");
    private final JLabel lblAvgTimePerDoc = new JLabel("-");

    // Results Table
    private final DefaultTableModel resultsModel = new DefaultTableModel(
            new String[]{"Document Name", "Matches", "First Position", "Search Time", "Sample Snippet", "Action"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5;
        }
    };
    private final JTable resultsTable = new JTable(resultsModel);
    private MultiSearchResult lastResult;

    public MultiSearchView(MultiDocumentSearchService multiSearchService, MainWindow mainWindow) {
        this.multiSearchService = multiSearchService;
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

        JLabel title = new JLabel("Multi-Document Corpus Search");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Query the entire document repository concurrently using precomputed suffix arrays");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);
        header.add(titleGroup, BorderLayout.WEST);

        // Input Card
        ModernCard searchCard = UITheme.createCardPanel();
        searchCard.setLayout(new BorderLayout(0, 12));

        JPanel inputsGrid = new JPanel(new GridLayout(2, 1, 0, 10));
        inputsGrid.setOpaque(false);

        // Pattern Row
        JPanel patternRow = new JPanel(new BorderLayout(10, 0));
        patternRow.setOpaque(false);
        JLabel lblPat = new JLabel("Search Pattern:");
        lblPat.setFont(UITheme.FONT_SUBHEADER);
        txtPattern.setFont(UITheme.FONT_MONO);
        txtPattern.addActionListener(e -> executeMultiSearch());

        patternRow.add(lblPat, BorderLayout.WEST);
        patternRow.add(txtPattern, BorderLayout.CENTER);

        // Controls Row
        JPanel controlsRow = new JPanel(new BorderLayout(10, 0));
        controlsRow.setOpaque(false);

        JPanel sortGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        sortGroup.setOpaque(false);
        JLabel lblSort = new JLabel("Sort Results:");
        lblSort.setFont(UITheme.FONT_BODY);
        comboSort.setFont(UITheme.FONT_BODY);
        comboSort.addActionListener(e -> applySort());
        sortGroup.add(lblSort);
        sortGroup.add(comboSort);

        JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionGroup.setOpaque(false);
        chkCaseSensitive.setFont(UITheme.FONT_BODY);
        chkCaseSensitive.setOpaque(false);

        btnSearch.setPreferredSize(new Dimension(150, 38));
        btnSearch.addActionListener(e -> executeMultiSearch());

        actionGroup.add(chkCaseSensitive);
        actionGroup.add(btnSearch);

        controlsRow.add(sortGroup, BorderLayout.WEST);
        controlsRow.add(actionGroup, BorderLayout.EAST);

        inputsGrid.add(patternRow);
        inputsGrid.add(controlsRow);
        searchCard.add(inputsGrid, BorderLayout.CENTER);

        // Top Section
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(searchCard, BorderLayout.SOUTH);

        // Results Section
        resultsPanel.setOpaque(false);
        resultsPanel.setVisible(false);

        // 4 Telemetry Metrics
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricBox("Total Matches Across Corpus", lblTotalMatches, UITheme.PRIMARY));
        metricsGrid.add(createMetricBox("Documents with Matches", lblMatchedDocs, UITheme.SUCCESS));
        metricsGrid.add(createMetricBox("Total Corpus Search Time", lblTotalTime, new Color(139, 92, 246)));
        metricsGrid.add(createMetricBox("Avg Time per Document", lblAvgTimePerDoc, UITheme.TEXT_SECONDARY));

        // Results Table
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel tableTitle = new JLabel("Corpus Search Results");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        UITheme.styleTable(resultsTable);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        resultsTable.getColumnModel().getColumn(1).setMaxWidth(100);
        resultsTable.getColumnModel().getColumn(2).setMaxWidth(130);
        resultsTable.getColumnModel().getColumn(3).setMaxWidth(120);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(280);
        resultsTable.getColumnModel().getColumn(5).setMaxWidth(130);

        resultsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                l.setFont(UITheme.FONT_MONO);
                l.setBorder(new EmptyBorder(4, 10, 4, 10));
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        resultsPanel.add(metricsGrid, BorderLayout.NORTH);
        resultsPanel.add(tableCard, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
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

    private void executeMultiSearch() {
        String pattern = txtPattern.getText();
        if (pattern == null || pattern.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search pattern.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean caseSensitive = chkCaseSensitive.isSelected();

        try {
            lastResult = multiSearchService.searchAllDocuments(pattern, caseSensitive, 30);
            applySort();

            int matchedDocs = lastResult.getMatchingDocumentsCount();
            int totalDocs = lastResult.getSummaries().size();
            long totalNs = lastResult.getTotalSearchTimeNs();
            double avgNs = totalDocs > 0 ? (double) totalNs / totalDocs : 0;

            lblTotalMatches.setText(String.format("%,d", lastResult.getTotalMatches()));
            lblMatchedDocs.setText(String.format("%d of %d docs", matchedDocs, totalDocs));
            lblTotalTime.setText(String.format("%.3f ms", lastResult.getTotalSearchTimeMs()));
            lblAvgTimePerDoc.setText(String.format("%.3f ms", avgNs / 1_000_000.0));

            resultsPanel.setVisible(true);
            mainWindow.showNotification(String.format("Found %,d matches across %d documents in %.2f ms",
                    lastResult.getTotalMatches(), matchedDocs, lastResult.getTotalSearchTimeMs()));

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error executing corpus search: " + ex.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applySort() {
        if (lastResult == null) return;

        int selected = comboSort.getSelectedIndex();
        if (selected == 0) {
            lastResult.sort(MultiSearchResult.SortOrder.BY_MATCH_COUNT);
        } else if (selected == 1) {
            lastResult.sort(MultiSearchResult.SortOrder.BY_DOCUMENT_NAME);
        } else {
            lastResult.sort(MultiSearchResult.SortOrder.BY_FIRST_POSITION);
        }

        resultsModel.setRowCount(0);
        for (MultiSearchResult.DocumentMatchSummary docSummary : lastResult.getSummaries()) {
            String snippetHtml = "<html><font color='#94A3B8'>No matches</font></html>";
            if (!docSummary.getSnippets().isEmpty()) {
                SearchResult.MatchSnippet s = docSummary.getSnippets().get(0);
                snippetHtml = "<html><font color='#64748B'>..." + escapeHtml(s.getPrefixContext()) + "</font>"
                        + "<span style='background:#EEF2FF; color:#4F46E5; font-weight:bold; padding:1px 4px; border-radius:3px;'>"
                        + escapeHtml(s.getMatchText()) + "</span>"
                        + "<font color='#64748B'>" + escapeHtml(s.getSuffixContext()) + "...</font></html>";
            }

            String firstPosStr = docSummary.getMatchCount() > 0 ? "Pos " + docSummary.getFirstMatchPosition() : "-";
            String searchTimeStr = String.format("%.3f ms", docSummary.getSearchTimeMs());

            resultsModel.addRow(new Object[]{
                    docSummary.getDocumentName(),
                    docSummary.getMatchCount() + " matches",
                    firstPosStr,
                    searchTimeStr,
                    snippetHtml,
                    "Open in Search"
            });
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
