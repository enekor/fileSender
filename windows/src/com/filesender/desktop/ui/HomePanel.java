package com.filesender.desktop.ui;

import com.filesender.desktop.util.AppConfig;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public final class HomePanel extends Components.GradientPanel implements MainFrame.Screen {

    private final MainFrame frame;

    public HomePanel(MainFrame frame) {
        super(new BorderLayout(), new Color(0xFBF7EE), Theme.BG_GREEN);
        this.frame = frame;
        setBorder(new EmptyBorder(28, 44, 24, 44));
        add(buildContent(), BorderLayout.CENTER);
    }

    @Override
    public JComponent component() {
        return this;
    }

    @Override
    public void onHide() {
        // Nothing to stop.
    }

    private JComponent buildContent() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        body.add(Box.createVerticalGlue());

        Components.LogoBadge logo = new Components.LogoBadge();
        logo.setAlignmentX(CENTER_ALIGNMENT);
        body.add(logo);

        JLabel title = UiUtils.label(AppConfig.APP_NAME, 39, Font.BOLD, Theme.INK);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(18, 0, 0, 0));
        body.add(title);

        JLabel subtitle = UiUtils.label("Archivos por WiFi, sin cables", 15, Font.PLAIN, Theme.MUTED);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        body.add(subtitle);

        Components.PillLabel device = new Components.PillLabel(
                "Este equipo: " + AppConfig.deviceName(), new Color(0xE8F1EC), Theme.TEAL_DARK);
        device.setAlignmentX(CENTER_ALIGNMENT);
        device.setBorder(new EmptyBorder(7, 16, 7, 16));
        device.setFont(Theme.font(Font.PLAIN, 13));
        JPanel pillRow = new JPanel();
        pillRow.setOpaque(false);
        pillRow.setLayout(new BoxLayout(pillRow, BoxLayout.X_AXIS));
        pillRow.add(device);
        pillRow.setAlignmentX(CENTER_ALIGNMENT);
        pillRow.setBorder(new EmptyBorder(18, 0, 0, 0));
        body.add(pillRow);

        body.add(Box.createVerticalStrut(34));

        Components.ActionCard receive = new Components.ActionCard(
                "Recibir", "Guardar lo que envíen a este PC",
                new Color(0x12A37F), Theme.RECEIVE_DARK,
                Icons.download(34, Color.WHITE));
        receive.addActionListener(e -> frame.showReceive());

        Components.ActionCard send = new Components.ActionCard(
                "Enviar", "Compartir archivos con el móvil o tablet",
                new Color(0xE68A14), Theme.SEND_DARK,
                Icons.upload(34, Color.WHITE));
        send.addActionListener(e -> frame.showSend());

        JPanel actions = new JPanel(new GridLayout(1, 2, 18, 0));
        actions.setOpaque(false);
        actions.add(receive);
        actions.add(send);
        Dimension actionSize = new Dimension(820, 190);
        actions.setPreferredSize(actionSize);
        actions.setMaximumSize(actionSize);
        actions.setMinimumSize(new Dimension(600, 170));
        actions.setAlignmentX(CENTER_ALIGNMENT);
        body.add(actions);

        body.add(Box.createVerticalStrut(26));

        JLabel note = new JLabel("<html><div style='text-align:center'>"
                + "Los dos dispositivos deben estar en la misma red WiFi.<br>"
                + "No se usa Bluetooth.</div></html>");
        note.setFont(Theme.font(Font.PLAIN, 13));
        note.setForeground(Theme.MUTED);
        note.setHorizontalAlignment(SwingConstants.CENTER);
        note.setAlignmentX(CENTER_ALIGNMENT);
        body.add(note);

        body.add(Box.createVerticalStrut(16));

        JLabel version = UiUtils.label("Windows · versión " + AppConfig.VERSION,
                11, Font.PLAIN, new Color(0x9CA3AF));
        version.setAlignmentX(CENTER_ALIGNMENT);
        body.add(version);

        body.add(Box.createVerticalGlue());
        return body;
    }
}
