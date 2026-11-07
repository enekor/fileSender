package com.filesender.desktop.ui;

import com.filesender.desktop.util.AppConfig;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class MainFrame extends JFrame {

    public interface Screen {
        JComponent component();

        void onHide();
    }

    private final JPanel content = new JPanel(new BorderLayout());
    private Screen current;

    public MainFrame() {
        super(AppConfig.APP_NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1020, 740);
        setLocationRelativeTo(null);
        content.setOpaque(true);
        content.setBackground(Theme.BG);
        setContentPane(content);
        loadIcon();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (current != null) {
                    current.onHide();
                }
                dispose();
                System.exit(0);
            }
        });
        showHome();
    }

    public void showHome() {
        setScreen(new HomePanel(this));
    }

    public void showReceive() {
        setScreen(new ReceivePanel(this));
    }

    public void showSend() {
        setScreen(new SendPanel(this));
    }

    public void setScreen(Screen next) {
        if (current != null) {
            current.onHide();
        }
        content.removeAll();
        current = next;
        content.add(next.component(), BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private void loadIcon() {
        try (InputStream stream = MainFrame.class.getResourceAsStream("/icon.png")) {
            if (stream == null) {
                return;
            }
            Image image = ImageIO.read(stream);
            if (image == null) {
                return;
            }
            List<Image> images = new ArrayList<>();
            images.add(image);
            setIconImages(images);
        } catch (Exception ignored) {
            // The executable icon still identifies the app.
        }
    }
}
