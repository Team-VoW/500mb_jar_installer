package com.voicesofwynn.installer;

import com.voicesofwynn.installer.utils.FileUtils;
import com.voicesofwynn.installer.utils.WebUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static String IP = "localhost";
    public static int PORT = 7878;

    public static void main(String[] args) {

        JFrame jFrame = new JFrame("Voices Of Wynn Installer");

        try {
            Image img = ImageIO.read(Main.class.getResource("/wynnvplogo.png"));
            jFrame.setIconImage(img);
            final Taskbar taskbar = Taskbar.getTaskbar();
            taskbar.setIconImage(img);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Map<String, WebUtil.remoteJar> options;
        Map<String, WebUtil.remoteJar> options1;
        try {
            options1 = WebUtil.getRemoteJarsFromCSV();
        } catch (Exception e) {
            options1 = null;
            e.printStackTrace();
            JOptionPane.showMessageDialog(jFrame, "Unable to connect to server!");
            System.exit(0);
        }
        options = options1;


        // logo
        JButton logo = new JButton();
        try {
            ImageIcon icon = new ImageIcon(ImageIO.read(Main.class.getResource("/wynnvplogo.png")).getScaledInstance(350, 350, Image.SCALE_FAST));
            logo.setIcon(icon);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logo.addActionListener(e -> {
            try {
                WebUtil.openWebpage(new URI("https://voicesofwynn.com"));
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });
        logo.setBounds(0, 0, 350, 350);

        // download chooser
        JComboBox<String> downloadChoose = new JComboBox<>();
        JLabel downloadLabel = new JLabel("Download:");
        downloadChoose.setBounds(0, 400, 350, 50);
        downloadLabel.setBounds(0, 380, 350, 50);

        for (Map.Entry<String, WebUtil.remoteJar> jar : options.entrySet()) {
            downloadChoose.addItem(jar.getKey());
        }


        // file chooser
        JTextField path = new JTextField(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        path.setBounds(0, 500, 300, 50);
        JButton chooserOpener = new JButton();
        chooserOpener.setText("open");
        chooserOpener.setBounds(300, 500, 50, 50);

        JLabel downloadToLabel = new JLabel("Download to:");
        downloadToLabel.setBounds(0, 470, 350, 50);

        JTextArea downloadToRecommendationLabel = new JTextArea("If you already have a version Voices of wynn please \nchoose it because it will speed up your download to at least 5x times faster!");
        downloadToRecommendationLabel.setBounds(0, 550, 350, 100);
        downloadToRecommendationLabel.setEditable(false);
        downloadToRecommendationLabel.setLineWrap(true);

        path.addActionListener(e -> {
            path.setText(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        });

        downloadChoose.addActionListener(a -> {
            path.setText(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        });

        chooserOpener.addActionListener(e -> {
            JFrame open = new JFrame("browse");

            JFileChooser chooser = new JFileChooser(new File(path.getText()));
            File loc = new File(path.getText());
            if (loc.isFile()) loc = loc.getParentFile();
            chooser.setCurrentDirectory(loc);

            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".jar") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return null;
                }
            });

            chooser.addActionListener(actionEvent -> {
                path.setText(FileUtils.getPreferredFileLocation(chooser.getSelectedFile().getPath(), options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
            });

            chooser.showOpenDialog(open);

        });

        // install button
        JButton install = new JButton();
        install.setText("Install");
        install.setBounds(0, 625, 350, 50);

        // feedback
        JLabel feedback = new JLabel();
        feedback.setBounds(0, 580, 350, 50);
        JProgressBar progress = new JProgressBar();
        progress.setBounds(0, 600, 350, 50);

        jFrame.setLayout(null);
        jFrame.add(logo);
        jFrame.add(downloadChoose);
        jFrame.add(path);
        jFrame.add(chooserOpener);
        jFrame.add(install);
        jFrame.add(feedback);
        jFrame.add(progress);

        jFrame.add(downloadToLabel);
        jFrame.add(downloadLabel);
        jFrame.add(downloadToRecommendationLabel);

        jFrame.setSize(350, 700);

        jFrame.setResizable(false);

        jFrame.setVisible(true);

        AtomicBoolean working = new AtomicBoolean(false);

        install.addActionListener(ev -> {
            if (working.get()) return;
            working.set(true);

            InstallerOut out = new InstallerOut() {
                @Override
                public void outState(String str, int done, int needed) {
                    feedback.setText(str);
                    progress.setMaximum(needed);
                    progress.setValue(done);
                }
            };

            new Thread(() -> {
                try {
                    File f = new File(path.getText());
                    if (f.exists()) {
                        int o = JOptionPane.showConfirmDialog(jFrame, "By proceeding the file [" + f.getPath() + "] will be overwritten.");

                        if (o != JOptionPane.OK_OPTION) {
                            return;
                        }
                    }
                    WebUtil.remoteJar jar = options.get((String) downloadChoose.getSelectedItem());

                    Installer.install(f, out, jar.id());
                    feedback.setText("Done");
                    progress.setValue(100);
                    progress.setMaximum(100);
                    String rec = jar.recommendedFileName();
                    if (!f.getName().equals(rec)) {
                        int o = JOptionPane.showConfirmDialog(jFrame, "Would you like to rename " + f.getName() + " to recommended " + rec + "?");

                        if (o == JOptionPane.OK_OPTION) {
                            f.renameTo(new File(f.getParent() + "/" + rec));
                        }
                    }

                    JOptionPane.showMessageDialog(jFrame, "Done!");
                    working.set(false);

                } catch (Exception e) {
                    e.printStackTrace();
                    out.outState("Something went wrong, please retry", 1, 1);
                    working.set(false);
                    feedback.setText("Failed");
                    JOptionPane.showMessageDialog(jFrame, "Failed to download the file :( \nPlease retry in a bit.");
                }
            }).start();
        });


        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
