package com.suffixindex.ui;

import com.suffixindex.repository.BenchmarkRepository;
import com.suffixindex.repository.ReportRepository;
import com.suffixindex.repository.SearchHistoryRepository;
import com.suffixindex.repository.SettingsRepository;
import com.suffixindex.service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window featuring a sleek dark sidebar, vector icons,
 * seamless CardLayout view switching, and real-time status notifications.
 */
public class MainWindow extends JFrame {

    private final DocumentService documentService;
    private final IndexService indexService;
    private final SearchService searchService;
    private final SearchHistoryRepository historyRepository;
    private final SettingsRepository settingsRepository;

    private final MultiDocumentSearchService multiSearchService;
    private final AutocompleteService autocompleteService;
    private final ReportService reportService;
    private final BenchmarkService benchmarkService;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentContainer = new JPanel(cardLayout);

    // Sidebar navigation buttons
    private final Map<String, ModernButton> navButtons = new HashMap<>();
    private String currentView = "Dashboard";

    // Core Views
    private DashboardView dashboardView;
    private DocumentView documentView;
    private IndexView indexView;
    private SearchView searchView;
    private SuffixArrayView suffixArrayView;
    private LCPArrayView lcpArrayView;
    private HistoryView historyView;
    private StatisticsView statisticsView;
    private SettingsView settingsView;

    // Advanced DSA & Feature Views
    private MultiSearchView multiSearchView;
    private AutocompleteView autocompleteView;
    private DSAVisualizerView dsaVisualizerView;
    private AlgorithmComparisonView algorithmComparisonView;
    private BenchmarkView benchmarkView;
    private ReportsView reportsView;

    // Toast / Status notification bar
    private final JPanel toastPanel = new JPanel(new BorderLayout());
    private final JLabel toastLabel = new JLabel("System Ready");
    private Timer toastTimer;

    public MainWindow(DocumentService documentService,
                      IndexService indexService,
                      SearchService searchService,
                      SearchHistoryRepository historyRepository,
                      SettingsRepository settingsRepository,
                      MultiDocumentSearchService multiSearchService,
                      AutocompleteService autocompleteService,
                      ReportService reportService,
                      BenchmarkService benchmarkService) {
        this.documentService = documentService;
        this.indexService = indexService;
        this.searchService = searchService;
        this.historyRepository = historyRepository;
        this.settingsRepository = settingsRepository;
        this.multiSearchService = multiSearchService;
        this.autocompleteService = autocompleteService;
        this.reportService = reportService;
        this.benchmarkService = benchmarkService;

        setTitle("Suffix Index Builder — Advanced DSA Text Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);

        initViews();
        initUI();

        // Start on Dashboard
        navigateTo("Dashboard");
    }

    private void initViews() {
        dashboardView = new DashboardView(documentService, historyRepository, this);
        documentView = new DocumentView(documentService, indexService, this);
        indexView = new IndexView(documentService, indexService, this);
        searchView = new SearchView(documentService, searchService, this);
        suffixArrayView = new SuffixArrayView(documentService, indexService, this);
        lcpArrayView = new LCPArrayView(documentService, indexService, this);
        historyView = new HistoryView(historyRepository, this);
        statisticsView = new StatisticsView(this);
        settingsView = new SettingsView(settingsRepository, this);

        multiSearchView = new MultiSearchView(multiSearchService, this);
        autocompleteView = new AutocompleteView(autocompleteService, this);
        dsaVisualizerView = new DSAVisualizerView(this);
        algorithmComparisonView = new AlgorithmComparisonView(documentService, indexService, this);
        benchmarkView = new BenchmarkView(benchmarkService, documentService, this);
        reportsView = new ReportsView(reportService, documentService, this);

        contentContainer.add(dashboardView, "Dashboard");
        contentContainer.add(documentView, "Documents");
        contentContainer.add(indexView, "Build Index");
        contentContainer.add(searchView, "Search");
        contentContainer.add(multiSearchView, "Corpus Search");
        contentContainer.add(autocompleteView, "Autocomplete");
        contentContainer.add(suffixArrayView, "Suffix Array");
        contentContainer.add(lcpArrayView, "LCP Array");
        contentContainer.add(dsaVisualizerView, "DSA Visualizer");
        contentContainer.add(algorithmComparisonView, "Algorithm Test");
        contentContainer.add(benchmarkView, "Benchmark");
        contentContainer.add(reportsView, "Reports");
        contentContainer.add(historyView, "History");
        contentContainer.add(statisticsView, "Statistics");
        contentContainer.add(settingsView, "Settings");
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);

        // 1. Sidebar Panel
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(245, getHeight()));
        sidebar.setBackground(UITheme.BG_SIDEBAR);

