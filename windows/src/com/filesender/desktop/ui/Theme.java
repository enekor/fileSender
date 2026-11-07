package com.filesender.desktop.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;

public final class Theme {

    public static final Color TEAL = new Color(0x0F766E);
    public static final Color TEAL_DARK = new Color(0x115E59);
    public static final Color TEAL_SOFT = new Color(0xE6F4F1);
    public static final Color RECEIVE = new Color(0x0E8F70);
    public static final Color RECEIVE_DARK = new Color(0x0B735A);
    public static final Color SEND = new Color(0xD97706);
    public static final Color SEND_DARK = new Color(0xC2680A);
    public static final Color BG = new Color(0xF7F5EF);
    public static final Color BG_GREEN = new Color(0xEAF6F1);
    public static final Color CARD = Color.WHITE;
    public static final Color INK = new Color(0x1F2937);
    public static final Color INK_SOFT = new Color(0x4B5563);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color LINE = new Color(0xE5E7EB);
    public static final Color ERROR = new Color(0xB91C1C);
    public static final Color ERROR_BG = new Color(0xFEF2F2);
    public static final Color ERROR_LINE = new Color(0xFECACA);
    public static final Color WHITE = Color.WHITE;
    public static final Color WHITE_80 = new Color(255, 255, 255, 205);
    public static final Color WHITE_20 = new Color(255, 255, 255, 51);

    private static final String FONT_FAMILY = chooseFont();
    public static final Font FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_MEDIUM = new Font(FONT_FAMILY, Font.PLAIN, 15);

    private Theme() {
    }

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", FONT);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 16);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Button.focusedBorderColor", TEAL);
        UIManager.put("Button.hoverBorderColor", LINE);
        UIManager.put("Button.default.boldText", false);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("OptionPane.buttonFont", FONT);
        UIManager.put("ToolTip.background", new Color(0x111827));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", FONT_SMALL);
        UIManager.put("Component.focusColor", TEAL);
    }

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    private static String chooseFont() {
        Set<String> available = new HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            available.add(name);
        }
        for (String candidate : new String[]{"Segoe UI", "Segoe UI Variable", "Arial", "SansSerif"}) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }
}
