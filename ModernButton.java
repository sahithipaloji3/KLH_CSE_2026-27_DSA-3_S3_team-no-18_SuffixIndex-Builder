package com.suffixindex.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom modern button component rendered purely via Java 2D.
 * Fixes native Windows LookAndFeel button background and text contrast issues,
 * ensuring rich aesthetics, smooth rounded corners, and crisp hover states.
 */
public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS,
        SIDEBAR_NAV
    }

    private ButtonStyle style = ButtonStyle.PRIMARY;
    private boolean hovered = false;
    private boolean pressed = false;
    private boolean active = false; // For sidebar navigation selection
    private int cornerRadius = 8;
    private Icon leadingIcon = null;

    public ModernButton(String text) {
        this(text, ButtonStyle.PRIMARY, null);
    }

    public ModernButton(String text, ButtonStyle style) {
        this(text, style, null);
    }

    public ModernButton(String text, ButtonStyle style, Icon icon) {
        super(text);
        this.style = style;
        this.leadingIcon = icon;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(UITheme.FONT_BODY_BOLD);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    public void setButtonStyle(ButtonStyle style) {
        this.style = style;
        repaint();
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    public void setLeadingIcon(Icon icon) {
        this.leadingIcon = icon;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        Color bgColor;
        Color borderColor = null;
        Color textColor;

        switch (style) {
            case PRIMARY:
                textColor = Color.WHITE;
                if (!isEnabled()) {
                    bgColor = new Color(199, 210, 254);
                } else if (pressed) {
                    bgColor = new Color(55, 48, 163); // Indigo 800
                } else if (hovered) {
                    bgColor = new Color(67, 56, 202); // Indigo 700
                } else {
                    bgColor = new Color(79, 70, 229); // Indigo 600
                }
                break;

            case SECONDARY:
                textColor = new Color(30, 41, 59); // Slate 800
                borderColor = hovered ? new Color(148, 163, 184) : new Color(203, 213, 225);
                if (pressed) {
                    bgColor = new Color(226, 232, 240);
                } else if (hovered) {
                    bgColor = new Color(241, 245, 249);
                } else {
                    bgColor = Color.WHITE;
                }
                break;

            case DANGER:
                textColor = Color.WHITE;
                if (pressed) {
                    bgColor = new Color(185, 28, 28); // Red 700
                } else if (hovered) {
                    bgColor = new Color(220, 38, 38); // Red 600
                } else {
                    bgColor = new Color(239, 68, 68); // Red 500
                }
                break;

            case SUCCESS:
                textColor = Color.WHITE;
                if (pressed) {
                    bgColor = new Color(4, 120, 87); // Emerald 700
                } else if (hovered) {
                    bgColor = new Color(5, 150, 105); // Emerald 600
                } else {
                    bgColor = new Color(16, 185, 129); // Emerald 500
                }
                break;

            case SIDEBAR_NAV:
            default:
                if (active) {
                    bgColor = new Color(51, 65, 85); // Slate 700 active
                    textColor = Color.WHITE;
                } else if (hovered) {
                    bgColor = new Color(30, 41, 59); // Slate 800 hover
                    textColor = new Color(241, 245, 249);
                } else {
                    bgColor = new Color(15, 23, 42, 0); // Transparent
                    textColor = new Color(148, 163, 184); // Slate 400
                }
                break;
        }

        // Fill background
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // Draw border if secondary
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, cornerRadius, cornerRadius));
        }

        // Draw active accent bar on left for sidebar navigation
        if (style == ButtonStyle.SIDEBAR_NAV && active) {
            g2.setColor(new Color(129, 140, 248)); // Indigo 400
            g2.fill(new RoundRectangle2D.Float(0, 4, 4, h - 8, 3, 3));
        }

        // Draw Icon and Text
        FontMetrics fm = g2.getFontMetrics(getFont());
        String text = getText();
        int iconWidth = (leadingIcon != null) ? leadingIcon.getIconWidth() + 10 : 0;
        int textWidth = fm.stringWidth(text);
        int totalWidth = iconWidth + textWidth;

        int x;
        if (style == ButtonStyle.SIDEBAR_NAV) {
            x = 16; // Left aligned for sidebar
        } else {
            x = Math.max(12, (w - totalWidth) / 2); // Center aligned
        }

        if (leadingIcon != null) {
            if (leadingIcon instanceof ModernIcon) {
                ((ModernIcon) leadingIcon).setColor(textColor);
            }
            int iconY = (h - leadingIcon.getIconHeight()) / 2;
            leadingIcon.paintIcon(this, g2, x, iconY);
            x += leadingIcon.getIconWidth() + 10;
        }

        g2.setFont(getFont());
        g2.setColor(textColor);
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int iconExtra = (leadingIcon != null) ? leadingIcon.getIconWidth() + 12 : 0;
        return new Dimension(Math.max(d.width + 28 + iconExtra, 90), Math.max(d.height + 12, 36));
    }
}