        // Sidebar Brand Header
        JPanel brandPanel = new JPanel(new BorderLayout(12, 0));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(18, 16, 16, 16));

        // Monogram Icon badge "SI"
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                // Gradient background
                GradientPaint gp = new GradientPaint(0, 0, new Color(99, 102, 241), 38, 38, new Color(79, 70, 229));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, 38, 38, 10, 10));

                // Monogram text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
                FontMetrics fm = g2.getFontMetrics();
                String txt = "SI";
                int tx = (38 - fm.stringWidth(txt)) / 2;
                int ty = (38 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(txt, tx, ty);

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
            }
        };
        logoBadge.setOpaque(false);

        JPanel brandTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandTextPanel.setOpaque(false);

        JLabel brandTitle = new JLabel("SUFFIX INDEX");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        brandTitle.setForeground(Color.WHITE);

        JLabel brandSubtitle = new JLabel("DSA Text Engine");
        brandSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brandSubtitle.setForeground(UITheme.TEXT_SIDEBAR_MUTED);

        brandTextPanel.add(brandTitle);
        brandTextPanel.add(brandSubtitle);

        brandPanel.add(logoBadge, BorderLayout.WEST);
        brandPanel.add(brandTextPanel, BorderLayout.CENTER);

        // Navigation Items List with Modern Vector Icons
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(4, 10, 8, 10));

        Object[][] navItems = {
                {"Dashboard", ModernIcon.IconType.DASHBOARD},
                {"Documents", ModernIcon.IconType.DOCUMENTS},
                {"Build Index", ModernIcon.IconType.BUILD_INDEX},
                {"Search", ModernIcon.IconType.SEARCH},
                {"Corpus Search", ModernIcon.IconType.MULTI_SEARCH},
                {"Autocomplete", ModernIcon.IconType.AUTOCOMPLETE},
                {"Suffix Array", ModernIcon.IconType.SUFFIX_ARRAY},
                {"LCP Array", ModernIcon.IconType.LCP_ARRAY},
                {"DSA Visualizer", ModernIcon.IconType.VISUALIZER},
                {"Algorithm Test", ModernIcon.IconType.ALGORITHM},
                {"Benchmark", ModernIcon.IconType.BENCHMARK},
                {"Reports", ModernIcon.IconType.REPORTS},
                {"History", ModernIcon.IconType.HISTORY},
                {"Statistics", ModernIcon.IconType.STATISTICS},
                {"Settings", ModernIcon.IconType.SETTINGS}
        };

        for (Object[] item : navItems) {
            String name = (String) item[0];
            ModernIcon.IconType iconType = (ModernIcon.IconType) item[1];
            ModernIcon icon = new ModernIcon(iconType, 17, UITheme.TEXT_SIDEBAR_MUTED);

            ModernButton btn = new ModernButton(name, ModernButton.ButtonStyle.SIDEBAR_NAV, icon);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            btn.addActionListener(e -> navigateTo(name));

            navButtons.put(name, btn);
            navList.add(btn);
            navList.add(Box.createVerticalStrut(3));
        }

        JScrollPane navScroll = new JScrollPane(navList);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setBorder(null);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

        // Sidebar Footer
        JPanel sidebarFooter = new JPanel(new GridLayout(2, 1, 0, 3));
        sidebarFooter.setOpaque(false);
        sidebarFooter.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel lblVer = new JLabel("Java 21 SE • Desktop");
        lblVer.setFont(UITheme.FONT_SMALL);
        lblVer.setForeground(UITheme.TEXT_SIDEBAR_MUTED);

        JLabel lblNoDb = new JLabel("● Pure File I/O • Zero DB");
        lblNoDb.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNoDb.setForeground(new Color(52, 211, 153)); // Emerald 400

        sidebarFooter.add(lblVer);
        sidebarFooter.add(lblNoDb);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(sidebarFooter, BorderLayout.SOUTH);

        // 2. Bottom Toast Bar
        toastPanel.setBackground(new Color(30, 41, 59));
        toastPanel.setBorder(new EmptyBorder(8, 20, 8, 20));
        toastLabel.setFont(UITheme.FONT_SMALL);
        toastLabel.setForeground(new Color(241, 245, 249));
        toastPanel.add(toastLabel, BorderLayout.WEST);

        // Main Center Layout
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.add(contentContainer, BorderLayout.CENTER);
        mainArea.add(toastPanel, BorderLayout.SOUTH);

        root.add(sidebar, BorderLayout.WEST);
        root.add(mainArea, BorderLayout.CENTER);

        setContentPane(root);
    }

    public void navigateTo(String viewName) {
        currentView = viewName;
        cardLayout.show(contentContainer, viewName);

        // Update button active states
        for (Map.Entry<String, ModernButton> entry : navButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(viewName);
            entry.getValue().setActive(isActive);
        }

        // Trigger view refreshes on navigation
        switch (viewName) {
            case "Dashboard":
                dashboardView.refreshData();
                break;
            case "Documents":
                documentView.refreshData();
                break;
            case "Build Index":
                indexView.loadDocuments(null);
                break;
            case "Search":
                searchView.loadDocuments(null);
                break;
            case "Corpus Search":
                // Ready for user query
                break;
            case "Autocomplete":
                autocompleteView.updateTrieTelemetry();
                break;
            case "Suffix Array":
                suffixArrayView.loadDocuments(null);
                break;
            case "LCP Array":
                lcpArrayView.loadDocuments(null);
                break;
            case "DSA Visualizer":
                // Visualizer retains active state or can be re-run
                break;
            case "Algorithm Test":
                algorithmComparisonView.loadDocuments();
                break;
            case "Benchmark":
                benchmarkView.loadDocuments();
                break;
            case "Reports":
                reportsView.loadDocuments();
                reportsView.loadSavedReports();
                break;
            case "History":
                historyView.refreshData();
                break;
            case "Statistics":
                statisticsView.refreshData();
                break;
        }
    }

    public void navigateToIndexWithDoc(String docId) {
        navigateTo("Build Index");
        indexView.loadDocuments(docId);
    }

    public void navigateToSearchWithDoc(String docId) {
        navigateTo("Search");
        searchView.loadDocuments(docId);
    }

    public void navigateToSuffixArrayWithDoc(String docId) {
        navigateTo("Suffix Array");
        suffixArrayView.loadDocuments(docId);
    }

    public void navigateToLCPWithDoc(String docId) {
        navigateTo("LCP Array");
        lcpArrayView.loadDocuments(docId);
    }

    public void showNotification(String message) {
        toastLabel.setText("✓  " + message);
        toastPanel.setBackground(new Color(30, 41, 59));
        if (toastTimer != null && toastTimer.isRunning()) {
            toastTimer.stop();
        }
        toastTimer = new Timer(5000, e -> toastLabel.setText("System Ready"));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }
}
