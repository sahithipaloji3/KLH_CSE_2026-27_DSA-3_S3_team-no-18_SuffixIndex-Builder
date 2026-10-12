package com.suffixindex.ui;

import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.IndexService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Document Management View: Create, Import, View, Edit, and Delete documents.
 */
public class DocumentView extends JPanel {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final MainWindow mainWindow;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Document Name", "Characters", "Index Status", "Last Modified"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(tableModel);

    public DocumentView(DocumentService documentService, IndexService indexService, MainWindow mainWindow) {
        this.documentService = documentService;
        this.indexService = indexService;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // Header Panel
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Document Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Create, import, edit, and organize text documents stored on disk");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        // Action Buttons
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        ModernButton btnNew = UITheme.createPrimaryButton("+ New Document",
                new ModernIcon(ModernIcon.IconType.PLUS, 16, Color.WHITE));
        btnNew.addActionListener(e -> showNewDocumentDialog());

        ModernButton btnImport = UITheme.createSecondaryButton("Import TXT",
                new ModernIcon(ModernIcon.IconType.DOCUMENTS, 16, UITheme.TEXT_PRIMARY));
        btnImport.addActionListener(e -> importTxtFile());

        ModernButton btnRefresh = UITheme.createSecondaryButton("Refresh",
                new ModernIcon(ModernIcon.IconType.REFRESH, 15, UITheme.TEXT_SECONDARY));
        btnRefresh.addActionListener(e -> refreshData());

        actionButtons.add(btnNew);
        actionButtons.add(btnImport);
        actionButtons.add(btnRefresh);

        header.add(titleGroup, BorderLayout.WEST);
        header.add(actionButtons, BorderLayout.EAST);

        // Center Card Panel containing Table + Action toolbar
        ModernCard centerCard = UITheme.createCardPanel();
        centerCard.setLayout(new BorderLayout(0, 12));

        UITheme.styleTable(table);

        // Custom status badge renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String statusStr = value != null ? value.toString() : "";
                Color bg;
                Color fg;

                if ("Indexed".equalsIgnoreCase(statusStr)) {
                    bg = UITheme.SUCCESS_BG;
                    fg = UITheme.SUCCESS_TEXT;
                } else if ("Outdated".equalsIgnoreCase(statusStr)) {
                    bg = UITheme.WARNING_BG;
                    fg = UITheme.WARNING_TEXT;
                } else {
                    bg = new Color(241, 245, 249);
                    fg = UITheme.TEXT_SECONDARY;
                }

                JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (!isSelected) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(bg);
                            g2.fill(new RoundRectangle2D.Float(8, 4, getWidth() - 16, getHeight() - 8, 8, 8));
                            g2.dispose();
                        }
                    }
                };
                badgePanel.setOpaque(isSelected);
                if (isSelected) {
                    badgePanel.setBackground(t.getSelectionBackground());
                }

                JLabel l = new JLabel(statusStr);
                l.setFont(UITheme.FONT_BODY_BOLD);
                l.setForeground(isSelected ? t.getSelectionForeground() : fg);
                l.setBorder(new EmptyBorder(0, 16, 0, 0));
                badgePanel.add(l);

                return badgePanel;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        // Bottom Operations Bar for Selected Document
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bottomBar.setOpaque(false);

        ModernButton btnViewEdit = UITheme.createPrimaryButton("View / Edit Content",
                new ModernIcon(ModernIcon.IconType.FILE_TEXT, 16, Color.WHITE));
        btnViewEdit.addActionListener(e -> openSelectedDocumentEditor());

        ModernButton btnBuildIndex = UITheme.createSecondaryButton("Build / Rebuild Index",
                new ModernIcon(ModernIcon.IconType.BUILD_INDEX, 16, UITheme.PRIMARY));
        btnBuildIndex.addActionListener(e -> buildIndexForSelected());

        ModernButton btnSearchThis = UITheme.createSecondaryButton("Search in Document",
                new ModernIcon(ModernIcon.IconType.SEARCH, 16, UITheme.PRIMARY));
        btnSearchThis.addActionListener(e -> searchSelectedDocument());

        ModernButton btnDelete = UITheme.createDangerButton("Delete Document",
                new ModernIcon(ModernIcon.IconType.TRASH, 16, Color.WHITE));
        btnDelete.addActionListener(e -> deleteSelectedDocument());

        bottomBar.add(btnViewEdit);
        bottomBar.add(btnBuildIndex);
        bottomBar.add(btnSearchThis);
        bottomBar.add(btnDelete);

        centerCard.add(scrollPane, BorderLayout.CENTER);
        centerCard.add(bottomBar, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(centerCard, BorderLayout.CENTER);
    }

    private String getSelectedDocId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return (String) table.getValueAt(row, 0);
    }

    private void showNewDocumentDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create New Document", true);
        dialog.setLayout(new BorderLayout(16, 16));
        dialog.setSize(620, 480);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.BG_MAIN);
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));

        ModernCard card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 14));

        JPanel namePanel = new JPanel(new BorderLayout(10, 0));
        namePanel.setOpaque(false);
        JLabel lblName = new JLabel("Document Name:");
        lblName.setFont(UITheme.FONT_SUBHEADER);
        JTextField txtName = new JTextField();
        txtName.setFont(UITheme.FONT_BODY);
        namePanel.add(lblName, BorderLayout.WEST);
        namePanel.add(txtName, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 6));
        contentPanel.setOpaque(false);
        JLabel lblContent = new JLabel("Document Content:");
        lblContent.setFont(UITheme.FONT_SUBHEADER);
        JTextArea txtContent = new JTextArea();
        txtContent.setFont(UITheme.FONT_MONO);
        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtContent);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        contentPanel.add(lblContent, BorderLayout.NORTH);
        contentPanel.add(scroll, BorderLayout.CENTER);

        card.add(namePanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        ModernButton btnSave = UITheme.createPrimaryButton("Save Document",
                new ModernIcon(ModernIcon.IconType.CHECK, 16, Color.WHITE));
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a document name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                documentService.createDocument(name, txtContent.getText());
                dialog.dispose();
                refreshData();
                mainWindow.showNotification("Document created and saved to disk.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating document: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        panel.add(card, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void importTxtFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a Text Document (.txt) to Import");
        chooser.setFileFilter(new FileNameExtensionFilter("Plain Text Files (*.txt)", "txt"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                DocumentMetadata meta = documentService.importDocument(selectedFile.toPath());
                refreshData();
                mainWindow.showNotification("Imported '" + meta.getDocumentName() + "' (" + meta.getCharacterCount() + " chars).");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to import file: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSelectedDocumentEditor() {
        String docId = getSelectedDocId();
        if (docId == null) {
            JOptionPane.showMessageDialog(this, "Please select a document from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Document doc = documentService.getDocument(docId);
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Document: " + doc.getName(), true);
            dialog.setLayout(new BorderLayout(16, 16));
            dialog.setSize(680, 520);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout(14, 14));
            panel.setBackground(UITheme.BG_MAIN);
            panel.setBorder(new EmptyBorder(22, 22, 22, 22));

            ModernCard card = UITheme.createCardPanel();
            card.setLayout(new BorderLayout(0, 14));

            JPanel namePanel = new JPanel(new BorderLayout(10, 0));
            namePanel.setOpaque(false);
            JLabel lblName = new JLabel("Document Name:");
            lblName.setFont(UITheme.FONT_SUBHEADER);
            JTextField txtName = new JTextField(doc.getName());
            txtName.setFont(UITheme.FONT_BODY);
            namePanel.add(lblName, BorderLayout.WEST);
            namePanel.add(txtName, BorderLayout.CENTER);

            JPanel contentPanel = new JPanel(new BorderLayout(0, 6));
            contentPanel.setOpaque(false);
            JLabel lblContent = new JLabel("Content (" + doc.length() + " characters):");
            lblContent.setFont(UITheme.FONT_SUBHEADER);
            JTextArea txtContent = new JTextArea(doc.getContent());
            txtContent.setFont(UITheme.FONT_MONO);
            txtContent.setLineWrap(true);
            txtContent.setWrapStyleWord(true);
            JScrollPane scroll = new JScrollPane(txtContent);
            scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
            contentPanel.add(lblContent, BorderLayout.NORTH);
            contentPanel.add(scroll, BorderLayout.CENTER);

            JPanel statusBanner = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusBanner.setBackground(UITheme.PRIMARY_LIGHT);
            JLabel lblNotice = new JLabel("ℹ️ Note: Modifying document content will immediately mark any existing suffix index as OUTDATED.");
            lblNotice.setFont(UITheme.FONT_SMALL);
            lblNotice.setForeground(UITheme.PRIMARY);
            statusBanner.add(lblNotice);

            card.add(namePanel, BorderLayout.NORTH);
            card.add(contentPanel, BorderLayout.CENTER);
            card.add(statusBanner, BorderLayout.SOUTH);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnPanel.setOpaque(false);

            ModernButton btnCancel = UITheme.createSecondaryButton("Cancel");
            btnCancel.addActionListener(e -> dialog.dispose());

            ModernButton btnSave = UITheme.createPrimaryButton("Save Changes",
                    new ModernIcon(ModernIcon.IconType.CHECK, 16, Color.WHITE));
            btnSave.addActionListener(e -> {
                try {
                    documentService.updateDocument(docId, txtName.getText(), txtContent.getText());
                    dialog.dispose();
                    refreshData();
                    mainWindow.showNotification("Document updated. Index marked OUTDATED.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error saving document: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnPanel.add(btnCancel);
            btnPanel.add(btnSave);

            panel.add(card, BorderLayout.CENTER);
            panel.add(btnPanel, BorderLayout.SOUTH);

            dialog.setContentPane(panel);
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening document: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildIndexForSelected() {
        String docId = getSelectedDocId();
        if (docId == null) {
            JOptionPane.showMessageDialog(this, "Please select a document first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        mainWindow.navigateToIndexWithDoc(docId);
    }

    private void searchSelectedDocument() {
        String docId = getSelectedDocId();
        if (docId == null) {
            JOptionPane.showMessageDialog(this, "Please select a document first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        mainWindow.navigateToSearchWithDoc(docId);
    }

    private void deleteSelectedDocument() {
        String docId = getSelectedDocId();
        if (docId == null) {
            JOptionPane.showMessageDialog(this, "Please select a document to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this document?\nThis will permanently remove the document text file, index file, and metadata from disk.",
                "Confirm Document Deletion",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = documentService.deleteDocument(docId);
                if (deleted) {
                    refreshData();
                    mainWindow.showNotification("Document " + docId + " deleted from disk.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting document: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshData() {
        try {
            tableModel.setRowCount(0);
            List<DocumentMetadata> docs = documentService.getAllDocuments();
            for (DocumentMetadata doc : docs) {
                tableModel.addRow(new Object[]{
                        doc.getDocumentId(),
                        doc.getDocumentName(),
                        String.format("%,d", doc.getCharacterCount()),
                        doc.getIndexStatus().getDisplayName(),
                        com.suffixindex.util.DateUtils.formatEpoch(doc.getLastModified())
                });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading documents from metadata: " + e.getMessage(), "Storage Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
