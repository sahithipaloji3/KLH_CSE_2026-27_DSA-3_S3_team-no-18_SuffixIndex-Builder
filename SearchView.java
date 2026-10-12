package com.suffixindex.ui;

import com.suffixindex.algorithm.NaiveSearcher;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.SearchResult;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.SearchService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Substring Search View: performs Binary Search on precomputed Suffix Array with Naive Search comparison.
 */
public class SearchView extends JPanel {

    private final DocumentService documentService;
    private final SearchService searchService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();
    private final JTextField txtPattern = new JTextField();
    private final JCheckBox chkCaseSensitive = new JCheckBox("Case Sensitive", true);
    private final ModernButton btnSearch = UITheme.createPrimaryButton("SEARCH",
            new ModernIcon(ModernIcon.IconType.SEARCH, 16, Color.WHITE));

    // Outdated warning banner
    private final ModernCard warningCard = new ModernCard(10, UITheme.WARNING);
    private final JLabel lblWarning = new JLabel();
    private final ModernButton btnRebuildNow = UITheme.createPrimaryButton("Rebuild Index",
            new ModernIcon(ModernIcon.IconType.BUILD_INDEX, 15, Color.WHITE));

    // Telemetry Cards
    private final JPanel resultsPanel = new JPanel(new BorderLayout(0, 16));
    private final JLabel lblOccurrences = new JLabel("0");
    private final JLabel lblSearchTime = new JLabel("-");
    private final JLabel lblNaiveTime = new JLabel("-");
    private final JLabel lblSpeedup = new JLabel("-");
    private final JLabel lblComparisons = new JLabel("-");

