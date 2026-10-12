package com.suffixindex.ui;

import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.IndexService;
import com.suffixindex.util.DateUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Index Construction View: executes Prefix Doubling & Kasai algorithms with real-time profiling.
 */
public class IndexView extends JPanel {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();
    private final JLabel lblDocChars = new JLabel("-");
    private final JLabel lblDocStatus = new JLabel("-");
    private final JLabel lblDocModified = new JLabel("-");
    private final JLabel lblDocIndexed = new JLabel("-");

    private final ModernButton btnBuild = UITheme.createPrimaryButton("⚡  BUILD SUFFIX INDEX",
            new ModernIcon(ModernIcon.IconType.BUILD_INDEX, 18, Color.WHITE));
    private final JProgressBar progressBar = new JProgressBar();

    // Result metrics card
    private final ModernCard resultCard = UITheme.createCardPanel();
    private final JLabel lblResultStatus = new JLabel("Ready");
    private final JLabel lblResultTime = new JLabel("-");
    private final JLabel lblResultSuffixes = new JLabel("-");
    private final JLabel lblResultFile = new JLabel("-");
    private final JPanel resultActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

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

    public IndexView(DocumentService documentService, IndexService indexService, MainWindow mainWindow) {
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

        JLabel title = new JLabel("Suffix Index Builder");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Construct Suffix Array via Prefix Doubling & LCP Array via Kasai Algorithm");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        header.add(titleGroup, BorderLayout.WEST);

        // Center Content Card: Selector + Info + Action
        ModernCard centerCard = UITheme.createCardPanel();
        centerCard.setLayout(new BorderLayout(0, 20));

        // Selection row
        JPanel selectPanel = new JPanel(new BorderLayout(12, 0));
        selectPanel.setOpaque(false);
        JLabel lblSelect = new JLabel("Select Document:");
        lblSelect.setFont(UITheme.FONT_SUBHEADER);
        comboDocuments.setFont(UITheme.FONT_BODY);
        comboDocuments.addActionListener(e -> updateSelectedDocInfo());

        ModernButton btnReload = UITheme.createSecondaryButton("Refresh List",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnReload.addActionListener(e -> loadDocuments(null));

        selectPanel.add(lblSelect, BorderLayout.WEST);
        selectPanel.add(comboDocuments, BorderLayout.CENTER);
        selectPanel.add(btnReload, BorderLayout.EAST);

        // Details Panel
        ModernCard detailsPanel = new ModernCard(10, null);
        detailsPanel.setLayout(new GridLayout(2, 2, 16, 12));
        detailsPanel.setCardBackground(new Color(248, 250, 252));

        detailsPanel.add(createDetailItem("Character Count:", lblDocChars));
        detailsPanel.add(createDetailItem("Index Status:", lblDocStatus));
        detailsPanel.add(createDetailItem("Last Modified:", lblDocModified));
        detailsPanel.add(createDetailItem("Last Indexed:", lblDocIndexed));

        // Action & Progress
        JPanel actionPanel = new JPanel(new BorderLayout(0, 12));
        actionPanel.setOpaque(false);

        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready to build");
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 24));

        btnBuild.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnBuild.setPreferredSize(new Dimension(btnBuild.getPreferredSize().width, 48));
        btnBuild.addActionListener(e -> executeIndexBuild());

        actionPanel.add(btnBuild, BorderLayout.NORTH);
        actionPanel.add(progressBar, BorderLayout.SOUTH);

        // Upper Section assembly
        JPanel upperSection = new JPanel(new BorderLayout(0, 16));
        upperSection.setOpaque(false);
        upperSection.add(selectPanel, BorderLayout.NORTH);
        upperSection.add(detailsPanel, BorderLayout.CENTER);
        upperSection.add(actionPanel, BorderLayout.SOUTH);

        // Result Card
        resultCard.setLayout(new BorderLayout(0, 14));
        resultCard.setVisible(false);

        JPanel resTitlePanel = new JPanel(new BorderLayout());
        resTitlePanel.setOpaque(false);
        JLabel resHeader = new JLabel("Index Construction Telemetry");
        resHeader.setFont(UITheme.FONT_SUBHEADER);
        lblResultStatus.setFont(UITheme.FONT_BODY_BOLD);
        resTitlePanel.add(resHeader, BorderLayout.WEST);
        resTitlePanel.add(lblResultStatus, BorderLayout.EAST);

