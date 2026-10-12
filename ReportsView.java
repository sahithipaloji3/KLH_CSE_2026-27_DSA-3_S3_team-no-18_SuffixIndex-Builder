package com.suffixindex.ui;

import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Reports View: Generates, displays, and exports comprehensive technical reports
 * on documents and their suffix indices to data/reports/.
 */
public class ReportsView extends JPanel {

    private final ReportService reportService;
    private final DocumentService documentService;
    private final MainWindow mainWindow;

    private final JComboBox<DocumentItem> comboDocuments = new JComboBox<>();
    private final ModernButton btnGenerate = UITheme.createPrimaryButton("GENERATE REPORT",
            new ModernIcon(ModernIcon.IconType.REPORTS, 16, Color.WHITE));
    private final ModernButton btnSaveDisk = UITheme.createSecondaryButton("Save to data/reports/",
            new ModernIcon(ModernIcon.IconType.DOWNLOAD, 15, UITheme.TEXT_SECONDARY));

    private final JTextArea txtReportArea = new JTextArea();
    private final DefaultListModel<PathItem> savedReportsModel = new DefaultListModel<>();
    private final JList<PathItem> listSavedReports = new JList<>(savedReportsModel);

    private static class DocumentItem {
        final DocumentMetadata meta;

        DocumentItem(DocumentMetadata meta) {
            this.meta = meta;
        }

        @Override
        public String toString() {
            return meta.getDocumentName() + " (" + meta.getDocumentId() + ")";
        }
    }

    private static class PathItem {
        final Path path;

        PathItem(Path path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path.getFileName().toString();
        }
    }

    public ReportsView(ReportService reportService, DocumentService documentService, MainWindow mainWindow) {
        this.reportService = reportService;
        this.documentService = documentService;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
        loadSavedReports();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Comprehensive Document & DSA Reports");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Audit reports with cryptographic SHA-256 integrity, lexical stats, LRS, LCP arrays, and alphabet distribution");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);
        header.add(titleGroup, BorderLayout.WEST);

        // Control Card
        ModernCard controlCard = UITheme.createCardPanel();
        controlCard.setLayout(new BorderLayout(12, 0));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        JLabel lblDoc = new JLabel("Target Document:");
        lblDoc.setFont(UITheme.FONT_SUBHEADER);
        comboDocuments.setFont(UITheme.FONT_BODY);
        leftGroup.add(lblDoc);
        leftGroup.add(comboDocuments);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightGroup.setOpaque(false);

        btnGenerate.addActionListener(e -> generateReportPreview());
        btnSaveDisk.addActionListener(e -> saveReportToDisk());

        rightGroup.add(btnGenerate);
        rightGroup.add(btnSaveDisk);

        controlCard.add(leftGroup, BorderLayout.WEST);
        controlCard.add(rightGroup, BorderLayout.EAST);

        // Top Section
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(controlCard, BorderLayout.SOUTH);

        // Center Split: Left = Saved Reports List, Right = Report Viewer
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(260);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // Left Panel: Saved Reports History
        ModernCard leftPanel = UITheme.createCardPanel();
        leftPanel.setLayout(new BorderLayout(0, 10));

        JLabel lblSaved = new JLabel("Saved Reports");
        lblSaved.setFont(UITheme.FONT_SUBHEADER);
        lblSaved.setForeground(UITheme.TEXT_PRIMARY);

        listSavedReports.setFont(UITheme.FONT_BODY);
        listSavedReports.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSavedReports.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                PathItem item = listSavedReports.getSelectedValue();
                if (item != null) {
                    try {
                        String content = reportService.readReport(item.path);
                        txtReportArea.setText(content);
                        txtReportArea.setCaretPosition(0);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error reading report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(listSavedReports);
        listScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        leftPanel.add(lblSaved, BorderLayout.NORTH);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        // Right Panel: Report Text View
        ModernCard rightPanel = UITheme.createCardPanel();
        rightPanel.setLayout(new BorderLayout(0, 8));

        JLabel lblViewer = new JLabel("Report Viewer");
        lblViewer.setFont(UITheme.FONT_SUBHEADER);
        lblViewer.setForeground(UITheme.TEXT_PRIMARY);

        txtReportArea.setFont(UITheme.FONT_MONO);
        txtReportArea.setEditable(false);
        txtReportArea.setBackground(new Color(248, 250, 252));
        txtReportArea.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane reportScroll = new JScrollPane(txtReportArea);
        reportScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        rightPanel.add(lblViewer, BorderLayout.NORTH);
        rightPanel.add(reportScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(topSection, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
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

    public void loadSavedReports() {
        savedReportsModel.clear();
        try {
            List<Path> list = reportService.listReports();
            for (Path p : list) {
                savedReportsModel.addElement(new PathItem(p));
            }
        } catch (IOException e) {
            System.err.println("Could not load reports list: " + e.getMessage());
        }
    }

    private void generateReportPreview() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a document first.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String content = reportService.generateReportContent(item.meta.getDocumentId());
            txtReportArea.setText(content);
            txtReportArea.setCaretPosition(0);
            mainWindow.showNotification("Report generated for " + item.meta.getDocumentName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Report generation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveReportToDisk() {
        DocumentItem item = (DocumentItem) comboDocuments.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a document first.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Path target = reportService.generateAndSaveReport(item.meta.getDocumentId());
            loadSavedReports();
            mainWindow.showNotification("Report successfully saved to: " + target.getFileName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
