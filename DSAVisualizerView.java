package com.suffixindex.ui;

import com.suffixindex.algorithm.LCPArrayBuilder;
import com.suffixindex.algorithm.SuffixArrayBuilder;
import com.suffixindex.model.SuffixArrayStep;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DSA Step-by-Step Visualizer: Interactive visualizer for Prefix Doubling and Kasai's LCP algorithm.
 */
public class DSAVisualizerView extends JPanel {

    private final MainWindow mainWindow;

    private final JTextField txtInput = new JTextField("banana");
    private final JComboBox<String> comboPresets = new JComboBox<>(new String[]{
            "banana", "mississippi", "abracadabra", "suffixarray", "panamabanana"
    });

    private final ModernButton btnVisualize = UITheme.createPrimaryButton("START VISUALIZATION",
            new ModernIcon(ModernIcon.IconType.VISUALIZER, 16, Color.WHITE));

    // Stepping controls
    private final ModernButton btnReset = UITheme.createSecondaryButton("Reset", null);
    private final ModernButton btnPrev = UITheme.createSecondaryButton("◀ Prev", null);
    private final ModernButton btnNext = UITheme.createPrimaryButton("Next ▶", null);
    private final ModernButton btnPlay = UITheme.createSecondaryButton("▶ Play",
            new ModernIcon(ModernIcon.IconType.PLAY, 14, UITheme.TEXT_SECONDARY));

    private final JLabel lblStepInfo = new JLabel("Step 0 of 0");
    private final JLabel lblKValue = new JLabel("k = -");
    private final JTextArea txtExplanation = new JTextArea();

    // Table
    private final DefaultTableModel stepTableModel = new DefaultTableModel(
            new String[]{"i", "SA[i]", "Suffix String", "Rank Pair (r1, r2)", "New Rank"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable stepTable = new JTable(stepTableModel);

    // Kasai LCP Summary Table
    private final DefaultTableModel lcpTableModel = new DefaultTableModel(
            new String[]{"i", "SA[i]", "LCP[i]", "Suffix Preview", "Kasai Amortized Invariant"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable lcpTable = new JTable(lcpTableModel);

    private List<SuffixArrayStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private String currentText = "banana";
    private Timer playTimer;

    public DSAVisualizerView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
        startVisualization();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("DSA Algorithm Step-by-Step Visualizer");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Interactive step-through of Prefix Doubling O(n log n) and Kasai's Linear O(n) LCP Construction");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);
        header.add(titleGroup, BorderLayout.WEST);

        // Input Card
        ModernCard inputCard = UITheme.createCardPanel();
        inputCard.setLayout(new BorderLayout(0, 10));

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);

        JLabel lblInput = new JLabel("Input String:");
        lblInput.setFont(UITheme.FONT_SUBHEADER);
        txtInput.setFont(UITheme.FONT_MONO);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        JLabel lblPreset = new JLabel("Presets:");
        lblPreset.setFont(UITheme.FONT_BODY);
        comboPresets.setFont(UITheme.FONT_BODY);
        comboPresets.addActionListener(e -> {
            String selected = (String) comboPresets.getSelectedItem();
            if (selected != null) {
                txtInput.setText(selected);
                startVisualization();
            }
        });

        btnVisualize.addActionListener(e -> startVisualization());

        rightControls.add(lblPreset);
        rightControls.add(comboPresets);
        rightControls.add(btnVisualize);

        inputRow.add(lblInput, BorderLayout.WEST);
        inputRow.add(txtInput, BorderLayout.CENTER);
        inputRow.add(rightControls, BorderLayout.EAST);

        inputCard.add(inputRow, BorderLayout.CENTER);

        // Stepper Navigation Card
        ModernCard stepperCard = UITheme.createCardPanel();
        stepperCard.setLayout(new BorderLayout(12, 10));

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navButtons.setOpaque(false);

        btnReset.addActionListener(e -> goToStep(0));
        btnPrev.addActionListener(e -> goToStep(currentStepIndex - 1));
        btnNext.addActionListener(e -> goToStep(currentStepIndex + 1));

        btnPlay.addActionListener(e -> togglePlay());

        lblStepInfo.setFont(UITheme.FONT_SUBHEADER);
        lblStepInfo.setForeground(UITheme.PRIMARY);

        lblKValue.setFont(UITheme.FONT_BODY_BOLD);
        lblKValue.setForeground(new Color(139, 92, 246));

        navButtons.add(btnReset);
        navButtons.add(btnPrev);
        navButtons.add(btnNext);
        navButtons.add(btnPlay);
        navButtons.add(Box.createHorizontalStrut(16));
        navButtons.add(lblStepInfo);
        navButtons.add(Box.createHorizontalStrut(10));
        navButtons.add(lblKValue);

        stepperCard.add(navButtons, BorderLayout.NORTH);

        // Explanation text area inside stepperCard
        txtExplanation.setEditable(false);
        txtExplanation.setLineWrap(true);
        txtExplanation.setWrapStyleWord(true);
        txtExplanation.setFont(UITheme.FONT_BODY);
        txtExplanation.setBackground(new Color(248, 250, 252));
        txtExplanation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                new EmptyBorder(8, 12, 8, 12)
        ));
        stepperCard.add(txtExplanation, BorderLayout.CENTER);

        // Tabbed Panel: Tab 1 = Prefix Doubling Steps, Tab 2 = Kasai LCP Array
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_SUBHEADER);

