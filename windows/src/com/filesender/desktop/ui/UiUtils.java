package com.filesender.desktop.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;

public final class UiUtils {

    private UiUtils() {
    }

    public static JLabel label(String text, int size, int style, java.awt.Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        return label;
    }

    public static JButton primaryButton(String text, java.awt.Color background,
                                        java.awt.Color foreground, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.PLAIN, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setFocusPainted(false);
        if (action != null) {
            button.addActionListener(action);
        }
        return button;
    }

    public static JButton outlineButton(String text, ActionListener action) {
        JButton button = primaryButton(text, Theme.CARD, Theme.TEAL_DARK, action);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JButton.borderColor", Theme.TEAL);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0x8A0F766E), 1),
                new EmptyBorder(9, 17, 9, 17)));
        return button;
    }

    public static JButton linkButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.PLAIN, 13));
        button.setForeground(Theme.TEAL);
        button.setContentAreaFilled(false);
        button.setBorder(new EmptyBorder(2, 4, 2, 4));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        if (action != null) {
            button.addActionListener(action);
        }
        return button;
    }

    public static JScrollPane scrollPane(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        return pane;
    }

    public static JComponent centered(JComponent component, int maxWidth) {
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.GridBagLayout());
        wrapper.setOpaque(false);
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        wrapper.add(component, gbc);
        wrapper.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
        return wrapper;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "FileSender",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "FileSender",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void setMaxWidth(JComponent component, int width) {
        component.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
    }
}