    // Matches Table
    private final DefaultTableModel matchesModel = new DefaultTableModel(
            new String[]{"#", "Start Position", "Match Text", "Context Snippet"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable matchesTable = new JTable(matchesModel);

    private static class DocumentItem {
        final DocumentMetadata meta;

        DocumentItem(DocumentMetadata meta) {
            this.meta = meta;
        }

        @Override
        public String toString() {
            return meta.getDocumentName() + " (" + meta.getDocumentId() + ") - " + meta.getIndexStatus().getDisplayName();
        }
    }

    public SearchView(DocumentService documentService, SearchService searchService, MainWindow mainWindow) {
        this.documentService = documentService;
        this.searchService = searchService;
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

        JLabel title = new JLabel("Substring Search Engine");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Instant O(m log n) substring retrieval using precomputed Suffix Array Binary Search");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        header.add(titleGroup, BorderLayout.WEST);

        // Warning banner (for missing/outdated index)
        warningCard.setLayout(new BorderLayout(12, 0));
        warningCard.setCardBackground(UITheme.WARNING_BG);
        warningCard.setBorderColor(new Color(251, 191, 36));

        lblWarning.setFont(UITheme.FONT_BODY_BOLD);
        lblWarning.setForeground(UITheme.WARNING_TEXT);
        warningCard.add(lblWarning, BorderLayout.CENTER);

        btnRebuildNow.addActionListener(e -> {
            DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
            if (item != null) mainWindow.navigateToIndexWithDoc(item.meta.getDocumentId());
        });
        warningCard.add(btnRebuildNow, BorderLayout.EAST);
        warningCard.setVisible(false);

        // Search Input Card
        ModernCard searchCard = UITheme.createCardPanel();
        searchCard.setLayout(new BorderLayout(0, 14));

        JPanel inputsGrid = new JPanel(new GridLayout(2, 1, 0, 10));
        inputsGrid.setOpaque(false);

        // Doc selector row
        JPanel docRow = new JPanel(new BorderLayout(10, 0));
        docRow.setOpaque(false);
        JLabel lblDoc = new JLabel("Target Document:");
        lblDoc.setFont(UITheme.FONT_SUBHEADER);
        comboDocuments.setFont(UITheme.FONT_BODY);
        comboDocuments.addActionListener(e -> checkIndexHealth());

        ModernButton btnRefreshDocs = UITheme.createSecondaryButton("Refresh",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnRefreshDocs.addActionListener(e -> loadDocuments(null));

        docRow.add(lblDoc, BorderLayout.WEST);
        docRow.add(comboDocuments, BorderLayout.CENTER);
        docRow.add(btnRefreshDocs, BorderLayout.EAST);

        // Pattern row
        JPanel patternRow = new JPanel(new BorderLayout(10, 0));
        patternRow.setOpaque(false);
        JLabel lblPat = new JLabel("Search Pattern:");
        lblPat.setFont(UITheme.FONT_SUBHEADER);
        txtPattern.setFont(UITheme.FONT_MONO);
        txtPattern.addActionListener(e -> executeSearch());

        chkCaseSensitive.setOpaque(false);
        chkCaseSensitive.setFont(UITheme.FONT_BODY);

        btnSearch.setPreferredSize(new Dimension(140, 38));
        btnSearch.addActionListener(e -> executeSearch());

        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        searchControls.setOpaque(false);
        searchControls.add(chkCaseSensitive);
        searchControls.add(btnSearch);

        patternRow.add(lblPat, BorderLayout.WEST);
        patternRow.add(txtPattern, BorderLayout.CENTER);
        patternRow.add(searchControls, BorderLayout.EAST);

        inputsGrid.add(docRow);
        inputsGrid.add(patternRow);

        searchCard.add(inputsGrid, BorderLayout.CENTER);

        // Top Section assembly
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(warningCard, BorderLayout.CENTER);
        topSection.add(searchCard, BorderLayout.SOUTH);

        // Results Section
        resultsPanel.setOpaque(false);
        resultsPanel.setVisible(false);

        // Telemetry cards grid (5 boxes)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricBox("Occurrences", lblOccurrences, UITheme.PRIMARY));
        metricsGrid.add(createMetricBox("Suffix Array Search", lblSearchTime, UITheme.SUCCESS));
        metricsGrid.add(createMetricBox("Naive Search", lblNaiveTime, new Color(239, 68, 68)));
        metricsGrid.add(createMetricBox("Speedup Factor", lblSpeedup, new Color(139, 92, 246)));
        metricsGrid.add(createMetricBox("Comparisons", lblComparisons, UITheme.TEXT_SECONDARY));

        // Matches Table Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel tableTitle = new JLabel("Match Occurrences & Context Snippets");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        UITheme.styleTable(matchesTable);
        matchesTable.getColumnModel().getColumn(0).setMaxWidth(60);
        matchesTable.getColumnModel().getColumn(1).setMaxWidth(120);
        matchesTable.getColumnModel().getColumn(2).setMaxWidth(160);

        // HTML snippet renderer with highlighted match
        matchesTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                l.setFont(UITheme.FONT_MONO);
                l.setBorder(new EmptyBorder(4, 12, 4, 12));
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(matchesTable);
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

    private void checkIndexHealth() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) {
            warningCard.setVisible(false);
            return;
        }

        DocumentMetadata meta = item.meta;
        if (meta.getIndexStatus() == com.suffixindex.model.IndexStatus.NOT_INDEXED) {
            lblWarning.setText("⚠️ Document is not yet indexed. Binary search requires building an index first.");
            btnRebuildNow.setText("Build Index Now");
            warningCard.setVisible(true);
        } else if (meta.getIndexStatus() == com.suffixindex.model.IndexStatus.OUTDATED) {
            lblWarning.setText("⚠️ This document was modified since indexing. The index is OUTDATED and must be rebuilt.");
            btnRebuildNow.setText("Rebuild Outdated Index");
            warningCard.setVisible(true);
        } else {
            warningCard.setVisible(false);
        }
    }

    public void loadDocuments(String preselectDocId) {
        try {
            comboDocuments.removeAllItems();
            List<DocumentMetadata> docs = documentService.getAllDocuments();
            DocumentItem selected = null;

            for (DocumentMetadata doc : docs) {
                DocumentItem item = new DocumentItem(doc);
                comboDocuments.addItem(item);
                if (preselectDocId != null && doc.getDocumentId().equals(preselectDocId)) {
                    selected = item;
                }
            }

            if (selected != null) {
                comboDocuments.setSelectedItem(selected);
            } else if (comboDocuments.getItemCount() > 0) {
                comboDocuments.setSelectedIndex(0);
            }
            checkIndexHealth();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading document list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeSearch() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a document.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String pattern = txtPattern.getText();
        if (pattern == null || pattern.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search pattern.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean caseSensitive = chkCaseSensitive.isSelected();

        try {
            SearchService.SearchComparison comparison = searchService.executeSearch(
                    item.meta.getDocumentId(), pattern, caseSensitive, 35
            );

            SearchResult saResult = comparison.getSuffixSearchResult();
            NaiveSearcher.NaiveSearchOutcome naive = comparison.getNaiveOutcome();

            // Populate metrics
            lblOccurrences.setText(String.format("%,d", saResult.getOccurrences()));
            lblSearchTime.setText(String.format("%.3f ms (%,d ns)", saResult.getSearchTimeMs(), saResult.getSearchTimeNs()));
            lblNaiveTime.setText(String.format("%.3f ms (%,d ns)", naive.getExecutionTimeMs(), naive.getExecutionTimeNs()));
            lblSpeedup.setText(String.format("%.1fx faster", comparison.getSpeedupFactor()));
            lblComparisons.setText(String.format("%,d vs %,d", saResult.getComparisonCount(), naive.getComparisonCount()));

            // Populate table
            matchesModel.setRowCount(0);
            int idx = 1;
            for (SearchResult.MatchSnippet s : saResult.getSnippets()) {
                String htmlSnippet = "<html><font color='#64748B'>..." + escapeHtml(s.getPrefixContext()) + "</font>"
                        + "<span style='background:#EEF2FF; color:#4F46E5; font-weight:bold; padding:2px 5px; border-radius:4px;'>"
                        + escapeHtml(s.getMatchText()) + "</span>"
                        + "<font color='#64748B'>" + escapeHtml(s.getSuffixContext()) + "...</font></html>";

                matchesModel.addRow(new Object[]{
                        idx++,
                        "Position " + s.getPosition(),
                        s.getMatchText(),
                        htmlSnippet
                });
            }

            resultsPanel.setVisible(true);
            mainWindow.showNotification("Search complete: " + saResult.getOccurrences() + " matches found in "
                    + String.format("%.3f ms", saResult.getSearchTimeMs()));

        } catch (SearchService.OutdatedIndexException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Index Outdated / Missing", JOptionPane.WARNING_MESSAGE);
            checkIndexHealth();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
