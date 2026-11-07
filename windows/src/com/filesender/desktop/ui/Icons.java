package com.filesender.desktop.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

public final class Icons {

    private Icons() {
    }

    public static Icon wifi(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(2.1f));
                Path2D outer = new Path2D.Double();
                outer.moveTo(2.3, 9.0);
                outer.quadTo(12, 1.5, 21.7, 9.0);
                g.draw(outer);
                Path2D middle = new Path2D.Double();
                middle.moveTo(5.5, 12.4);
                middle.quadTo(12, 7.2, 18.5, 12.4);
                g.draw(middle);
                Path2D inner = new Path2D.Double();
                inner.moveTo(8.7, 15.8);
                inner.quadTo(12, 12.9, 15.3, 15.8);
                g.draw(inner);
                g.fillOval(10, 17, 4, 4);
            }
        };
    }

    public static Icon download(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(2.1f));
                g.drawLine(12, 3, 12, 14);
                Path2D head = new Path2D.Double();
                head.moveTo(7.4, 9.6);
                head.lineTo(12, 14.2);
                head.lineTo(16.6, 9.6);
                g.draw(head);
                g.drawLine(5, 19, 19, 19);
            }
        };
    }

    public static Icon upload(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(2.1f));
                g.drawLine(12, 21, 12, 10);
                Path2D head = new Path2D.Double();
                head.moveTo(7.4, 14.4);
                head.lineTo(12, 9.8);
                head.lineTo(16.6, 14.4);
                g.draw(head);
                g.drawLine(5, 5, 19, 5);
            }
        };
    }

    public static Icon back(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(2.1f));
                g.drawLine(19, 12, 5, 12);
                Path2D head = new Path2D.Double();
                head.moveTo(10, 7);
                head.lineTo(5, 12);
                head.lineTo(10, 17);
                g.draw(head);
            }
        };
    }

    public static Icon chevron(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(2.2f));
                Path2D p = new Path2D.Double();
                p.moveTo(9.2, 5.5);
                p.lineTo(15.7, 12);
                p.lineTo(9.2, 18.5);
                g.draw(p);
            }
        };
    }

    public static Icon check(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(2.4f));
                Path2D p = new Path2D.Double();
                p.moveTo(4.5, 12.5);
                p.lineTo(9.5, 17.5);
                p.lineTo(20, 6.5);
                g.draw(p);
            }
        };
    }

    public static Icon folder(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(1.8f));
                Path2D p = new Path2D.Double();
                p.moveTo(2.8, 6.5);
                p.lineTo(9.2, 6.5);
                p.lineTo(11.2, 8.8);
                p.lineTo(21.2, 8.8);
                p.quadTo(22, 8.8, 22, 9.7);
                p.lineTo(22, 18.2);
                p.quadTo(22, 19.2, 21, 19.2);
                p.lineTo(3, 19.2);
                p.quadTo(2, 19.2, 2, 18.2);
                p.lineTo(2, 7.7);
                p.quadTo(2, 6.5, 2.8, 6.5);
                g.draw(p);
            }
        };
    }

    public static Icon tablet(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(1.9f));
                g.drawRoundRect(3, 3, 18, 18, 3, 3);
                g.drawLine(9, 18, 15, 18);
                g.fillOval(11, 6, 3, 3);
            }
        };
    }

    public static Icon file(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(1.8f));
                Path2D p = new Path2D.Double();
                p.moveTo(6, 2.8);
                p.lineTo(14, 2.8);
                p.lineTo(19, 7.8);
                p.lineTo(19, 21.2);
                p.lineTo(6, 21.2);
                p.closePath();
                g.draw(p);
                Path2D fold = new Path2D.Double();
                fold.moveTo(14, 2.8);
                fold.lineTo(14, 7.8);
                fold.lineTo(19, 7.8);
                g.draw(fold);
                g.drawLine(9, 12, 16, 12);
                g.drawLine(9, 16, 16, 16);
            }
        };
    }

    public static Icon close(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(1.9f));
                g.drawLine(6, 6, 18, 18);
                g.drawLine(18, 6, 6, 18);
            }
        };
    }

    public static Icon grid(int size, Color color) {
        return new VectorIcon(size, color) {
            @Override
            void paint(Graphics2D g) {
                g.setStroke(stroke(1.8f));
                g.drawRoundRect(3, 3, 7, 7, 2, 2);
                g.drawRoundRect(14, 3, 7, 7, 2, 2);
                g.drawRoundRect(3, 14, 7, 7, 2, 2);
                g.drawRoundRect(14, 14, 7, 7, 2, 2);
            }
        };
    }

    private static BasicStroke stroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private abstract static class VectorIcon implements Icon {
        private final int size;
        private final Color color;

        VectorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        abstract void paint(Graphics2D g);

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.scale(size / 24.0d, size / 24.0d);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);
                g.setColor(color);
                paint(g);
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
