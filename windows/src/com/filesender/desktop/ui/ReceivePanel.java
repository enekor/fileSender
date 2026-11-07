package com.filesender.desktop.ui;

import com.filesender.desktop.net.DiscoveryResponder;
import com.filesender.desktop.net.NetworkUtils;
import com.filesender.desktop.net.TransferServer;
import com.filesender.desktop.storage.DownloadStore;
import com.filesender.desktop.util.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReceivePanel extends Components.GradientPanel implements MainFrame.Screen {

    private final MainFrame frame;
    private final List<String> savedFiles = new ArrayList<>();

    private TransferServer server;
    private DiscoveryResponder responder;
    private DownloadStore store;

    private Components.RadarPanel radar;
    private JLabel statusTitle;
    private JLabel statusDetail;
    private JLabel ipLine;
    private Components.RoundedPanel progressCard;
    private JLabel progressTitle;
    private Components.SlimProgressBar progressBar;
    private JLabel progressText;
    private Components.RoundedPanel errorCard;
    private JLabel errorTitle;
    private JLabel errorText;
    private Components.RoundedPanel receivedCard;
    private JLabel receivedDetail;
    private JPanel receivedFiles;

    private boolean ready;
    private boolean transferring;
    private boolean fatal;
    private boolean destroyed;
    private boolean networkWarning;
    private int totalFiles;

    public ReceivePanel(MainFrame frame) {
        super(new BorderLayout(), new Color(0xFBF7EE), Theme.BG_GREEN);
        this.frame = frame;
        add(new Components.HeaderPanel("Recibir", this::handleBack), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        installEscape();
        startReceiver();
    }

    @Override
    public JComponent component() {
        return this;
    }

    @Override
    public void onHide() {
        destroyed = true;
        if (radar != null) {
            radar.stopPulse();
        }
        if (responder != null) {
            responder.stop();
            responder = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private JComponent buildBody() {
        JPanel body = new Components.ScrollableColumn(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(8, 24, 34, 24));
        body.add(Box.createVerticalGlue());

        radar = new Components.RadarPanel();
        radar.setAlignmentX(CENTER_ALIGNMENT);
        body.add(radar);

        statusTitle = UiUtils.label("Iniciando receptor…", 30, Font.BOLD, Theme.INK);
        statusTitle.setAlignmentX(CENTER_ALIGNMENT);
        statusTitle.setHorizontalAlignment(SwingConstants.CENTER);
        statusTitle.setBorder(new EmptyBorder(12, 0, 0, 0));
        body.add(statusTitle);

        statusDetail = wrappedLabel("Preparando la carpeta de descargas.", 15, Theme.MUTED);
        statusDetail.setAlignmentX(CENTER_ALIGNMENT);
        statusDetail.setBorder(new EmptyBorder(7, 0, 0, 0));
        body.add(statusDetail);

        Components.PillLabel device = new Components.PillLabel(
                "Visible como " + AppConfig.deviceName(), new Color(0xE8F1EC), Theme.TEAL_DARK);
        JPanel pillRow = new JPanel();
        pillRow.setOpaque(false);
        pillRow.setLayout(new BoxLayout(pillRow, BoxLayout.X_AXIS));
        pillRow.add(device);
        pillRow.setAlignmentX(CENTER_ALIGNMENT);
        pillRow.setBorder(new EmptyBorder(16, 0, 0, 0));
        body.add(pillRow);

        ipLine = UiUtils.label("IP — · puerto " + AppConfig.TCP_PORT, 12, Font.PLAIN, Theme.MUTED);
        ipLine.setFont(new Font("Consolas", Font.PLAIN, 12));
        ipLine.setAlignmentX(CENTER_ALIGNMENT);
        ipLine.setBorder(new EmptyBorder(7, 0, 0, 0));
        body.add(ipLine);

        body.add(Box.createVerticalStrut(22));

        progressCard = buildProgressCard();
        progressCard.setAlignmentX(CENTER_ALIGNMENT);
        progressCard.setVisible(false);
        body.add(progressCard);

        errorCard = buildErrorCard();
        errorCard.setAlignmentX(CENTER_ALIGNMENT);
        errorCard.setVisible(false);
        body.add(errorCard);

        receivedCard = buildReceivedCard();
        receivedCard.setAlignmentX(CENTER_ALIGNMENT);
        receivedCard.setVisible(false);
        body.add(receivedCard);

        body.add(Box.createVerticalStrut(22));

        JLabel hint = new JLabel("<html><div style='text-align:center;width:580px'>"
                + "En el móvil: abre FileSender, pulsa <b>Enviar</b>, elige los archivos "
                + "y selecciona este PC.<br>"
                + "Si el móvil no encuentra el PC, permite FileSender en el Firewall de Windows."
                + "</div></html>");
        hint.setFont(Theme.font(Font.PLAIN, 12));
        hint.setForeground(Theme.MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        body.add(hint);
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = UiUtils.scrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private Components.RoundedPanel buildProgressCard() {
        Components.RoundedPanel card = new Components.RoundedPanel(
                new BorderLayout(0, 0), Theme.CARD, Theme.LINE, 22, true);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setMaximumSize(new Dimension(620, 150));
        card.setPreferredSize(new Dimension(620, 118));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        progressTitle = UiUtils.label("Recibiendo archivos", 18, Font.BOLD, Theme.INK);
        content.add(progressTitle);
        progressBar = new Components.SlimProgressBar();
        progressBar.setFill(Theme.RECEIVE);
        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        JPanel barWrap = new JPanel(new BorderLayout());
        barWrap.setOpaque(false);
        barWrap.setBorder(new EmptyBorder(14, 0, 0, 0));
        barWrap.add(progressBar, BorderLayout.CENTER);
        content.add(barWrap);
        progressText = UiUtils.label("", 12, Font.PLAIN, Theme.INK_SOFT);
        progressText.setBorder(new EmptyBorder(8, 0, 0, 0));
        content.add(progressText);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private Components.RoundedPanel buildErrorCard() {
        Components.RoundedPanel card = new Components.RoundedPanel(
                new BorderLayout(0, 0), Theme.ERROR_BG, Theme.ERROR_LINE, 16, false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(620, 130));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        errorTitle = UiUtils.label("No se pudo recibir", 14, Font.BOLD, Theme.ERROR);
        errorText = wrappedLabel("", 12, Theme.INK_SOFT);
        errorText.setBorder(new EmptyBorder(4, 0, 0, 0));
        content.add(errorTitle);
        content.add(errorText);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private Components.RoundedPanel buildReceivedCard() {
        Components.RoundedPanel card = new Components.RoundedPanel(
                new BorderLayout(0, 0), Theme.CARD, Theme.LINE, 22, true);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setMaximumSize(new Dimension(620, 310));
        card.setPreferredSize(new Dimension(620, 275));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        Components.CheckBadge check = new Components.CheckBadge(32);
        header.add(check, BorderLayout.WEST);
        header.add(UiUtils.label("Guardados en esta sesión", 18, Font.BOLD, Theme.INK),
                BorderLayout.CENTER);
        content.add(header);

        receivedDetail = UiUtils.label("", 12, Font.PLAIN, Theme.INK_SOFT);
        receivedDetail.setBorder(new EmptyBorder(8, 0, 0, 0));
        content.add(receivedDetail);

        receivedFiles = new JPanel();
        receivedFiles.setOpaque(false);
        receivedFiles.setLayout(new BoxLayout(receivedFiles, BoxLayout.Y_AXIS));
        JScrollPane listScroll = UiUtils.scrollPane(receivedFiles);
        listScroll.setPreferredSize(new Dimension(560, 118));
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        listScroll.setBorder(new EmptyBorder(10, 0, 0, 0));
        content.add(listScroll);

        JButton open = UiUtils.linkButton("Abrir carpeta de Descargas", e -> openFolder());
        open.setAlignmentX(LEFT_ALIGNMENT);
        open.setBorder(new EmptyBorder(8, 0, 0, 0));
        content.add(open);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void startReceiver() {
        try {
            store = new DownloadStore();
        } catch (Exception e) {
            showFatal("No se pudo preparar Descargas/FileSender: " + shortMessage(e));
            return;
        }
        server = new TransferServer(store, serverListener);
        server.start();
    }

    private final TransferServer.Listener serverListener = new TransferServer.Listener() {
        @Override
        public void onReady() {
            post(() -> {
                ready = true;
                startResponder();
                refreshNetwork();
                showWaiting();
            });
        }

        @Override
        public void onSessionStarted(int fileCount, long totalKnownBytes) {
            post(() -> {
                transferring = true;
                totalFiles = fileCount;
                savedFiles.clear();
                receivedFiles.removeAll();
                receivedCard.setVisible(false);
                hideError();
                progressCard.setVisible(true);
                progressTitle.setText("Recibiendo archivos");
                progressBar.setValue(0);
                progressText.setText("");
                statusTitle.setText("Recibiendo archivos");
                setDetail(AppConfig.fileCount(fileCount));
                radar.stopPulse();
                radar.setVisible(false);
                revalidate();
            });
        }

        @Override
        public void onFileStarted(int index, String name, long size) {
            post(() -> {
                progressTitle.setText("Archivo " + index + " de " + totalFiles);
                setDetail(name);
                progressBar.setValue(0);
            });
        }

        @Override
        public void onProgress(int index, String name, long fileBytes, long fileSize,
                               long totalReceived, long totalKnownBytes) {
            post(() -> {
                int percent;
                if (totalKnownBytes > 0) {
                    percent = AppConfig.percent(totalReceived, totalKnownBytes);
                } else if (fileSize > 0) {
                    percent = AppConfig.percent(fileBytes, fileSize);
                } else {
                    percent = 0;
                }
                progressBar.setValue(percent * 10);
                String amount;
                if (fileSize > 0) {
                    amount = AppConfig.formatBytes(fileBytes) + " de " + AppConfig.formatBytes(fileSize);
                } else {
                    amount = AppConfig.formatBytes(fileBytes) + " recibidos";
                }
                progressText.setText(percent + " % · " + amount);
            });
        }

        @Override
        public void onFileSaved(int index, String name, String location) {
            post(() -> savedFiles.add(name));
        }

        @Override
        public void onComplete(int fileCount, long totalReceived) {
            post(() -> {
                transferring = false;
                progressBar.setValue(1000);
                progressCard.setVisible(false);
                hideError();
                receivedDetail.setText("En Descargas/FileSender: " + AppConfig.fileCount(fileCount));
                rebuildReceivedFiles();
                receivedCard.setVisible(true);
                statusTitle.setText("Archivos recibidos");
                setDetail("Puedes enviar más archivos cuando quieras.");
                revalidate();
            });
        }

        @Override
        public void onError(String message) {
            post(() -> {
                if (!ready) {
                    showFatal(message);
                    return;
                }
                transferring = false;
                progressCard.setVisible(false);
                statusTitle.setText("No se pudo recibir");
                setDetail("Puedes volver a intentarlo. El receptor sigue activo.");
                showError("No se pudo recibir", message);
                if (!savedFiles.isEmpty()) {
                    receivedDetail.setText("En Descargas/FileSender: "
                            + AppConfig.fileCount(savedFiles.size()));
                    rebuildReceivedFiles();
                    receivedCard.setVisible(true);
                }
                revalidate();
            });
        }
    };

    private void startResponder() {
        if (responder != null || destroyed) {
            return;
        }
        responder = new DiscoveryResponder(message -> post(
                () -> showError("No se pudo iniciar la búsqueda", message)));
        responder.start();
    }

    private void refreshNetwork() {
        String ip = NetworkUtils.localIpv4Address();
        if (ip == null) {
            ipLine.setText("IP — · puerto " + AppConfig.TCP_PORT);
            networkWarning = true;
            showError("Sin conexión de red",
                    "Conecta este PC a la misma red WiFi que el móvil.");
        } else {
            ipLine.setText("IP " + ip + " · puerto " + AppConfig.TCP_PORT);
            if (networkWarning) {
                networkWarning = false;
                hideError();
            }
        }
    }

    private void showWaiting() {
        transferring = false;
        statusTitle.setText("Esperando archivos");
        setDetail("Mantén esta ventana abierta.\nEn el móvil pulsa Enviar.");
        progressCard.setVisible(false);
        radar.setVisible(true);
        radar.startPulse();
        revalidate();
    }

    private void showFatal(String message) {
        fatal = true;
        ready = false;
        transferring = false;
        statusTitle.setText("No se pudo iniciar la recepción");
        setDetail(message);
        progressCard.setVisible(false);
        showError("No se pudo iniciar la recepción", message);
        radar.stopPulse();
        revalidate();
    }

    private void showError(String title, String message) {
        errorTitle.setText(title);
        errorText.setText("<html><div style='width:540px'>"
                + UiUtils.escapeHtml(message).replace("\n", "<br>") + "</div></html>");
        errorCard.setVisible(true);
        revalidate();
    }

    private void hideError() {
        errorCard.setVisible(false);
        revalidate();
    }

    private void rebuildReceivedFiles() {
        receivedFiles.removeAll();
        int shown = Math.min(savedFiles.size(), 30);
        for (int i = 0; i < shown; i++) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4, 2, 4, 2));
            row.add(new JLabel(Icons.check(12, Theme.RECEIVE)), BorderLayout.WEST);
            JLabel name = UiUtils.label(savedFiles.get(i), 12, Font.PLAIN, Theme.INK_SOFT);
            name.setToolTipText(savedFiles.get(i));
            row.add(name, BorderLayout.CENTER);
            receivedFiles.add(row);
        }
        if (savedFiles.size() > shown) {
            JLabel more = UiUtils.label("y " + (savedFiles.size() - shown) + " más",
                    11, Font.PLAIN, Theme.MUTED);
            more.setBorder(new EmptyBorder(4, 2, 4, 2));
            receivedFiles.add(more);
        }
    }

    private void openFolder() {
        try {
            Path folder = AppConfig.downloadsDir();
            Files.createDirectories(folder);
            Desktop.getDesktop().open(folder.toFile());
        } catch (Exception e) {
            UiUtils.error(this, "No se pudo abrir la carpeta:\n" + shortMessage(e));
        }
    }

    private void handleBack() {
        if (transferring) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Si sales ahora se cancelará la transferencia en curso.",
                    "¿Detener la recepción?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        frame.showHome();
    }

    private void installEscape() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleBack();
            }
        });
    }

    private JLabel wrappedLabel(String text, int size, Color color) {
        JLabel label = new JLabel("<html><div style='text-align:center;width:520px'>"
                + UiUtils.escapeHtml(text).replace("\n", "<br>") + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, size));
        label.setForeground(color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private void setDetail(String text) {
        statusDetail.setText("<html><div style='text-align:center;width:520px'>"
                + UiUtils.escapeHtml(text).replace("\n", "<br>") + "</div></html>");
    }

    private void post(Runnable runnable) {
        if (destroyed) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (!destroyed) {
                runnable.run();
            }
        });
    }

    private static String shortMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
