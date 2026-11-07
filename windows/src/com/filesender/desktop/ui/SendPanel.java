package com.filesender.desktop.ui;

import com.filesender.desktop.SelectedFile;
import com.filesender.desktop.net.DeviceInfo;
import com.filesender.desktop.net.DiscoveryClient;
import com.filesender.desktop.net.TransferClient;
import com.filesender.desktop.net.TransferProtocol;
import com.filesender.desktop.util.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SendPanel extends Components.GradientPanel implements MainFrame.Screen {

    private static final int ACTION_CANCEL = 1;
    private static final int ACTION_DONE = 2;
    private static final int ACTION_RESCAN = 3;

    private final MainFrame frame;
    private final List<SelectedFile> files = new ArrayList<>();
    private final Set<String> deviceKeys = new LinkedHashSet<>();

    private Components.DropZone dropZone;
    private JLabel fileCount;
    private JButton clearFiles;
    private JPanel fileRows;
    private JPanel fileListWrap;
    private JLabel emptyFiles;

    private JPanel rightHost;
    private CardLayout rightCards;
    private Components.RoundedPanel discoverCard;
    private JLabel scanStatus;
    private JLabel scanHint;
    private JProgressBar scanSpinner;
    private JPanel deviceList;
    private JLabel emptyDevices;
    private JButton scanButton;

    private Components.RoundedPanel transferCard;
    private JLabel transferTitle;
    private JLabel transferDetail;
    private Components.SlimProgressBar transferBar;
    private JLabel transferPercent;
    private JButton transferAction;

    private DiscoveryClient discovery;
    private TransferClient transfer;
    private DeviceInfo target;

    private boolean transferring;
    private boolean scanning;
    private boolean destroyed;
    private int actionMode;
    private String lastDirectory;

    public SendPanel(MainFrame frame) {
        super(new BorderLayout(), new Color(0xFBF7EE), Theme.BG_GREEN);
        this.frame = frame;
        add(new Components.HeaderPanel("Enviar", this::handleBack), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        installEscape();
        updateFileList();
        showIdle();
    }

    @Override
    public JComponent component() {
        return this;
    }

    @Override
    public void onHide() {
        destroyed = true;
        stopDiscovery();
        cancelTransfer();
    }

    private JComponent buildBody() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 24, 26, 24));

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.56;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 22);
        content.add(buildFilesColumn(), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.44;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        content.add(buildRightColumn(), right);

        return content;
    }

    private JComponent buildFilesColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setOpaque(false);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = UiUtils.label("Archivos", 20, Font.BOLD, Theme.INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        top.add(title);

        JLabel hint = UiUtils.label("Arrastra varios archivos o haz clic en la zona de abajo.",
                12, Font.PLAIN, Theme.MUTED);
        hint.setBorder(new EmptyBorder(3, 0, 0, 0));
        hint.setAlignmentX(LEFT_ALIGNMENT);
        top.add(hint);

        dropZone = new Components.DropZone(this::chooseFiles);
        dropZone.setOnFiles(this::addFiles);
        dropZone.setAlignmentX(LEFT_ALIGNMENT);
        JPanel dropWrap = new JPanel(new BorderLayout());
        dropWrap.setOpaque(false);
        dropWrap.setBorder(new EmptyBorder(12, 0, 0, 0));
        dropWrap.add(dropZone, BorderLayout.CENTER);
        dropWrap.setAlignmentX(LEFT_ALIGNMENT);
        top.add(dropWrap);
        column.add(top, BorderLayout.NORTH);

        Components.RoundedPanel selectedCard = new Components.RoundedPanel(
                new BorderLayout(0, 0), Theme.CARD, Theme.LINE, 22, true);
        selectedCard.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel selectedHeader = new JPanel(new BorderLayout(8, 0));
        selectedHeader.setOpaque(false);
        fileCount = UiUtils.label("0 archivos seleccionados", 15, Font.BOLD, Theme.INK);
        selectedHeader.add(fileCount, BorderLayout.WEST);
        clearFiles = UiUtils.linkButton("Quitar todos", e -> clearFiles());
        clearFiles.setEnabled(false);
        selectedHeader.add(clearFiles, BorderLayout.EAST);
        selectedCard.add(selectedHeader, BorderLayout.NORTH);

        fileRows = new JPanel();
        fileRows.setOpaque(false);
        fileRows.setLayout(new BoxLayout(fileRows, BoxLayout.Y_AXIS));

        emptyFiles = UiUtils.label("Todavía no has añadido archivos.", 12, Font.PLAIN, Theme.MUTED);
        emptyFiles.setHorizontalAlignment(SwingConstants.CENTER);

        fileListWrap = new JPanel(new BorderLayout());
        fileListWrap.setOpaque(false);
        fileListWrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        fileListWrap.add(fileRows, BorderLayout.NORTH);

        JScrollPane fileScroll = UiUtils.scrollPane(fileListWrap);
        selectedCard.add(fileScroll, BorderLayout.CENTER);
        column.add(selectedCard, BorderLayout.CENTER);
        return column;
    }

    private JComponent buildRightColumn() {
        rightCards = new CardLayout();
        rightHost = new JPanel(rightCards);
        rightHost.setOpaque(false);

        discoverCard = new Components.RoundedPanel(
                new BorderLayout(0, 0), Theme.CARD, Theme.LINE, 22, true);
        discoverCard.setBorder(new EmptyBorder(20, 20, 18, 20));

        JPanel discoverContent = new JPanel();
        discoverContent.setOpaque(false);
        discoverContent.setLayout(new BoxLayout(discoverContent, BoxLayout.Y_AXIS));

        JLabel title = UiUtils.label("Enviar a", 21, Font.BOLD, Theme.INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        discoverContent.add(title);
        JLabel subtitle = UiUtils.label("Elige el dispositivo que debe recibir.", 12, Font.PLAIN,
                Theme.MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setHorizontalAlignment(SwingConstants.LEFT);
        subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        discoverContent.add(subtitle);

        JPanel scanRow = new JPanel(new BorderLayout(9, 0));
        scanRow.setOpaque(false);
        scanRow.setBorder(new EmptyBorder(18, 0, 0, 0));
        scanRow.setAlignmentX(LEFT_ALIGNMENT);
        scanSpinner = new JProgressBar();
        scanSpinner.setIndeterminate(true);
        scanSpinner.setBorderPainted(false);
        scanSpinner.setPreferredSize(new Dimension(34, 6));
        scanSpinner.setMaximumSize(new Dimension(34, 6));
        scanSpinner.setForeground(Theme.TEAL);
        scanSpinner.setBackground(Theme.LINE);
        scanSpinner.setVisible(false);
        scanRow.add(scanSpinner, BorderLayout.WEST);
        JPanel scanText = new JPanel();
        scanText.setOpaque(false);
        scanText.setLayout(new BoxLayout(scanText, BoxLayout.Y_AXIS));
        scanStatus = wrappedLabel("Añade archivos para buscar dispositivos.", 13, Theme.INK_SOFT, 300);
        scanStatus.setFont(Theme.font(Font.BOLD, 13));
        scanHint = wrappedLabel("", 11, Theme.MUTED, 300);
        scanHint.setBorder(new EmptyBorder(2, 0, 0, 0));
        scanText.add(scanStatus);
        scanText.add(scanHint);
        scanRow.add(scanText, BorderLayout.CENTER);
        discoverContent.add(scanRow);

        deviceList = new JPanel();
        deviceList.setOpaque(false);
        deviceList.setLayout(new BoxLayout(deviceList, BoxLayout.Y_AXIS));
        deviceList.setBorder(new EmptyBorder(12, 0, 0, 0));

        emptyDevices = UiUtils.label("", 12, Font.PLAIN, Theme.MUTED);
        emptyDevices.setHorizontalAlignment(SwingConstants.CENTER);
        emptyDevices.setAlignmentX(CENTER_ALIGNMENT);
        emptyDevices.setBorder(new EmptyBorder(20, 0, 20, 0));
        deviceList.add(emptyDevices);

        JScrollPane deviceScroll = UiUtils.scrollPane(deviceList);
        JPanel listWrap = new JPanel(new BorderLayout());
        listWrap.setOpaque(false);
        listWrap.setAlignmentX(LEFT_ALIGNMENT);
        listWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        listWrap.setPreferredSize(new Dimension(300, 300));
        listWrap.add(deviceScroll, BorderLayout.CENTER);
        discoverContent.add(listWrap);

        scanButton = UiUtils.outlineButton("Buscar de nuevo", e -> startScan());
        scanButton.setAlignmentX(LEFT_ALIGNMENT);
        scanButton.setVisible(false);
        JPanel buttonWrap = new JPanel(new BorderLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttonWrap.add(scanButton, BorderLayout.CENTER);
        discoverContent.add(buttonWrap);

        discoverCard.add(discoverContent, BorderLayout.CENTER);
        rightHost.add(discoverCard, "discover");

        transferCard = buildTransferCard();
        rightHost.add(transferCard, "transfer");
        rightCards.show(rightHost, "discover");
        return rightHost;
    }

    private Components.RoundedPanel buildTransferCard() {
        Components.RoundedPanel card = new Components.RoundedPanel(
                new BorderLayout(0, 0), Theme.CARD, Theme.LINE, 22, true);
        card.setBorder(new EmptyBorder(20, 20, 18, 20));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        transferTitle = UiUtils.label("Enviando archivos", 21, Font.BOLD, Theme.INK);
        transferTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(transferTitle);

        transferDetail = wrappedLabel("Preparando la conexión…", 12, Theme.INK_SOFT, 320);
        transferDetail.setBorder(new EmptyBorder(6, 0, 0, 0));
        transferDetail.setAlignmentX(LEFT_ALIGNMENT);
        content.add(transferDetail);

        transferBar = new Components.SlimProgressBar();
        transferBar.setFill(Theme.SEND);
        transferBar.setAlignmentX(LEFT_ALIGNMENT);
        JPanel barWrap = new JPanel(new BorderLayout());
        barWrap.setOpaque(false);
        barWrap.setBorder(new EmptyBorder(18, 0, 0, 0));
        barWrap.add(transferBar, BorderLayout.CENTER);
        content.add(barWrap);

        transferPercent = UiUtils.label("0 %", 12, Font.PLAIN, Theme.MUTED);
        transferPercent.setBorder(new EmptyBorder(8, 0, 0, 0));
        transferPercent.setAlignmentX(LEFT_ALIGNMENT);
        content.add(transferPercent);

        content.add(Box.createVerticalStrut(16));
        transferAction = UiUtils.outlineButton("Cancelar envío", e -> handleTransferAction());
        transferAction.setAlignmentX(LEFT_ALIGNMENT);
        transferAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        content.add(transferAction);

        card.add(content, BorderLayout.NORTH);
        return card;
    }

    private void chooseFiles() {
        if (transferring) {
            return;
        }
        try {
            FileDialog dialog = new FileDialog(frame,
                    "Selecciona uno o varios archivos", FileDialog.LOAD);
            dialog.setMultipleMode(true);
            if (lastDirectory != null) {
                dialog.setDirectory(lastDirectory);
            }
            dialog.setVisible(true);
            File[] chosen = dialog.getFiles();
            if (chosen != null && chosen.length > 0) {
                lastDirectory = chosen[0].getParent();
                addFiles(java.util.Arrays.asList(chosen));
            }
            dialog.dispose();
        } catch (Exception e) {
            UiUtils.error(this, "No se pudo abrir el explorador de archivos:\n" + e.getMessage());
        }
    }

    private void addFiles(List<File> incoming) {
        if (transferring || incoming == null || incoming.isEmpty()) {
            return;
        }
        Set<String> existing = new LinkedHashSet<>();
        for (SelectedFile file : files) {
            existing.add(file.getPath().toAbsolutePath().normalize().toString());
        }
        int added = 0;
        for (File file : incoming) {
            if (file == null || !file.isFile()) {
                continue;
            }
            Path path = file.toPath().toAbsolutePath().normalize();
            if (!existing.add(path.toString())) {
                continue;
            }
            if (files.size() >= TransferProtocol.MAX_FILES) {
                UiUtils.info(this, "Puedes enviar como máximo "
                        + TransferProtocol.MAX_FILES + " archivos a la vez.");
                break;
            }
            files.add(new SelectedFile(path));
            added++;
        }
        if (added == 0) {
            return;
        }
        updateFileList();
        if (discovery == null || !scanning) {
            startScan();
        }
    }

    private void updateFileList() {
        int count = files.size();
        if (count == 0) {
            fileCount.setText("0 archivos seleccionados");
        } else if (count == 1) {
            fileCount.setText("1 archivo seleccionado");
        } else {
            fileCount.setText(String.format(Locale.getDefault(), "%d archivos seleccionados", count));
        }
        clearFiles.setEnabled(count > 0 && !transferring);
        fileRows.removeAll();
        if (count == 0) {
            fileRows.add(emptyFiles);
        } else {
            for (SelectedFile file : new ArrayList<>(files)) {
                fileRows.add(new Components.FileRow(file.getPath(), file.getDisplayName(),
                        file.getSize(), () -> removeFile(file)));
            }
        }
        fileRows.revalidate();
        fileRows.repaint();
        fileListWrap.revalidate();
        fileListWrap.repaint();
    }

    private void removeFile(SelectedFile file) {
        if (transferring) {
            return;
        }
        files.remove(file);
        updateFileList();
        if (files.isEmpty()) {
            stopDiscovery();
            showIdle();
        }
    }

    private void clearFiles() {
        if (transferring) {
            return;
        }
        files.clear();
        updateFileList();
        stopDiscovery();
        showIdle();
    }

    private void showIdle() {
        if (rightCards == null) {
            return;
        }
        rightCards.show(rightHost, "discover");
        scanStatus.setText("Añade archivos para buscar dispositivos.");
        scanHint.setText("Arrastra archivos o haz clic en la zona de la izquierda.");
        scanSpinner.setVisible(false);
        scanButton.setVisible(false);
        emptyDevices.setText("Aún no hay dispositivos.");
        deviceKeys.clear();
        deviceList.removeAll();
        deviceList.add(emptyDevices);
        deviceList.revalidate();
        deviceList.repaint();
    }

    private void startScan() {
        if (transferring || destroyed || files.isEmpty()) {
            if (files.isEmpty()) {
                showIdle();
            }
            return;
        }
        stopDiscovery();
        scanning = true;
        deviceKeys.clear();
        deviceList.removeAll();
        emptyDevices.setText("Buscando…");
        deviceList.add(emptyDevices);
        deviceList.revalidate();
        deviceList.repaint();
        rightCards.show(rightHost, "discover");
        scanStatus.setText("Buscando dispositivos…");
        scanHint.setText("El móvil o tablet debe estar en la pantalla Recibir.");
        scanSpinner.setVisible(true);
        scanButton.setVisible(false);

        discovery = new DiscoveryClient(new DiscoveryClient.Listener() {
            @Override
            public void onDevice(DeviceInfo device) {
                post(() -> addDevice(device));
            }

            @Override
            public void onFinished(String errorMessage) {
                post(() -> finishScan(errorMessage));
            }
        });
        discovery.start();
    }

    private void addDevice(DeviceInfo device) {
        if (destroyed || transferring || !deviceKeys.add(device.key())) {
            return;
        }
        if (emptyDevices.getParent() == deviceList) {
            deviceList.remove(emptyDevices);
        }
        Components.DeviceRow row = new Components.DeviceRow(device.name,
                device.address + " · puerto " + device.port,
                () -> startTransfer(device));
        row.setAlignmentX(LEFT_ALIGNMENT);
        deviceList.add(row);
        deviceList.add(Box.createVerticalStrut(8));
        deviceList.revalidate();
        deviceList.repaint();
    }

    private void finishScan(String errorMessage) {
        if (destroyed || transferring) {
            return;
        }
        scanning = false;
        discovery = null;
        scanSpinner.setVisible(false);
        scanButton.setVisible(true);
        if (errorMessage != null) {
            scanStatus.setText("No se pudo buscar dispositivos.");
            scanHint.setText(errorMessage);
            return;
        }
        if (deviceKeys.isEmpty()) {
            scanStatus.setText("No se encontraron dispositivos.");
            scanHint.setText("Comprueba que estén en la misma WiFi y que el móvil esté en Recibir.");
            emptyDevices.setText("No hay dispositivos disponibles.");
            if (emptyDevices.getParent() != deviceList) {
                deviceList.add(emptyDevices);
            }
        } else {
            scanStatus.setText("Dispositivos encontrados.");
            scanHint.setText("Toca un dispositivo para empezar la transferencia.");
            if (emptyDevices.getParent() == deviceList) {
                deviceList.remove(emptyDevices);
            }
        }
        deviceList.revalidate();
        deviceList.repaint();
    }

    private void startTransfer(DeviceInfo device) {
        if (transferring || destroyed || files.isEmpty()) {
            return;
        }
        stopDiscovery();
        target = device;
        transferring = true;
        dropZone.setEnabled(false);
        clearFiles.setEnabled(false);
        rightCards.show(rightHost, "transfer");
        transferTitle.setText("Conectando…");
        transferTitle.setForeground(Theme.INK);
        transferDetail.setText("<html><div style='width:320px'>Conectando con "
                + UiUtils.escapeHtml(device.name) + "…</div></html>");
        transferBar.setValue(0);
        transferPercent.setText("0 %");
        setTransferAction(ACTION_CANCEL, "Cancelar envío");

        transfer = new TransferClient(device, new ArrayList<>(files), new TransferClient.Listener() {
            @Override
            public void onStarted(int fileCount, long totalKnownBytes) {
                post(() -> transferDetail.setText(AppConfig.fileCount(fileCount)));
            }

            @Override
            public void onFileStarted(int index, String name, long size,
                                      long totalSent, long totalKnownBytes) {
                post(() -> {
                    if (!transferring) {
                        return;
                    }
                    transferTitle.setText("Enviando archivos");
                    transferDetail.setText("<html><div style='width:320px'>Archivo "
                            + index + " de " + files.size() + " · "
                            + UiUtils.escapeHtml(name) + "</div></html>");
                });
            }

            @Override
            public void onProgress(int index, String name, long fileBytes, long fileSize,
                                   long totalSent, long totalKnownBytes) {
                post(() -> {
                    if (!transferring) {
                        return;
                    }
                    int percent;
                    if (totalKnownBytes > 0) {
                        percent = AppConfig.percent(totalSent, totalKnownBytes);
                    } else if (fileSize > 0) {
                        percent = AppConfig.percent(fileBytes, fileSize);
                    } else {
                        percent = 0;
                    }
                    transferBar.setValue(percent * 10);
                    String amount;
                    if (fileSize > 0) {
                        amount = AppConfig.formatBytes(fileBytes) + " de "
                                + AppConfig.formatBytes(fileSize);
                    } else {
                        amount = AppConfig.formatBytes(fileBytes) + " enviados";
                    }
                    transferPercent.setText(percent + " % · " + amount);
                    transferDetail.setText("<html><div style='width:320px'>Archivo "
                            + index + " de " + files.size() + " · "
                            + UiUtils.escapeHtml(name) + "</div></html>");
                });
            }

            @Override
            public void onFileSent(int index, String name) {
                // Progress already updates the interface.
            }

            @Override
            public void onComplete(int fileCount, long totalSent) {
                post(() -> {
                    transferring = false;
                    transfer = null;
                    dropZone.setEnabled(true);
                    clearFiles.setEnabled(!files.isEmpty());
                    transferTitle.setText("Enviado");
                    transferTitle.setForeground(Theme.RECEIVE);
                    transferBar.setValue(1000);
                    transferPercent.setText("100 %");
                    transferDetail.setText("<html><div style='width:320px'>Enviados a "
                            + UiUtils.escapeHtml(target == null ? "" : target.name)
                            + ": " + AppConfig.fileCount(fileCount) + ".</div></html>");
                    setTransferAction(ACTION_DONE, "Listo");
                });
            }

            @Override
            public void onError(String message) {
                post(() -> {
                    transferring = false;
                    transfer = null;
                    dropZone.setEnabled(true);
                    clearFiles.setEnabled(!files.isEmpty());
                    transferTitle.setText("No se pudo enviar");
                    transferTitle.setForeground(Theme.ERROR);
                    transferDetail.setText("<html><div style='width:320px'>"
                            + UiUtils.escapeHtml(message).replace("\n", "<br>")
                            + "</div></html>");
                    transferBar.setValue(0);
                    transferPercent.setText("");
                    setTransferAction(ACTION_RESCAN, "Buscar de nuevo");
                });
            }
        });
        transfer.start();
    }

    private void handleTransferAction() {
        if (actionMode == ACTION_CANCEL) {
            confirmCancel();
        } else if (actionMode == ACTION_DONE || actionMode == ACTION_RESCAN) {
            rightCards.show(rightHost, "discover");
            startScan();
        }
    }

    private void setTransferAction(int mode, String text) {
        actionMode = mode;
        transferAction.setText(text);
    }

    private void confirmCancel() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Los archivos que ya se hayan recibido se conservarán.",
                "¿Cancelar la transferencia?", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        cancelTransfer();
        transferring = false;
        dropZone.setEnabled(true);
        clearFiles.setEnabled(!files.isEmpty());
        rightCards.show(rightHost, "discover");
        startScan();
    }

    private void cancelTransfer() {
        if (transfer != null) {
            transfer.cancel();
            transfer = null;
        }
        transferring = false;
    }

    private void stopDiscovery() {
        scanning = false;
        if (discovery != null) {
            discovery.stop();
            discovery = null;
        }
    }

    private void handleBack() {
        if (transferring) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Si sales ahora se cancelará la transferencia en curso.",
                    "¿Cancelar el envío?", JOptionPane.YES_NO_OPTION,
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

    private JLabel wrappedLabel(String text, int size, Color color, int width) {
        JLabel label = new JLabel("<html><div style='width:" + width + "px'>"
                + UiUtils.escapeHtml(text).replace("\n", "<br>") + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static String shortMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
