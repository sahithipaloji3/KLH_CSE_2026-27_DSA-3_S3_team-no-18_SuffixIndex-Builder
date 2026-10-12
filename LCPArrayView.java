package com.suffixindex.ui;

import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.IndexService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * LCP Array Visualization & Analysis: computes Longest Repeated Substring (LRS)
 * and displays LCP distribution metrics using Kasai's algorithm output.
 */
public class LCPArrayView extends JPanel {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();

    // LRS Analysis Card components
    private final JLabel lblLrsText = new JLabel("-");
    private final JLabel lblLrsPositions = new JLabel("-");
    private final JLabel lblAvgLcp = new JLabel("0.0");
    private final JLabel lblMaxLcp = new JLabel("0");

    private final DefaultTableModel lcpTableModel = new DefaultTableModel(
            new String[]{"Rank", "Position", "LCP", "Common Prefix Substring", "Suffix 1", "Suffix 2 (Predecessor)"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable lcpTable = new JTable(lcpTableModel);

    private SuffixIndex currentIndex = null;
    private String currentText = "";

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

    public LCPArrayView(DocumentService documentService, IndexService indexService, MainWindow mainWindow) {
        this.documentService = documentService;
        this.indexService = indexService;
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

        JLabel title = new JLabel("LCP Array & Repeat Analysis");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Longest Common Prefix evaluation via Kasai's algorithm & Longest Repeated Substring (LRS) discovery");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        header.add(titleGroup, BorderLayout.WEST);

        // Document selector bar
        ModernCard selectorCard = UITheme.createCardPanel();
        selectorCard.setLayout(new BorderLayout(12, 0));

        JLabel lblDoc = new JLabel("Target Document:");
        lblDoc.setFont(UITheme.FONT_SUBHEADER);
        comboDocuments.setFont(UITheme.FONT_BODY);
        comboDocuments.addActionListener(e -> analyzeLcp());

        ModernButton btnReload = UITheme.createSecondaryButton("Refresh",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnReload.addActionListener(e -> loadDocuments(null));

        selectorCard.add(lblDoc, BorderLayout.WEST);
        selectorCard.add(comboDocuments, BorderLayout.CENTER);
        selectorCard.add(btnReload, BorderLayout.EAST);

        // Analysis Cards Grid: LRS + Metrics
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricCard("Longest Repeated Substring (LRS)", lblLrsText, UITheme.PRIMARY));
        metricsGrid.add(createMetricCard("Max LCP Length", lblMaxLcp, UITheme.SUCCESS));
        metricsGrid.add(createMetricCard("Average LCP", lblAvgLcp, new Color(139, 92, 246)));
        metricsGrid.add(createMetricCard("LRS Occurrence Positions", lblLrsPositions, UITheme.TEXT_SECONDARY));

        // Center Table Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel tableTitle = new JLabel("LCP Pairs (Suffix Array Neighbors)");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        UITheme.styleTable(lcpTable);
        lcpTable.setFont(UITheme.FONT_MONO);
        lcpTable.getColumnModel().getColumn(0).setMaxWidth(70);
        lcpTable.getColumnModel().getColumn(1).setMaxWidth(80);
        lcpTable.getColumnModel().getColumn(2).setMaxWidth(80);

        // Highlight non-zero LCP cells
        lcpTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                l.setBorder(new EmptyBorder(4, 10, 4, 10));
                if (value instanceof Integer && ((Integer) value) > 0) {
                    l.setForeground(UITheme.PRIMARY);
                    l.setFont(UITheme.FONT_MONO_BOLD);
                } else {
                    l.setForeground(UITheme.TEXT_MUTED);
                    l.setFont(UITheme.FONT_MONO);
                }
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(lcpTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Top assembly
        JPanel topGroup = new JPanel(new BorderLayout(0, 12));
        topGroup.setOpaque(false);
        topGroup.add(header, BorderLayout.NORTH);
        topGroup.add(selectorCard, BorderLayout.CENTER);
        topGroup.add(metricsGrid, BorderLayout.SOUTH);

        add(topGroup, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valLabel, Color color) {
        ModernCard p = UITheme.createCardPanel();
        p.setLayout(new BorderLayout(0, 4));

        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_SECONDARY);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valLabel.setForeground(color);

        p.add(t, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
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
            analyzeLcp();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading documents: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void analyzeLcp() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        lcpTableModel.setRowCount(0);

        if (item == null) {
            lblLrsText.setText("-");
            lblMaxLcp.setText("0");
            lblAvgLcp.setText("0.0");
            lblLrsPositions.setText("-");
            return;
        }

        try {
            Document doc = documentService.getDocument(item.meta.getDocumentId());
            currentText = doc.getContent();
            currentIndex = indexService.getIndex(item.meta.getDocumentId());

            if (currentIndex == null || currentIndex.getSuffixCount() == 0) {
                lblLrsText.setText("No index found");
                lblMaxLcp.setText("0");
                lblAvgLcp.setText("0.0");
                lblLrsPositions.setText("-");
                return;
            }

            int[] sa = currentIndex.getSuffixArray();
            int[] lcp = currentIndex.getLcpArray();
            int n = currentIndex.getSuffixCount();

            // Compute statistics and Longest Repeated Substring
            int maxLcp = 0;
            int maxIdx = -1;
            long lcpSum = 0;

            for (int i = 0; i < n; i++) {
                int val = lcp[i];
                lcpSum += val;
                if (val > maxLcp) {
                    maxLcp = val;
                    maxIdx = i;
                }
            }

            double avgLcp = n > 0 ? (double) lcpSum / n : 0.0;

            lblMaxLcp.setText(maxLcp + " chars");
            lblAvgLcp.setText(String.format("%.2f", avgLcp));

            if (maxIdx >= 0 && maxLcp > 0) {
                int pos1 = sa[maxIdx];
                int pos2 = sa[maxIdx - 1];
                String lrs = currentText.substring(pos1, pos1 + maxLcp);
                String lrsDisplay = (lrs.length() > 25) ? lrs.substring(0, 25) + "..." : lrs;
                lblLrsText.setText("\"" + lrsDisplay + "\"");
                lblLrsPositions.setText("Pos " + pos1 + " & " + pos2);
            } else {
                lblLrsText.setText("No repeats");
                lblLrsPositions.setText("N/A");
            }

            // Populate table (up to 500 rows for memory efficiency)
            int rowsToDisplay = Math.min(n, 500);
            for (int rank = 0; rank < rowsToDisplay; rank++) {
                int pos = sa[rank];
                int lcpVal = lcp[rank];

                String commonPrefix = "";
                if (lcpVal > 0 && rank > 0) {
                    int prevPos = sa[rank - 1];
                    int len = Math.min(lcpVal, Math.min(currentText.length() - pos, currentText.length() - prevPos));
                    commonPrefix = currentText.substring(pos, pos + len);
                    if (commonPrefix.length() > 30) commonPrefix = commonPrefix.substring(0, 30) + "...";
                }

                int s1Len = Math.min(40, currentText.length() - pos);
                String s1 = currentText.substring(pos, pos + s1Len);

                String s2 = "-";
                if (rank > 0) {
                    int prevPos = sa[rank - 1];
                    int s2Len = Math.min(40, currentText.length() - prevPos);
                    s2 = currentText.substring(prevPos, prevPos + s2Len);
                }

                lcpTableModel.addRow(new Object[]{
                        rank,
                        pos,
                        lcpVal,
                        commonPrefix.isEmpty() ? "-" : commonPrefix,
                        s1,
                        s2
                });
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error analyzing LCP array: " + ex.getMessage(), "Analysis Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
