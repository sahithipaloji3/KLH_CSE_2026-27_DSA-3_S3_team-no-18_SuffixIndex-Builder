package com.suffixindex.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern Card panel with smooth rounded corners, clean border, and optional top accent stripe.
 */
public class ModernCard extends JPanel {

    private int cornerRadius = 12;
    private Color topAccentColor = null;
    private int topAccentHeight = 4;
    private Color cardBackground = Color.WHITE;
    private Color borderColor = new Color(226, 232, 240); // Slate 200

    public ModernCard() {
        this(12, null);
    }

    public ModernCard(int cornerRadius, Color topAccentColor) {
        this.cornerRadius = cornerRadius;
        this.topAccentColor = topAccentColor;
        setOpaque(false);
        setBorder(new EmptyBorder(16, 18, 16, 18));
    }

    public void setTopAccentColor(Color color) {
        this.topAccentColor = color;
        repaint();
    }

    public void setCardBackground(Color color) {
        this.cardBackground = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Fill card background with rounded rect
        g2.setColor(cardBackground);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // 2. Draw top accent strip if specified
        if (topAccentColor != null && topAccentHeight > 0) {
            g2.setColor(topAccentColor);
            // Draw top rounded bar using clipping or path
            Shape oldClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
            g2.fillRect(0, 0, w, topAccentHeight);
            g2.setClip(oldClip);
        }

        // 3. Draw subtle border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, cornerRadius, cornerRadius));

        g2.dispose();
        super.paintComponent(g);
    }
}
