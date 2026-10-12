package com.suffixindex.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * High-definition vector icons drawn purely with Java 2D Graphics.
 * Guaranteed 100% crisp rendering on all platforms without missing glyph boxes or font dependencies.
 */
public class ModernIcon implements Icon {

    public enum IconType {
        DASHBOARD,
        DOCUMENTS,
        BUILD_INDEX,
        SEARCH,
        SUFFIX_ARRAY,
        LCP_ARRAY,
        HISTORY,
        STATISTICS,
        SETTINGS,
        PLUS,
        REFRESH,
        CHECK,
        ALERT,
        FILE_TEXT,
        TRASH,
        PLAY,
        MULTI_SEARCH,
        AUTOCOMPLETE,
        VISUALIZER,
        ALGORITHM,
        BENCHMARK,
        REPORTS,
        DOWNLOAD
    }

    private final IconType type;
    private final int size;
    private Color color;

    public ModernIcon(IconType type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);

        Color drawColor = (color != null) ? color : c.getForeground();
        g2.setColor(drawColor);

        float s = size / 20.0f; // normalize to 20x20 base canvas
        g2.scale(s, s);

        Stroke thinStroke = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke mediumStroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        switch (type) {
            case DASHBOARD:
                // 4 modern tiles
                g2.setStroke(thinStroke);
                g2.draw(new RoundRectangle2D.Float(2, 2, 6.5f, 6.5f, 2, 2));
                g2.draw(new RoundRectangle2D.Float(11.5f, 2, 6.5f, 6.5f, 2, 2));
                g2.draw(new RoundRectangle2D.Float(2, 11.5f, 6.5f, 6.5f, 2, 2));
                g2.draw(new RoundRectangle2D.Float(11.5f, 11.5f, 6.5f, 6.5f, 2, 2));
                break;

            case DOCUMENTS:
                // Document folder / files icon
                g2.setStroke(thinStroke);
                Path2D folder = new Path2D.Float();
                folder.moveTo(2, 5);
                folder.lineTo(7, 5);
                folder.lineTo(9, 7);
                folder.lineTo(18, 7);
                folder.curveTo(18.5, 7, 19, 7.5, 19, 8);
                folder.lineTo(19, 16);
                folder.curveTo(19, 16.5, 18.5, 17, 18, 17);
                folder.lineTo(2, 17);
                folder.curveTo(1.5, 17, 1, 16.5, 1, 16);
                folder.lineTo(1, 6);
                folder.curveTo(1, 5.5, 1.5, 5, 2, 5);
                folder.closePath();
                g2.draw(folder);
                break;

            case BUILD_INDEX:
                // Lightning bolt / build icon
                Path2D bolt = new Path2D.Float();
                bolt.moveTo(11.5f, 1.5f);
                bolt.lineTo(4.5f, 11);
                bolt.lineTo(10, 11);
                bolt.lineTo(8.5f, 18.5f);
                bolt.lineTo(16, 9);
                bolt.lineTo(10.5f, 9);
                bolt.closePath();
                g2.fill(bolt);
                break;

            case SEARCH:
                // Magnifying glass
                g2.setStroke(mediumStroke);
                g2.draw(new Ellipse2D.Float(3, 3, 10, 10));
                g2.draw(new Line2D.Float(11, 11, 17.5f, 17.5f));
                break;

            case SUFFIX_ARRAY:
                // Ordered table / list rows
                g2.setStroke(thinStroke);
                g2.draw(new RoundRectangle2D.Float(2, 2, 16, 16, 3, 3));
                g2.draw(new Line2D.Float(2, 7, 18, 7));
                g2.draw(new Line2D.Float(7, 2, 7, 18));
                g2.draw(new Line2D.Float(2, 12.5f, 18, 12.5f));
                break;

            case LCP_ARRAY:
                // Overlapping common prefix blocks / DNA chain
                g2.setStroke(thinStroke);
                g2.draw(new RoundRectangle2D.Float(2, 3, 11, 6, 2, 2));
                g2.draw(new RoundRectangle2D.Float(7, 11, 11, 6, 2, 2));
                g2.draw(new Line2D.Float(7, 9, 7, 11));
                g2.draw(new Line2D.Float(13, 9, 13, 11));
                break;

            case HISTORY:
                // Clock / time history
                g2.setStroke(thinStroke);
                g2.draw(new Ellipse2D.Float(2, 2, 16, 16));
                g2.draw(new Line2D.Float(10, 5.5f, 10, 10));
                g2.draw(new Line2D.Float(10, 10, 14, 10));
                break;

            case STATISTICS:
                // Bar graph
                g2.setStroke(thinStroke);
                g2.draw(new Line2D.Float(2, 17.5f, 18, 17.5f));
                g2.fill(new RoundRectangle2D.Float(3.5f, 11, 3, 6.5f, 1, 1));
                g2.fill(new RoundRectangle2D.Float(8.5f, 6, 3, 11.5f, 1, 1));
                g2.fill(new RoundRectangle2D.Float(13.5f, 2, 3, 15.5f, 1, 1));
                break;

            case SETTINGS:
                // Gear icon
                g2.setStroke(thinStroke);
                g2.draw(new Ellipse2D.Float(7, 7, 6, 6));
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4.0;
                    float x1 = 10 + (float) (Math.cos(angle) * 6);
                    float y1 = 10 + (float) (Math.sin(angle) * 6);
                    float x2 = 10 + (float) (Math.cos(angle) * 8.5);
                    float y2 = 10 + (float) (Math.sin(angle) * 8.5);
                    g2.draw(new Line2D.Float(x1, y1, x2, y2));
                }
                break;

