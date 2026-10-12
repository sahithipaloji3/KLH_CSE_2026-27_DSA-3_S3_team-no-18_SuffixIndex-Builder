package com.suffixindex.ui;

import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.IndexService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Suffix Array Visualizer: paginated inspection of sorted suffixes, start positions, and LCP values.
 */
public class SuffixArrayView extends JPanel {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();
    private final JComboBox<Integer> comboPageSize = new JComboBox<>(new Integer[]{25, 50, 100, 250, 500});
    private final ModernButton btnPrev = UITheme.createSecondaryButton("◀ Previous");
    private final ModernButton btnNext = UITheme.createSecondaryButton("Next ▶");
    private final JLabel lblPageInfo = new JLabel("Page 1 of 1");
    private final JLabel lblTotalSuffixes = new JLabel("Total Suffixes: 0");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Rank", "Position", "Suffix Preview", "LCP with Previous"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    // Selected suffix detail preview
    private final JTextArea txtSuffixDetail = new JTextArea(3, 20);

    private SuffixIndex currentIndex = null;
    private String currentText = "";
    private int currentPage = 1;
    private int pageSize = 50;

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

    public SuffixArrayView(DocumentService documentService, IndexService indexService, MainWindow mainWindow) {
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

        JLabel title = new JLabel("Suffix Array Visualization");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Inspect all lexicographically sorted suffixes, starting positions, and LCP values");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        header.add(titleGroup, BorderLayout.WEST);

        // Controls bar: document selector + page size
        ModernCard controlsCard = UITheme.createCardPanel();
        controlsCard.setLayout(new BorderLayout(16, 0));

        JPanel docPanel = new JPanel(new BorderLayout(10, 0));
        docPanel.setOpaque(false);
        JLabel lblDoc = new JLabel("Select Document:");
        lblDoc.setFont(UITheme.FONT_SUBHEADER);
        comboDocuments.setFont(UITheme.FONT_BODY);
        comboDocuments.addActionListener(e -> loadSelectedDocumentIndex());

        ModernButton btnReload = UITheme.createSecondaryButton("Refresh",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnReload.addActionListener(e -> loadDocuments(null));

        docPanel.add(lblDoc, BorderLayout.WEST);
        docPanel.add(comboDocuments, BorderLayout.CENTER);
        docPanel.add(btnReload, BorderLayout.EAST);

        controlsCard.add(docPanel, BorderLayout.CENTER);

        // Center Table Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 12));

        // Table setup
        UITheme.styleTable(table);
        table.setFont(UITheme.FONT_MONO);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setMaxWidth(100);
        table.getColumnModel().getColumn(3).setMaxWidth(140);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    int pos = (int) table.getValueAt(row, 1);
                    if (pos >= 0 && pos < currentText.length()) {
                        txtSuffixDetail.setText(currentText.substring(pos));
                        txtSuffixDetail.setCaretPosition(0);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        // Detail Drawer
        JPanel detailDrawer = new JPanel(new BorderLayout(0, 6));
        detailDrawer.setOpaque(false);
        JLabel lblDetail = new JLabel("Selected Full Suffix Preview:");
        lblDetail.setFont(UITheme.FONT_SUBHEADER);
        txtSuffixDetail.setFont(UITheme.FONT_MONO);
        txtSuffixDetail.setEditable(false);
        txtSuffixDetail.setLineWrap(true);
        txtSuffixDetail.setWrapStyleWord(true);
        txtSuffixDetail.setBackground(new Color(248, 250, 252));
        JScrollPane detailScroll = new JScrollPane(txtSuffixDetail);
        detailScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 75));
        detailScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        detailDrawer.add(lblDetail, BorderLayout.NORTH);
        detailDrawer.add(detailScroll, BorderLayout.CENTER);

        // Pagination toolbar
        JPanel paginationBar = new JPanel(new BorderLayout());
        paginationBar.setOpaque(false);

        JPanel leftPaging = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPaging.setOpaque(false);
        leftPaging.add(lblTotalSuffixes);
        lblTotalSuffixes.setFont(UITheme.FONT_BODY_BOLD);
        leftPaging.add(new JLabel("Rows per page:"));
        comboPageSize.setSelectedItem(pageSize);
        comboPageSize.addActionListener(e -> {
            pageSize = (Integer) comboPageSize.getSelectedItem();
            currentPage = 1;
            renderCurrentPage();
        });
        leftPaging.add(comboPageSize);

        JPanel rightPaging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPaging.setOpaque(false);
        btnPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
            }
        });
        btnNext.addActionListener(e -> {
            int totalPages = getTotalPages();
            if (currentPage < totalPages) {
                currentPage++;
                renderCurrentPage();
            }
        });
        lblPageInfo.setFont(UITheme.FONT_BODY_BOLD);
        rightPaging.add(btnPrev);
        rightPaging.add(lblPageInfo);
        rightPaging.add(btnNext);

        paginationBar.add(leftPaging, BorderLayout.WEST);
        paginationBar.add(rightPaging, BorderLayout.EAST);

        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(detailDrawer, BorderLayout.SOUTH);

        // Top assembly
        JPanel topGroup = new JPanel(new BorderLayout(0, 12));
        topGroup.setOpaque(false);
        topGroup.add(header, BorderLayout.NORTH);
        topGroup.add(controlsCard, BorderLayout.CENTER);
        topGroup.add(paginationBar, BorderLayout.SOUTH);

        add(topGroup, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    private int getTotalPages() {
        if (currentIndex == null || currentIndex.getSuffixCount() == 0) return 1;
        return (int) Math.ceil((double) currentIndex.getSuffixCount() / pageSize);
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
            loadSelectedDocumentIndex();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading documents: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedDocumentIndex() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        tableModel.setRowCount(0);
        txtSuffixDetail.setText("");

        if (item == null) {
            currentIndex = null;
            currentText = "";
            lblTotalSuffixes.setText("Total Suffixes: 0");
            lblPageInfo.setText("Page 1 of 1");
            return;
        }

        try {
            Document doc = documentService.getDocument(item.meta.getDocumentId());
            currentText = doc.getContent();
            currentIndex = indexService.getIndex(item.meta.getDocumentId());

            if (currentIndex == null) {
                lblTotalSuffixes.setText("No index file found for document. Please build index.");
                lblPageInfo.setText("Page 1 of 1");
                return;
            }

            lblTotalSuffixes.setText(String.format("Total Suffixes: %,d", currentIndex.getSuffixCount()));
            currentPage = 1;
            renderCurrentPage();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load index: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderCurrentPage() {
        tableModel.setRowCount(0);
        if (currentIndex == null || currentIndex.getSuffixCount() == 0) {
            lblPageInfo.setText("Page 1 of 1");
            btnPrev.setEnabled(false);
            btnNext.setEnabled(false);
            return;
        }

        int totalSuffixes = currentIndex.getSuffixCount();
        int totalPages = getTotalPages();
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        lblPageInfo.setText("Page " + currentPage + " of " + totalPages);
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);

        int startIdx = (currentPage - 1) * pageSize;
        int endIdx = Math.min(totalSuffixes, startIdx + pageSize);

        int[] sa = currentIndex.getSuffixArray();
        int[] lcp = currentIndex.getLcpArray();

        for (int rank = startIdx; rank < endIdx; rank++) {
            int pos = sa[rank];
            int lcpVal = (lcp != null && rank < lcp.length) ? lcp[rank] : 0;

            // Generate truncated preview for current row (up to 60 characters)
            int previewLen = Math.min(60, currentText.length() - pos);
            String preview = (previewLen > 0) ? currentText.substring(pos, pos + previewLen) : "";
            if (pos + previewLen < currentText.length()) {
                preview += "...";
            }

            tableModel.addRow(new Object[]{
                    rank,
                    pos,
                    preview,
                    lcpVal
            });
        }
    }
}