        JPanel metricsGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricBox("Construction Time", lblResultTime, UITheme.PRIMARY));
        metricsGrid.add(createMetricBox("Suffixes Indexed", lblResultSuffixes, UITheme.SUCCESS));
        metricsGrid.add(createMetricBox("Index Storage File", lblResultFile, new Color(139, 92, 246)));

        // Result Action buttons
        resultActions.setOpaque(false);
        ModernButton btnViewSA = UITheme.createPrimaryButton("View Suffix Array",
                new ModernIcon(ModernIcon.IconType.SUFFIX_ARRAY, 16, Color.WHITE));
        btnViewSA.addActionListener(e -> {
            DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
            if (item != null) mainWindow.navigateToSuffixArrayWithDoc(item.meta.getDocumentId());
        });

        ModernButton btnViewLCP = UITheme.createSecondaryButton("View LCP Array",
                new ModernIcon(ModernIcon.IconType.LCP_ARRAY, 16, UITheme.PRIMARY));
        btnViewLCP.addActionListener(e -> {
            DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
            if (item != null) mainWindow.navigateToLCPWithDoc(item.meta.getDocumentId());
        });

        ModernButton btnGoSearch = UITheme.createSecondaryButton("Search in Document",
                new ModernIcon(ModernIcon.IconType.SEARCH, 16, UITheme.PRIMARY));
        btnGoSearch.addActionListener(e -> {
            DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
            if (item != null) mainWindow.navigateToSearchWithDoc(item.meta.getDocumentId());
        });

        resultActions.add(btnViewSA);
        resultActions.add(btnViewLCP);
        resultActions.add(btnGoSearch);

        resultCard.add(resTitlePanel, BorderLayout.NORTH);
        resultCard.add(metricsGrid, BorderLayout.CENTER);
        resultCard.add(resultActions, BorderLayout.SOUTH);

        centerCard.add(upperSection, BorderLayout.NORTH);
        centerCard.add(resultCard, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(centerCard, BorderLayout.CENTER);
    }

    private JPanel createDetailItem(String title, JLabel valLabel) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(title);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        valLabel.setFont(UITheme.FONT_BODY);
        p.add(l, BorderLayout.WEST);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    private JPanel createMetricBox(String title, JLabel valueLabel, Color accentColor) {
        ModernCard p = new ModernCard(10, null);
        p.setLayout(new BorderLayout(0, 4));
        p.setCardBackground(new Color(248, 250, 252));

        JLabel l = new JLabel(title);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(accentColor);

        p.add(l, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private void updateSelectedDocInfo() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) {
            lblDocChars.setText("-");
            lblDocStatus.setText("-");
            lblDocModified.setText("-");
            lblDocIndexed.setText("-");
            btnBuild.setEnabled(false);
            return;
        }

        DocumentMetadata meta = item.meta;
        lblDocChars.setText(String.format("%,d characters", meta.getCharacterCount()));
        lblDocStatus.setText(meta.getIndexStatus().getDisplayName());
        if (meta.getIndexStatus() == IndexStatus.INDEXED) {
            lblDocStatus.setForeground(UITheme.SUCCESS_TEXT);
        } else if (meta.getIndexStatus() == IndexStatus.OUTDATED) {
            lblDocStatus.setForeground(UITheme.WARNING_TEXT);
        } else {
            lblDocStatus.setForeground(UITheme.TEXT_MUTED);
        }
        lblDocModified.setText(DateUtils.formatEpoch(meta.getLastModified()));
        lblDocIndexed.setText(DateUtils.formatEpoch(meta.getLastIndexed()));

        btnBuild.setEnabled(true);
        if (meta.getIndexStatus() == IndexStatus.OUTDATED) {
            btnBuild.setText("⚠️  REBUILD OUTDATED SUFFIX INDEX");
        } else if (meta.getIndexStatus() == IndexStatus.INDEXED) {
            btnBuild.setText("⚡  REBUILD SUFFIX INDEX");
        } else {
            btnBuild.setText("⚡  BUILD SUFFIX INDEX");
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
            updateSelectedDocInfo();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading document list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeIndexBuild() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) return;

        btnBuild.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Executing Prefix Doubling & Kasai Algorithms...");

        // Run asynchronously so UI remains responsive and smooth
        SwingWorker<SuffixIndex, Void> worker = new SwingWorker<>() {
            @Override
            protected SuffixIndex doInBackground() throws Exception {
                return indexService.buildIndex(item.meta.getDocumentId());
            }

            @Override
            protected void done() {
                btnBuild.setEnabled(true);
                progressBar.setVisible(false);

                try {
                    SuffixIndex index = get();
                    lblResultStatus.setText("✓ Index Generated & Saved");
                    lblResultStatus.setForeground(UITheme.SUCCESS_TEXT);
                    lblResultTime.setText(String.format("%.3f ms (%,d ns)", index.getConstructionTimeMs(), index.getConstructionTimeNs()));
                    lblResultSuffixes.setText(String.format("%,d suffixes", index.getSuffixCount()));
                    lblResultFile.setText(index.getDocumentId() + ".index");
                    resultCard.setVisible(true);

                    loadDocuments(item.meta.getDocumentId());
                    mainWindow.showNotification("Suffix Array & LCP Array created in " + String.format("%.3f ms", index.getConstructionTimeMs()) + ".");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(IndexView.this, "Failed to build index: " + ex.getMessage(), "Indexing Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
}
