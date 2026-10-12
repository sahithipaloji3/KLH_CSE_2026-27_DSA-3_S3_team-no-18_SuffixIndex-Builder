package com.suffixindex.ui;

import com.suffixindex.model.SearchHistoryEntry;
import com.suffixindex.repository.SearchHistoryRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Search History View: inspects, re-runs, and exports persistent search records.
 */
public class HistoryView extends JPanel {

    private final SearchHistoryRepository historyRepository;
    private final MainWindow mainWindow;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Timestamp", "Doc ID", "Document Name", "Search Pattern", "Matches", "Search Time"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel lblCount = new JLabel("0 inquiries recorded");

    public HistoryView(SearchHistoryRepository historyRepository, MainWindow mainWindow) {
        this.historyRepository = historyRepository;
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

        JLabel title = new JLabel("Persistent Search History");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Logged search queries loaded from data/searches/search_history.txt");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        // Action buttons
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        ModernButton btnExport = UITheme.createSecondaryButton("Export Log (.txt)",
                new ModernIcon(ModernIcon.IconType.DOCUMENTS, 15, UITheme.TEXT_PRIMARY));
        btnExport.addActionListener(e -> exportHistory());

        ModernButton btnClear = UITheme.createDangerButton("Clear History",
                new ModernIcon(ModernIcon.IconType.TRASH, 15, Color.WHITE));
        btnClear.addActionListener(e -> clearHistory());

        ModernButton btnRefresh = UITheme.createSecondaryButton("Refresh",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnRefresh.addActionListener(e -> refreshData());

        actionButtons.add(btnExport);
        actionButtons.add(btnClear);
        actionButtons.add(btnRefresh);

        header.add(titleGroup, BorderLayout.WEST);
        header.add(actionButtons, BorderLayout.EAST);

        // Center Table Card
        ModernCard tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel tableTitle = new JLabel("Historical Query Records");
        tableTitle.setFont(UITheme.FONT_SUBHEADER);
        tableTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblCount.setFont(UITheme.FONT_BODY_BOLD);
        lblCount.setForeground(UITheme.TEXT_SECONDARY);
        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(lblCount, BorderLayout.EAST);

        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(160);
        table.getColumnModel().getColumn(1).setMaxWidth(110);
        table.getColumnModel().getColumn(4).setMaxWidth(110);
        table.getColumnModel().getColumn(5).setMaxWidth(130);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        List<SearchHistoryEntry> list = historyRepository.loadAll();
        for (SearchHistoryEntry e : list) {
            tableModel.addRow(new Object[]{
                    e.getTimestamp(),
                    e.getDocumentId(),
                    e.getDocumentName(),
                    e.getPattern(),
                    e.getMatchCount() + " matches",
                    String.format("%.3f ms", e.getSearchTimeMs())
            });
        }
        lblCount.setText(list.size() + " inquiries recorded");
    }

    private void clearHistory() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear the search history file?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            historyRepository.clearHistory();
            refreshData();
            mainWindow.showNotification("Search history cleared.");
        }
    }

    private void exportHistory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("exported_search_history.txt"));
        chooser.setFileFilter(new FileNameExtensionFilter("Text File (*.txt)", "txt"));

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                File target = chooser.getSelectedFile();
                StringBuilder sb = new StringBuilder();
                List<SearchHistoryEntry> list = historyRepository.loadAll();
                for (SearchHistoryEntry e : list) {
                    sb.append(e.toFileString()).append(System.lineSeparator());
                }
                Files.writeString(target.toPath(), sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                JOptionPane.showMessageDialog(this, "History exported to " + target.getName(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
