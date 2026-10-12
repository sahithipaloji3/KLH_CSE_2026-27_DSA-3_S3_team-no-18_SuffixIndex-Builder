package com.suffixindex.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern design system and styling tokens for Suffix Index Builder desktop GUI.
 */
public class UITheme {

    // Palette Colors
    public static final Color BG_SIDEBAR = new Color(15, 23, 42);       // Midnight Slate (Slate 900)
    public static final Color BG_SIDEBAR_HOVER = new Color(30, 41, 59); // Slate 800
    public static final Color BG_SIDEBAR_ACTIVE = new Color(51, 65, 85); // Slate 700
    public static final Color TEXT_SIDEBAR = new Color(226, 232, 240);  // Slate 200
    public static final Color TEXT_SIDEBAR_MUTED = new Color(148, 163, 184);

    public static final Color BG_MAIN = new Color(248, 250, 252);       // Slate 50
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(226, 232, 240);  // Slate 200
    public static final Color BORDER_CARD = new Color(203, 213, 225);   // Slate 300

    public static final Color PRIMARY = new Color(79, 70, 229);         // Indigo 600
    public static final Color PRIMARY_HOVER = new Color(67, 56, 202);   // Indigo 700
    public static final Color PRIMARY_LIGHT = new Color(238, 242, 255); // Indigo 50

    public static final Color SUCCESS = new Color(16, 185, 129);        // Emerald 500
    public static final Color SUCCESS_BG = new Color(236, 253, 245);    // Emerald 50
    public static final Color SUCCESS_TEXT = new Color(6, 95, 70);

    public static final Color WARNING = new Color(245, 158, 11);        // Amber 500
    public static final Color WARNING_BG = new Color(254, 243, 199);    // Amber 50
    public static final Color WARNING_TEXT = new Color(146, 64, 14);

    public static final Color DANGER = new Color(239, 68, 68);          // Red 500
    public static final Color DANGER_BG = new Color(254, 242, 242);     // Red 50
    public static final Color DANGER_TEXT = new Color(153, 27, 27);

    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);     // Slate 900
    public static final Color TEXT_SECONDARY = new Color(71, 85, 105);  // Slate 600
    public static final Color TEXT_MUTED = new Color(148, 163, 184);    // Slate 400

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_MONO_BOLD = new Font("Consolas", Font.BOLD, 13);

    /**
     * Creates a styled card container panel with rounded corners.
     */
    public static ModernCard createCardPanel() {
        return new ModernCard(12, null);
    }

    /**
     * Creates a styled card with a top accent bar.
     */
    public static ModernCard createCardPanelWithAccent(Color accentColor) {
        return new ModernCard(12, accentColor);
    }

    /**
     * Creates a custom primary button with guaranteed contrast on all platforms.
     */
    public static ModernButton createPrimaryButton(String text) {
        return new ModernButton(text, ModernButton.ButtonStyle.PRIMARY);
    }

    public static ModernButton createPrimaryButton(String text, Icon icon) {
        return new ModernButton(text, ModernButton.ButtonStyle.PRIMARY, icon);
    }

    /**
     * Creates a custom secondary button.
     */
    public static ModernButton createSecondaryButton(String text) {
        return new ModernButton(text, ModernButton.ButtonStyle.SECONDARY);
    }

    public static ModernButton createSecondaryButton(String text, Icon icon) {
        return new ModernButton(text, ModernButton.ButtonStyle.SECONDARY, icon);
    }

    /**
     * Creates a custom danger button.
     */
    public static ModernButton createDangerButton(String text) {
        return new ModernButton(text, ModernButton.ButtonStyle.DANGER);
    }

    public static ModernButton createDangerButton(String text, Icon icon) {
        return new ModernButton(text, ModernButton.ButtonStyle.DANGER, icon);
    }

    /**
     * Styles a standard JTable with modern headers, padding, and row colors.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(34);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(PRIMARY);
        table.setGridColor(new Color(241, 245, 249));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        // Header styling
        table.getTableHeader().setFont(FONT_SUBHEADER);
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getWidth(), 38));

        // Default cell renderer with padding
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(4, 14, 4, 14));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_PRIMARY);
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }
}