        // Tab 1: Steps Table
        UITheme.styleTable(stepTable);
        stepTable.getColumnModel().getColumn(0).setMaxWidth(60);
        stepTable.getColumnModel().getColumn(1).setMaxWidth(100);
        stepTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        stepTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        stepTable.getColumnModel().getColumn(4).setMaxWidth(120);

        JScrollPane stepScroll = new JScrollPane(stepTable);
        stepScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel stepTab = new JPanel(new BorderLayout());
        stepTab.setOpaque(false);
        stepTab.add(stepScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Prefix Doubling Iterations", stepTab);

        // Tab 2: Kasai LCP Table
        UITheme.styleTable(lcpTable);
        lcpTable.getColumnModel().getColumn(0).setMaxWidth(60);
        lcpTable.getColumnModel().getColumn(1).setMaxWidth(100);
        lcpTable.getColumnModel().getColumn(2).setMaxWidth(100);
        lcpTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        lcpTable.getColumnModel().getColumn(4).setPreferredWidth(280);

        JScrollPane lcpScroll = new JScrollPane(lcpTable);
        lcpScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel lcpTab = new JPanel(new BorderLayout(0, 10));
        lcpTab.setOpaque(false);
        lcpTab.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel lblKasaiTheory = new JLabel("Kasai's Invariant: If suffix i has LCP h > 0 with its predecessor in SA, then suffix i+1 has LCP >= h - 1.");
        lblKasaiTheory.setFont(UITheme.FONT_BODY_BOLD);
        lblKasaiTheory.setForeground(UITheme.SUCCESS);

        lcpTab.add(lblKasaiTheory, BorderLayout.NORTH);
        lcpTab.add(lcpScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Kasai LCP Construction O(n)", lcpTab);

        // Assemble View
        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);
        topContainer.add(header, BorderLayout.NORTH);
        topContainer.add(inputCard, BorderLayout.SOUTH);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 12));
        centerContainer.setOpaque(false);
        centerContainer.add(stepperCard, BorderLayout.NORTH);
        centerContainer.add(tabbedPane, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);
        add(centerContainer, BorderLayout.CENTER);
    }

    public void startVisualization() {
        stopPlay();
        String text = txtInput.getText();
        if (text == null || text.trim().isEmpty()) {
            text = "banana";
            txtInput.setText(text);
        }

        currentText = text;
        steps = SuffixArrayBuilder.buildSuffixArrayWithSteps(text);
        currentStepIndex = 0;

        // Populate Kasai LCP tab once for the text
        int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
        int[] lcp = LCPArrayBuilder.buildLCPArray(text, sa);

        lcpTableModel.setRowCount(0);
        for (int i = 0; i < sa.length; i++) {
            int pos = sa[i];
            int lcpVal = lcp[i];
            String preview = text.substring(pos, Math.min(text.length(), pos + 30));
            String invariant = (i == 0) ? "Base case: LCP[0] = 0 (no predecessor in SA)" :
                    String.format("Common prefix length with SA[%d] ('%s')", i - 1,
                            text.substring(sa[i-1], Math.min(text.length(), sa[i-1] + lcpVal)));

            lcpTableModel.addRow(new Object[]{
                    i, pos, lcpVal, preview, invariant
            });
        }

        renderCurrentStep();
    }

    private void renderCurrentStep() {
        if (steps == null || steps.isEmpty()) return;

        SuffixArrayStep step = steps.get(currentStepIndex);
        lblStepInfo.setText(String.format("Step %d of %d", step.getStepNumber(), steps.size() - 1));
        lblKValue.setText(step.getK() > 0 ? String.format("k = %d (comparing %d-char prefixes)", step.getK(), 2 * step.getK()) : "Base Character Step");
        txtExplanation.setText(step.getDescription());

        btnPrev.setEnabled(currentStepIndex > 0);
        btnNext.setEnabled(currentStepIndex < steps.size() - 1);

        stepTableModel.setRowCount(0);
        List<SuffixArrayStep.TupleInfo> tuples = step.getTuples();

        for (int i = 0; i < tuples.size(); i++) {
            SuffixArrayStep.TupleInfo t = tuples.get(i);
            int pos = t.getSuffixIndex();
            String sub = currentText.substring(pos, Math.min(currentText.length(), pos + 30));

            stepTableModel.addRow(new Object[]{
                    i,
                    pos,
                    sub,
                    t.getRankPairString(),
                    t.getNewRank()
            });
        }
    }

    private void goToStep(int index) {
        if (index >= 0 && index < steps.size()) {
            currentStepIndex = index;
            renderCurrentStep();
        }
    }

    private void togglePlay() {
        if (playTimer != null && playTimer.isRunning()) {
            stopPlay();
        } else {
            btnPlay.setText("⏸ Pause");
            playTimer = new Timer(1200, e -> {
                if (currentStepIndex < steps.size() - 1) {
                    goToStep(currentStepIndex + 1);
                } else {
                    stopPlay();
                }
            });
            playTimer.start();
        }
    }

    private void stopPlay() {
        if (playTimer != null) {
            playTimer.stop();
            playTimer = null;
        }
        btnPlay.setText("▶ Play");
    }
}
