package com.filesender.desktop;

import com.filesender.desktop.ui.MainFrame;
import com.filesender.desktop.ui.ReceivePanel;
import com.filesender.desktop.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ReceivePreview {

    public static void main(String[] args) throws Exception {
        Theme.install();
        final MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            holder[0] = frame;
            ReceivePanel panel = new ReceivePanel(frame);
            frame.setScreen(panel);
            frame.setLocation(40, 40);
            frame.setVisible(true);
        });
        Thread.sleep(1800);
        MainFrame frame = holder[0];
        SwingUtilities.invokeAndWait(() -> {
            try {
                frame.getContentPane().doLayout();
                BufferedImage image = new BufferedImage(
                        frame.getContentPane().getWidth(),
                        frame.getContentPane().getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                frame.getContentPane().paint(g);
                g.dispose();
                File out = new File("windows/build/screens/receive_preview.png");
                out.getParentFile().mkdirs();
                ImageIO.write(image, "png", out);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SwingUtilities.invokeAndWait(frame::dispose);
        System.exit(0);
    }
}
