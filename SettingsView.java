package com.suffixindex.ui;

import com.suffixindex.model.AppSettings;
import com.suffixindex.repository.SettingsRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Application Settings View: allows editing parameters saved to data/settings/application.conf.
 */
public class SettingsView extends JPanel {

    private final SettingsRepository settingsRepository;
    private final MainWindow mainWindow;

    private final JSpinner spnPageSize = new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10));
    private final JCheckBox chkCaseSensitive = new JCheckBox("Default searches to case-sensitive", true);
    private final JSpinner spnContextLen = new JSpinner(new SpinnerNumberModel(35, 10, 100, 5));
    private final JComboBox<String> comboTheme = new JComboBox<>(new String[]{"LIGHT", "DARK"});
    private final JCheckBox chkAutoRebuild = new JCheckBox("Automatically prompt to rebuild outdated indexes", true);

    public SettingsView(SettingsRepository settingsRepository, MainWindow mainWindow) {
        this.settingsRepository = settingsRepository;
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
        loadSettingsToForm();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("System Settings & Preferences");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Configuration saved in data/settings/application.conf via Java File I/O");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        header.add(titleGroup, BorderLayout.WEST);

        // Center Settings Card
        ModernCard card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 20));

        JPanel formGrid = new JPanel(new GridLayout(5, 2, 20, 16));
        formGrid.setOpaque(false);

        formGrid.add(createLabel("Default Table Page Size:", "Controls rows loaded per page in Suffix Array visualizer"));
        formGrid.add(spnPageSize);

        formGrid.add(createLabel("Default Search Case Sensitivity:", "Determines whether pattern search defaults to case-sensitive"));
        formGrid.add(chkCaseSensitive);
        chkCaseSensitive.setOpaque(false);

        formGrid.add(createLabel("Snippet Preview Context Length:", "Number of characters to display before and after matched substring"));
        formGrid.add(spnContextLen);

        formGrid.add(createLabel("UI Color Theme:", "Visual appearance theme for desktop application window"));
        formGrid.add(comboTheme);

        formGrid.add(createLabel("Outdated Index Prompting:", "Alerts user immediately when document text changes"));
        formGrid.add(chkAutoRebuild);
        chkAutoRebuild.setOpaque(false);

        // Storage Information box
        ModernCard storageInfo = new ModernCard(10, null);
        storageInfo.setLayout(new BorderLayout(0, 8));
        storageInfo.setCardBackground(new Color(248, 250, 252));

        JLabel infoTitle = new JLabel("📁 File Persistence Directory Structure");
        infoTitle.setFont(UITheme.FONT_SUBHEADER);
        JLabel infoDetails = new JLabel("<html>All data is 100% persisted on local disk:<br>"
                + "• Documents: <code>data/documents/*.txt</code><br>"
                + "• Suffix & LCP Indexes: <code>data/indexes/*.index</code><br>"
                + "• Metadata: <code>data/metadata/documents.meta</code><br>"
                + "• Search History: <code>data/searches/search_history.txt</code><br>"
                + "• Settings: <code>data/settings/application.conf</code></html>");
        infoDetails.setFont(UITheme.FONT_BODY);
        storageInfo.add(infoTitle, BorderLayout.NORTH);
        storageInfo.add(infoDetails, BorderLayout.CENTER);

        // Save Button Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnBar.setOpaque(false);

        ModernButton btnReset = UITheme.createSecondaryButton("Restore Defaults");
        btnReset.addActionListener(e -> {
            AppSettings d = new AppSettings();
            applyToForm(d);
        });

        ModernButton btnSave = UITheme.createPrimaryButton("Save Configuration",
                new ModernIcon(ModernIcon.IconType.CHECK, 16, Color.WHITE));
        btnSave.addActionListener(e -> saveSettingsFromForm());

        btnBar.add(btnReset);
        btnBar.add(btnSave);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 20));
        centerContainer.setOpaque(false);
        centerContainer.add(formGrid, BorderLayout.NORTH);
        centerContainer.add(storageInfo, BorderLayout.CENTER);
        centerContainer.add(btnBar, BorderLayout.SOUTH);

        card.add(centerContainer, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    private JPanel createLabel(String title, String desc) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_BODY_BOLD);
        t.setForeground(UITheme.TEXT_PRIMARY);

        JLabel d = new JLabel(desc);
        d.setFont(UITheme.FONT_SMALL);
        d.setForeground(UITheme.TEXT_MUTED);

        p.add(t);
        p.add(d);
        return p;
    }

    private void loadSettingsToForm() {
        AppSettings s = settingsRepository.loadSettings();
        applyToForm(s);
    }

    private void applyToForm(AppSettings s) {
        spnPageSize.setValue(s.getPageSize());
        chkCaseSensitive.setSelected(s.isCaseSensitive());
        spnContextLen.setValue(s.getPreviewContextLength());
        comboTheme.setSelectedItem(s.getTheme());
        chkAutoRebuild.setSelected(s.isAutoRebuildOutdated());
    }

    private void saveSettingsFromForm() {
        AppSettings s = new AppSettings();
        s.setPageSize((Integer) spnPageSize.getValue());
        s.setCaseSensitive(chkCaseSensitive.isSelected());
        s.setPreviewContextLength((Integer) spnContextLen.getValue());
        s.setTheme((String) comboTheme.getSelectedItem());
        s.setAutoRebuildOutdated(chkAutoRebuild.isSelected());

        settingsRepository.saveSettings(s);
        mainWindow.showNotification("Settings saved to data/settings/application.conf.");
    }
}
