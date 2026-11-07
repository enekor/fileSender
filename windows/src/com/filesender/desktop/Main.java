package com.filesender.desktop;

import com.filesender.desktop.ui.MainFrame;
import com.filesender.desktop.ui.Theme;
import com.filesender.desktop.util.AppConfig;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public final class Main {

    private static FileChannel lockChannel;
    private static FileLock lock;

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            Theme.install();
        } catch (Exception e) {
            // The system look and feel is a safe fallback.
        }

        try {
            Path folder = AppConfig.appDataDir();
            Files.createDirectories(folder);
            lockChannel = FileChannel.open(folder.resolve("app.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = lockChannel.tryLock();
            if (lock == null) {
                JOptionPane.showMessageDialog(null,
                        "FileSender ya está abierto en este equipo.",
                        "FileSender", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (Exception ignored) {
            // A lock is a convenience, not a requirement.
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log(throwable);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Ha ocurrido un error inesperado:\n" + shortMessage(throwable),
                    "FileSender", JOptionPane.ERROR_MESSAGE));
        });

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    private static void log(Throwable throwable) {
        try {
            Path folder = AppConfig.appDataDir();
            Files.createDirectories(folder);
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            Files.writeString(folder.resolve("error.log"),
                    LocalDateTime.now() + "\n" + writer + "\n\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Logging must never hide the original failure.
        }
    }

    private static String shortMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }
}
