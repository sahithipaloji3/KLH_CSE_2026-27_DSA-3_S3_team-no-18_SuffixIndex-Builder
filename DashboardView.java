package com.suffixindex.ui;

import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.model.SearchHistoryEntry;
import com.suffixindex.repository.SearchHistoryRepository;
import com.suffixindex.service.DocumentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Dashboard View displaying real-time statistics directly from files.
 */
public class DashboardView extends JPanel {

    private final DocumentService documentService;
    private final SearchHistoryRepository searchHistoryRepository;
    private final MainWindow mainWindow;

    // Stat metric labels
    private final JLabel lblTotalDocs = new JLabel("0");
    private final JLabel lblIndexedDocs = new JLabel("0");
    private final JLabel lblTotalChars = new JLabel("0");
    private final JLabel lblTotalIndexes = new JLabel("0");
    private final JLabel lblTotalSearches = new JLabel("0");

    private final ModernCard alertCard = new ModernCard(10, UITheme.WARNING);
    private final JLabel alertLabel = new JLabel();

    private final DefaultTableModel recentSearchesModel = new DefaultTableModel(
            new String[]{"Time", "Document", "Pattern", "Matches", "Search Time"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public DashboardView(DocumentService documentService, SearchHistoryRepository searchHistoryRepository, MainWindow mainWindow) {
        this.documentService = documentService;
        this.searchHistoryRepository = searchHistoryRepository;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("System Dashboard");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Real-time index telemetry loaded directly from local file persistence");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        ModernButton btnRefresh = UITheme.createSecondaryButton("Refresh Data",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnRefresh.addActionListener(e -> refreshData());

        headerPanel.add(titleGroup, BorderLayout.WEST);
        headerPanel.add(btnRefresh, BorderLayout.EAST);

        // Alerts / Outdated Banner
        alertCard.setLayout(new BorderLayout(14, 0));
        alertCard.setCardBackground(UITheme.WARNING_BG);
        alertCard.setBorderColor(new Color(251, 191, 36));

        alertLabel.setFont(UITheme.FONT_BODY_BOLD);
        alertLabel.setForeground(UITheme.WARNING_TEXT);
        alertCard.add(alertLabel, BorderLayout.CENTER);

        ModernButton btnFixAlert = UITheme.createPrimaryButton("Review Outdated",
                new ModernIcon(ModernIcon.IconType.ALERT, 16, Color.WHITE));
        btnFixAlert.addActionListener(e -> mainWindow.navigateTo("Documents"));
        alertCard.add(btnFixAlert, BorderLayout.EAST);
        alertCard.setVisible(false);

        // Metrics Grid (5 cards)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 5, 14, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.add(createMetricCard("Documents", lblTotalDocs, new Color(59, 130, 246)));
        metricsGrid.add(createMetricCard("Indexed Docs", lblIndexedDocs, UITheme.SUCCESS));
        metricsGrid.add(createMetricCard("Total Characters", lblTotalChars, UITheme.PRIMARY));
        metricsGrid.add(createMetricCard("Total Indexes", lblTotalIndexes, new Color(139, 92, 246)));
        metricsGrid.add(createMetricCard("Total Searches", lblTotalSearches, new Color(236, 72, 153)));

        // Center Content: Quick Actions + Recent Searches
        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);

        // Quick Actions Card
        ModernCard actionsCard = UITheme.createCardPanel();
        actionsCard.setLayout(new BorderLayout(16, 0));

        JLabel actTitle = new JLabel("Quick Actions:");
        actTitle.setFont(UITheme.FONT_SUBHEADER);
        actTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel actButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actButtons.setOpaque(false);

        ModernButton btnNewDoc = UITheme.createPrimaryButton("+ New Document",
                new ModernIcon(ModernIcon.IconType.PLUS, 16, Color.WHITE));
        btnNewDoc.addActionListener(e -> mainWindow.navigateTo("Documents"));

        ModernButton btnBuildIdx = UITheme.createSecondaryButton("Build Suffix Index",
                new ModernIcon(ModernIcon.IconType.BUILD_INDEX, 16, UITheme.PRIMARY));
        btnBuildIdx.addActionListener(e -> mainWindow.navigateTo("Build Index"));

        ModernButton btnSearch = UITheme.createSecondaryButton("Search Documents",
                new ModernIcon(ModernIcon.IconType.SEARCH, 16, UITheme.PRIMARY));
        btnSearch.addActionListener(e -> mainWindow.navigateTo("Search"));

        actButtons.add(btnNewDoc);
        actButtons.add(btnBuildIdx);
        actButtons.add(btnSearch);

        actionsCard.add(actTitle, BorderLayout.WEST);
        actionsCard.add(actButtons, BorderLayout.CENTER);

        // Recent Searches Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 12));

        JLabel tableTitle = new JLabel("Recent Search Inquiries");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);

        JTable table = new JTable(recentSearchesModel);
        UITheme.styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(actionsCard, BorderLayout.NORTH);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        // Assembly
        JPanel topContainer = new JPanel(new BorderLayout(0, 16));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(alertCard, BorderLayout.CENTER);
        topContainer.add(metricsGrid, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String label, JLabel valueLabel, Color accentColor) {
        ModernCard card = UITheme.createCardPanelWithAccent(accentColor);
        card.setLayout(new BorderLayout(0, 8));

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 6));
        content.setOpaque(false);

        JLabel lblTitle = new JLabel(label);
        lblTitle.setFont(UITheme.FONT_SMALL);
        lblTitle.setForeground(UITheme.TEXT_SECONDARY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        content.add(lblTitle);
        content.add(valueLabel);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    public void refreshData() {
        try {
            List<DocumentMetadata> docs = documentService.getAllDocuments();
            int totalDocs = docs.size();
            int indexedDocs = 0;
            int outdatedDocs = 0;
            long totalChars = 0;

            for (DocumentMetadata doc : docs) {
                totalChars += doc.getCharacterCount();
                if (doc.getIndexStatus() == IndexStatus.INDEXED) {
                    indexedDocs++;
                } else if (doc.getIndexStatus() == IndexStatus.OUTDATED) {
                    outdatedDocs++;
                }
            }

            int totalSearches = searchHistoryRepository.getCount();

            lblTotalDocs.setText(String.format("%,d", totalDocs));
            lblIndexedDocs.setText(String.format("%,d", indexedDocs));
            lblTotalChars.setText(String.format("%,d", totalChars));
            lblTotalIndexes.setText(String.format("%,d", indexedDocs));
            lblTotalSearches.setText(String.format("%,d", totalSearches));

            // Outdated alert banner
            if (outdatedDocs > 0) {
                alertLabel.setText("⚠️  Warning: " + outdatedDocs + " document index(es) are OUTDATED due to text edits. Rebuilding is recommended.");
                alertCard.setVisible(true);
            } else {
                alertCard.setVisible(false);
            }

            // Recent searches table
            recentSearchesModel.setRowCount(0);
            List<SearchHistoryEntry> history = searchHistoryRepository.loadAll();
            int count = 0;
            for (SearchHistoryEntry h : history) {
                if (count++ >= 10) break;
                recentSearchesModel.addRow(new Object[]{
                        h.getTimestamp(),
                        h.getDocumentName(),
                        h.getPattern(),
                        h.getMatchCount() + " matches",
                        String.format("%.3f ms", h.getSearchTimeMs())
                });
            }

            revalidate();
            repaint();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error refreshing dashboard metrics: " + e.getMessage(),
                    "Storage Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
