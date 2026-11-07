package com.filesender.desktop.ui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Components {

    private Components() {
    }

    public static class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        public GradientPanel(LayoutManager layout, Color start, Color end) {
            super(layout);
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, start,
                        Math.max(1, getWidth()), Math.max(1, getHeight()), end));
                g.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public static final class ScrollableColumn extends JPanel implements Scrollable {
        public ScrollableColumn(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation,
                                               int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation,
                                                int direction) {
            return Math.max(80, visibleRect.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null
                    && getParent().getHeight() > getPreferredSize().height;
        }
    }

    public static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color borderColor;
        private final int arc;
        private final boolean shadow;

        public RoundedPanel(LayoutManager layout, Color fill, Color borderColor, int arc, boolean shadow) {
            super(layout);
            this.fill = fill;
            this.borderColor = borderColor;
            this.arc = arc;
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int pad = 1;
                if (shadow) {
                    for (int i = 5; i >= 1; i--) {
                        g.setColor(new Color(0, 0, 0, 4 + i));
                        g.fill(new RoundRectangle2D.Float(pad, pad + i, w - 2, h - 2, arc, arc));
                    }
                }
                if (fill != null) {
                    g.setColor(fill);
                    g.fillRoundRect(pad, pad, w - 2, h - 2, arc, arc);
                }
                if (borderColor != null) {
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(borderColor);
                    g.drawRoundRect(pad, pad, w - 2, h - 2, arc, arc);
                }
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public static final class PillLabel extends JLabel {
        private final Color background;
        private final Color foreground;

        public PillLabel(String text, Color background, Color foreground) {
            super(text);
            this.background = background;
            this.foreground = foreground;
            setForeground(foreground);
            setFont(Theme.font(Font.PLAIN, 13));
            setBorder(new EmptyBorder(7, 16, 7, 16));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(background);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public static final class CheckBadge extends JComponent {
        private final int size;

        public CheckBadge(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.RECEIVE);
                g.fillOval(0, 0, size, size);
                Icon check = Icons.check(Math.round(size * 0.56f), Color.WHITE);
                check.paintIcon(this, g, (size - check.getIconWidth()) / 2,
                        (size - check.getIconHeight()) / 2);
            } finally {
                g.dispose();
            }
        }
    }

    public static final class LogoBadge extends JComponent {
        public LogoBadge() {
            setPreferredSize(new Dimension(92, 92));
            setMaximumSize(new Dimension(92, 92));
            setMinimumSize(new Dimension(92, 92));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g.setPaint(new GradientPaint(x, y, new Color(0x12A37F),
                        x + size, y + size, Theme.TEAL_DARK));
                g.fillOval(x, y, size, size);
                Icon icon = Icons.wifi(42, Color.WHITE);
                icon.paintIcon(this, g, x + (size - 42) / 2, y + (size - 42) / 2);
            } finally {
                g.dispose();
            }
        }
    }

    public static final class ActionCard extends JComponent {
        private final String title;
        private final String subtitle;
        private final Color start;
        private final Color end;
        private final Icon icon;
        private final List<ActionListener> listeners = new ArrayList<>();
        private boolean hover;
        private boolean pressed;

        public ActionCard(String title, String subtitle, Color start, Color end, Icon icon) {
            this.title = title;
            this.subtitle = subtitle;
            this.start = start;
            this.end = end;
            this.icon = icon;
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(390, 190));
            setMinimumSize(new Dimension(280, 170));
            setToolTipText(title);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        pressed = true;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean fire = pressed && contains(e.getPoint())
                            && SwingUtilities.isLeftMouseButton(e);
                    pressed = false;
                    repaint();
                    if (fire) {
                        requestFocusInWindow();
                        fire();
                    }
                }
            };
            addMouseListener(mouse);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
            getActionMap().put("activate", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fire();
                }
            });
        }

        public void addActionListener(ActionListener listener) {
            listeners.add(listener);
        }

        private void fire() {
            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, title);
            for (ActionListener listener : listeners) {
                listener.actionPerformed(event);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int shadowOffset = hover ? 6 : 4;
                for (int i = 8; i >= 1; i--) {
                    g.setColor(new Color(0, 0, 0, 3 + i));
                    g.fillRoundRect(2, shadowOffset + i, w - 4, h - 8, 26, 26);
                }
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 2, h - 8, 26, 26);
                g.setPaint(new GradientPaint(0, 0, start, w, h, end));
                g.fill(shape);
                if (hover) {
                    g.setColor(new Color(255, 255, 255, 18));
                    g.fill(shape);
                }
                if (pressed) {
                    g.setColor(new Color(0, 0, 0, 20));
                    g.fill(shape);
                }

                int iconSize = 62;
                int iconX = 28;
                int iconY = (h - 8 - iconSize) / 2;
                g.setColor(Theme.WHITE_20);
                g.fillRoundRect(iconX, iconY, iconSize, iconSize, 20, 20);
                int is = icon == null ? 0 : icon.getIconWidth();
                icon.paintIcon(this, g, iconX + (iconSize - is) / 2,
                        iconY + (iconSize - is) / 2);

                int textX = iconX + iconSize + 20;
                g.setFont(Theme.font(Font.BOLD, 27));
                g.setColor(Color.WHITE);
                FontMetrics titleMetrics = g.getFontMetrics();
                int titleY = h / 2 - 2;
                g.drawString(title, textX, titleY);

                g.setFont(Theme.font(Font.PLAIN, 13));
                g.setColor(Theme.WHITE_80);
                g.drawString(subtitle, textX, titleY + 24);
                if (titleMetrics.stringWidth(title) > w - textX - 56) {
                    // Keep the title visually inside the card on narrow windows.
                    g.setFont(Theme.font(Font.BOLD, 22));
                    g.drawString(title, textX, titleY);
                }

                Icon chevron = Icons.chevron(23, new Color(255, 255, 255, 205));
                chevron.paintIcon(this, g, w - 46, (h - 8 - 23) / 2);

                if (isFocusOwner()) {
                    g.setColor(new Color(255, 255, 255, 190));
                    g.setStroke(new BasicStroke(2f));
                    g.drawRoundRect(4, 4, w - 10, h - 16, 22, 22);
                }
            } finally {
                g.dispose();
            }
        }
    }

    public static final class IconButton extends JButton {
        private final Icon icon;

        public IconButton(Icon icon, String tooltip, ActionListener action) {
            this.icon = icon;
            setToolTipText(tooltip);
            setPreferredSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
            if (action != null) {
                addActionListener(action);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isPressed()) {
                    g.setColor(getModel().isPressed()
                            ? new Color(0, 0, 0, 22) : new Color(0, 0, 0, 12));
                    g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                }
                int x = (getWidth() - icon.getIconWidth()) / 2;
                int y = (getHeight() - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g, x, y);
                if (isFocusOwner()) {
                    g.setColor(Theme.TEAL);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawOval(2, 2, getWidth() - 5, getHeight() - 5);
                }
            } finally {
                g.dispose();
            }
        }
    }

    public static final class SlimProgressBar extends JComponent {
        private int value;
        private Color fill = Theme.RECEIVE;

        public SlimProgressBar() {
            setPreferredSize(new Dimension(220, 8));
            setMinimumSize(new Dimension(80, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            setOpaque(false);
        }

        public void setValue(int value) {
            this.value = Math.max(0, Math.min(1000, value));
            repaint();
        }

        public int getValue() {
            return value;
        }

        public void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int h = Math.min(8, getHeight());
                int y = (getHeight() - h) / 2;
                g.setColor(new Color(0xE9ECEF));
                g.fillRoundRect(0, y, getWidth(), h, h, h);
                int fillWidth = (int) Math.round(getWidth() * (value / 1000.0d));
                if (fillWidth > 0) {
                    g.setColor(fill);
                    g.fillRoundRect(0, y, Math.max(h, fillWidth), h, h, h);
                }
            } finally {
                g.dispose();
            }
        }
    }

    public static final class HeaderPanel extends JPanel {
        public HeaderPanel(String title, Runnable onBack) {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(10, 12, 10, 20));
            IconButton back = new IconButton(Icons.back(23, Theme.INK), "Volver", e -> {
                if (onBack != null) {
                    onBack.run();
                }
            });
            add(back, BorderLayout.WEST);
            JLabel titleLabel = UiUtils.label(title, 21, Font.BOLD, Theme.INK);
            add(titleLabel, BorderLayout.CENTER);
        }
    }

    public static final class RadarPanel extends JPanel {
        private final Timer timer;
        private boolean pulsing;
        private double phase;

        public RadarPanel() {
            setPreferredSize(new Dimension(198, 198));
            setMinimumSize(new Dimension(160, 160));
            setMaximumSize(new Dimension(205, 205));
            setOpaque(false);
            timer = new Timer(40, e -> {
                phase += 0.055;
                repaint();
            });
        }

        public void startPulse() {
            if (!pulsing) {
                pulsing = true;
                timer.start();
            }
        }

        public void stopPulse() {
            pulsing = false;
            timer.stop();
            phase = 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int base = Math.min(getWidth(), getHeight());
                double pulse = pulsing ? (Math.sin(phase) + 1.0d) / 2.0d : 0.35d;
                for (int i = 0; i < 3; i++) {
                    int radius = (int) (base * (0.41 + i * 0.065));
                    int alpha = 18 + (int) (22 * pulse) + i * 6;
                    g.setColor(new Color(14, 143, 112, Math.min(90, alpha)));
                    g.setStroke(new BasicStroke(1.4f));
                    g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
                    g.setColor(new Color(14, 143, 112, 8 + i * 3));
                    g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                }
                int core = (int) (base * 0.29);
                g.setPaint(new GradientPaint(cx - core, cy - core, new Color(0x12A37F),
                        cx + core, cy + core, Theme.RECEIVE_DARK));
                g.fillOval(cx - core / 2, cy - core / 2, core, core);
                Icon wifi = Icons.wifi(38, Color.WHITE);
                wifi.paintIcon(this, g, cx - 19, cy - 19);
            } finally {
                g.dispose();
            }
        }
    }

    public static final class DropZone extends JPanel {
        private final Runnable onClick;
        private Consumer<List<File>> onFiles;
        private boolean highlight;
        private boolean dropEnabled = true;

        public DropZone(Runnable onClick) {
            this.onClick = onClick;
            setOpaque(false);
            setPreferredSize(new Dimension(520, 168));
            setMinimumSize(new Dimension(300, 150));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Arrastra archivos o haz clic para seleccionarlos");
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && isEnabled() && onClick != null) {
                        onClick.run();
                    }
                }
            });
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "open");
            getActionMap().put("open", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isEnabled() && onClick != null) {
                        onClick.run();
                    }
                }
            });
            try {
                setDropTarget(new java.awt.dnd.DropTarget(this,
                        java.awt.dnd.DnDConstants.ACTION_COPY,
                        new java.awt.dnd.DropTargetAdapter() {
                            @Override
                            public void dragEnter(java.awt.dnd.DropTargetDragEvent e) {
                                if (isEnabled() && e.isDataFlavorSupported(
                                        java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                    e.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
                                    highlight = true;
                                    repaint();
                                } else {
                                    e.rejectDrag();
                                }
                            }

                            @Override
                            public void dragOver(java.awt.dnd.DropTargetDragEvent e) {
                                if (isEnabled() && e.isDataFlavorSupported(
                                        java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                    e.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
                                } else {
                                    e.rejectDrag();
                                }
                            }

                            @Override
                            public void dragExit(java.awt.dnd.DropTargetEvent e) {
                                highlight = false;
                                repaint();
                            }

                            @Override
                            public void drop(java.awt.dnd.DropTargetDropEvent e) {
                                highlight = false;
                                repaint();
                                if (!isEnabled()) {
                                    e.rejectDrop();
                                    return;
                                }
                                try {
                                    e.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                                    Object data = e.getTransferable().getTransferData(
                                            java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                                    if (data instanceof List<?> raw) {
                                        List<File> files = new ArrayList<>();
                                        for (Object item : raw) {
                                            if (item instanceof File file && file.isFile()) {
                                                files.add(file);
                                            }
                                        }
                                        if (onFiles != null) {
                                            onFiles.accept(files);
                                        }
                                    }
                                    e.dropComplete(true);
                                } catch (Exception ex) {
                                    e.dropComplete(false);
                                }
                            }
                        }));
            } catch (Exception ignored) {
                // Drag and drop is unavailable in some restricted environments.
            }
        }

        public void setOnFiles(Consumer<List<File>> onFiles) {
            this.onFiles = onFiles;
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            dropEnabled = enabled;
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Color fill = highlight ? new Color(0xDDF4EC)
                        : new Color(255, 255, 255, 150);
                g.setColor(fill);
                g.fillRoundRect(1, 1, w - 2, h - 2, 22, 22);
                g.setColor(highlight ? Theme.RECEIVE : new Color(0x9BCDC0));
                g.setStroke(new BasicStroke(highlight ? 2.2f : 1.7f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[]{7f, 7f}, 0f));
                g.drawRoundRect(2, 2, w - 5, h - 5, 22, 22);

                int iconSize = 54;
                int iconX = (w - iconSize) / 2;
                int iconY = Math.max(20, h / 2 - 60);
                g.setColor(Theme.TEAL_SOFT);
                g.fillOval(iconX, iconY, iconSize, iconSize);
                Icon up = Icons.upload(27, Theme.TEAL);
                up.paintIcon(this, g, iconX + (iconSize - 27) / 2,
                        iconY + (iconSize - 27) / 2);

                g.setFont(Theme.font(Font.BOLD, 17));
                g.setColor(Theme.INK);
                String title = "Arrastra para compartir";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(title, (w - fm.stringWidth(title)) / 2, iconY + iconSize + 29);

                g.setFont(Theme.font(Font.PLAIN, 13));
                g.setColor(Theme.MUTED);
                String subtitle = "o haz clic para elegir uno o varios archivos";
                FontMetrics fm2 = g.getFontMetrics();
                g.drawString(subtitle, (w - fm2.stringWidth(subtitle)) / 2,
                        iconY + iconSize + 50);
            } finally {
                g.dispose();
            }
        }
    }

    public static final class DeviceRow extends JPanel {
        private boolean hover;

        public DeviceRow(String name, String address, Runnable onClick) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(11, 13, 11, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
            setPreferredSize(new Dimension(320, 68));

            RoundedPanel iconWrap = new RoundedPanel(
                    new GridBagLayout(), Theme.TEAL_SOFT, new Color(0xCBE8DF), 14, false);
            iconWrap.setPreferredSize(new Dimension(44, 44));
            iconWrap.setMinimumSize(new Dimension(44, 44));
            iconWrap.setMaximumSize(new Dimension(44, 44));
            iconWrap.add(new JLabel(Icons.tablet(23, Theme.TEAL)));
            add(iconWrap, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            JLabel nameLabel = UiUtils.label(name, 15, Font.BOLD, Theme.INK);
            nameLabel.setToolTipText(name);
            JLabel addressLabel = UiUtils.label(address, 11, Font.PLAIN, Theme.MUTED);
            addressLabel.setBorder(new EmptyBorder(3, 0, 0, 0));
            text.add(nameLabel);
            text.add(addressLabel);
            add(text, BorderLayout.CENTER);

            JLabel arrow = new JLabel(Icons.chevron(20, Theme.SEND));
            add(arrow, BorderLayout.EAST);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && onClick != null) {
                        onClick.run();
                    }
                }
            };
            addMouseListener(mouse);
            iconWrap.addMouseListener(mouse);
            text.addMouseListener(mouse);
            nameLabel.addMouseListener(mouse);
            addressLabel.addMouseListener(mouse);
            arrow.addMouseListener(mouse);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(hover ? new Color(0xF5FBF8) : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(hover ? new Color(0xC6E5DC) : Theme.LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public static final class FileRow extends JPanel {
        public FileRow(Path path, String displayName, long size, Runnable onRemove) {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 10, 8, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

            JLabel icon = new JLabel(Icons.file(20, Theme.TEAL));
            icon.setBorder(new EmptyBorder(0, 0, 0, 2));
            add(icon, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            JLabel name = UiUtils.label(displayName, 13, Font.PLAIN, Theme.INK);
            name.setToolTipText(path == null ? displayName : path.toString());
            JLabel meta = UiUtils.label(AppConfigShim.size(size), 11, Font.PLAIN, Theme.MUTED);
            text.add(name);
            text.add(meta);
            add(text, BorderLayout.CENTER);

            IconButton remove = new IconButton(Icons.close(13, Theme.MUTED), "Quitar", e -> {
                if (onRemove != null) {
                    onRemove.run();
                }
            });
            remove.setPreferredSize(new Dimension(30, 30));
            remove.setMinimumSize(new Dimension(30, 30));
            remove.setMaximumSize(new Dimension(30, 30));
            add(remove, BorderLayout.EAST);
        }
    }

    private static final class AppConfigShim {
        static String size(long bytes) {
            return com.filesender.desktop.util.AppConfig.formatBytes(bytes);
        }
    }
}
