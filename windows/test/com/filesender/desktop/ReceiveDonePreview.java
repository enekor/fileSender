package com.filesender.desktop;

import com.filesender.desktop.net.TransferServer;
import com.filesender.desktop.ui.MainFrame;
import com.filesender.desktop.ui.ReceivePanel;
import com.filesender.desktop.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

public final class ReceiveDonePreview {

    public static void main(String[] args) throws Exception {
        Theme.install();
        final MainFrame[] frameHolder = new MainFrame[1];
        final ReceivePanel[] panelHolder = new ReceivePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            ReceivePanel panel = new ReceivePanel(frame);
            frame.setScreen(panel);
            frame.setLocation(40, 40);
            frame.setVisible(true);
            frameHolder[0] = frame;
            panelHolder[0] = panel;
        });
        Thread.sleep(1200);
        Field listenerField = ReceivePanel.class.getDeclaredField("serverListener");
        listenerField.setAccessible(true);
        TransferServer.Listener listener =
                (TransferServer.Listener) listenerField.get(panelHolder[0]);
        listener.onSessionStarted(2, 9090);
        listener.onFileSaved(1, "desde_android_prueba.txt", "x");
        listener.onFileSaved(2, "foto_prueba.bin", "y");
        listener.onComplete(2, 9090);
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(() -> {
            try {
                MainFrame frame = frameHolder[0];
                frame.getContentPane().doLayout();
                BufferedImage image = new BufferedImage(
                        frame.getContentPane().getWidth(),
                        frame.getContentPane().getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                frame.getContentPane().paint(g);
                g.dispose();
                File out = new File("windows/build/screens/receive_done_preview.png");
                out.getParentFile().mkdirs();
                ImageIO.write(image, "png", out);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SwingUtilities.invokeAndWait(frameHolder[0]::dispose);
        System.exit(0);
    }
}