            case PLUS:
                g2.setStroke(mediumStroke);
                g2.draw(new Line2D.Float(10, 4, 10, 16));
                g2.draw(new Line2D.Float(4, 10, 16, 10));
                break;

            case REFRESH:
                g2.setStroke(thinStroke);
                g2.draw(new Arc2D.Float(3, 3, 14, 14, 45, 270, Arc2D.OPEN));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(11, 2);
                arrow.lineTo(16, 5);
                arrow.lineTo(13, 8);
                arrow.closePath();
                g2.fill(arrow);
                break;

            case CHECK:
                g2.setStroke(mediumStroke);
                Path2D check = new Path2D.Float();
                check.moveTo(4, 10);
                check.lineTo(8.5f, 15);
                check.lineTo(16.5f, 5);
                g2.draw(check);
                break;

            case ALERT:
                g2.setStroke(thinStroke);
                Path2D tri = new Path2D.Float();
                tri.moveTo(10, 2);
                tri.lineTo(18, 17);
                tri.lineTo(2, 17);
                tri.closePath();
                g2.draw(tri);
                g2.setStroke(mediumStroke);
                g2.draw(new Line2D.Float(10, 7, 10, 11.5f));
                g2.fill(new Ellipse2D.Float(9, 13.5f, 2, 2));
                break;

            case FILE_TEXT:
                g2.setStroke(thinStroke);
                Path2D doc = new Path2D.Float();
                doc.moveTo(4, 2);
                doc.lineTo(12, 2);
                doc.lineTo(16, 6);
                doc.lineTo(16, 18);
                doc.lineTo(4, 18);
                doc.closePath();
                g2.draw(doc);
                g2.draw(new Line2D.Float(7, 8, 13, 8));
                g2.draw(new Line2D.Float(7, 11.5f, 13, 11.5f));
                g2.draw(new Line2D.Float(7, 15, 11, 15));
                break;

            case TRASH:
                g2.setStroke(thinStroke);
                g2.draw(new Line2D.Float(3, 6, 17, 6));
                g2.draw(new Line2D.Float(7, 3, 13, 3));
                Path2D bin = new Path2D.Float();
                bin.moveTo(5, 6);
                bin.lineTo(6, 17);
                bin.curveTo(6, 17.5, 6.5, 18, 7, 18);
                bin.lineTo(13, 18);
                bin.curveTo(13.5, 18, 14, 17.5, 14, 17);
                bin.lineTo(15, 6);
                g2.draw(bin);
                g2.draw(new Line2D.Float(8.5f, 9, 8.5f, 15));
                g2.draw(new Line2D.Float(11.5f, 9, 11.5f, 15));
                break;

