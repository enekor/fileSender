package com.filesender.desktop;

import com.filesender.desktop.ui.MainFrame;
import com.filesender.desktop.ui.SendPanel;
import com.filesender.desktop.ui.Theme;

import javax.swing.SwingUtilities;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UiPreview {

    public static void main(String[] args) throws Exception {
        Theme.install();
        List<File> files = new ArrayList<>();
        for (String arg : args) {
            File file = new File(arg);
            if (file.isFile()) {
                files.add(file);
            }
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                MainFrame frame = new MainFrame();
                SendPanel panel = new SendPanel(frame);
                frame.setScreen(panel);
                frame.setVisible(true);
                if (!files.isEmpty()) {
                    Method addFiles = SendPanel.class.getDeclaredMethod("addFiles", List.class);
                    addFiles.setAccessible(true);
                    addFiles.invoke(panel, files);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