            case PLAY:
                Path2D play = new Path2D.Float();
                play.moveTo(5, 3);
                play.lineTo(16, 10);
                play.lineTo(5, 17);
                play.closePath();
                g2.fill(play);
                break;

            case MULTI_SEARCH:
                // Overlapping search with layers
                g2.setStroke(thinStroke);
                g2.draw(new RoundRectangle2D.Float(2, 2, 9, 11, 2, 2));
                g2.draw(new Ellipse2D.Float(9, 7, 7, 7));
                g2.setStroke(mediumStroke);
                g2.draw(new Line2D.Float(14.5f, 12.5f, 18, 16));
                break;

            case AUTOCOMPLETE:
                // Text lines with auto-prompt sparkle / right arrow
                g2.setStroke(thinStroke);
                g2.draw(new Line2D.Float(2, 5, 11, 5));
                g2.draw(new Line2D.Float(2, 10, 14, 10));
                g2.draw(new Line2D.Float(2, 15, 8, 15));
                Path2D prompt = new Path2D.Float();
                prompt.moveTo(13, 13);
                prompt.lineTo(17, 15);
                prompt.lineTo(13, 17);
                prompt.closePath();
                g2.fill(prompt);
                break;

            case VISUALIZER:
                // Tree / graph nodes connecting
                g2.setStroke(thinStroke);
                g2.draw(new Ellipse2D.Float(8, 2, 4, 4));
                g2.draw(new Ellipse2D.Float(3, 14, 4, 4));
                g2.draw(new Ellipse2D.Float(13, 14, 4, 4));
                g2.draw(new Line2D.Float(9, 6, 5, 14));
                g2.draw(new Line2D.Float(11, 6, 15, 14));
                break;

            case ALGORITHM:
                // Code comparison / braces { }
                g2.setStroke(mediumStroke);
                Path2D leftBrace = new Path2D.Float();
                leftBrace.moveTo(8, 3);
                leftBrace.lineTo(5, 6);
                leftBrace.lineTo(5, 9);
                leftBrace.lineTo(2, 10);
                leftBrace.lineTo(5, 11);
                leftBrace.lineTo(5, 14);
                leftBrace.lineTo(8, 17);
                g2.draw(leftBrace);

                Path2D rightBrace = new Path2D.Float();
                rightBrace.moveTo(12, 3);
                rightBrace.lineTo(15, 6);
                rightBrace.lineTo(15, 9);
                rightBrace.lineTo(18, 10);
                rightBrace.lineTo(15, 11);
                rightBrace.lineTo(15, 14);
                rightBrace.lineTo(12, 17);
                g2.draw(rightBrace);
                break;

            case BENCHMARK:
                // Gauge / stopwatch
                g2.setStroke(thinStroke);
                g2.draw(new Ellipse2D.Float(2, 3, 16, 16));
                g2.draw(new Line2D.Float(8, 1, 12, 1));
                g2.setStroke(mediumStroke);
                g2.draw(new Line2D.Float(10, 11, 14, 7));
                g2.fill(new Ellipse2D.Float(8.5f, 9.5f, 3, 3));
                break;

            case REPORTS:
                // Document clipboard with check / stats lines
                g2.setStroke(thinStroke);
                g2.draw(new RoundRectangle2D.Float(3, 3, 14, 16, 2, 2));
                g2.draw(new RoundRectangle2D.Float(7, 1, 6, 3, 1, 1));
                g2.draw(new Line2D.Float(6, 8, 14, 8));
                g2.draw(new Line2D.Float(6, 12, 14, 12));
                g2.draw(new Line2D.Float(6, 16, 11, 16));
                break;

            case DOWNLOAD:
                // Downward arrow into tray
                g2.setStroke(mediumStroke);
                g2.draw(new Line2D.Float(10, 3, 10, 13));
                Path2D downArrow = new Path2D.Float();
                downArrow.moveTo(6, 10);
                downArrow.lineTo(10, 14);
                downArrow.lineTo(14, 10);
                g2.draw(downArrow);
                g2.draw(new Line2D.Float(3, 17, 17, 17));
                break;
        }

        g2.dispose();
    }
}
