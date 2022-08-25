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

    public static void main(String[] args) {

        JFrame jFrame = new JFrame("Voices Of Wynn Installer");

        try {
            Image img = ImageIO.read(Main.class.getResource("/wynnvplogo.png"));
            jFrame.setIconImage(img);

            // mac stuff
            final Taskbar taskbar = Taskbar.getTaskbar();
            taskbar.setIconImage(img);
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Voices Of Wynn Installer");
        } catch (Exception e) {
            System.out.println("No taskbar?");
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
        logo.setOpaque(false);
        logo.setContentAreaFilled(false);
        logo.setBorderPainted(false);

        // download chooser
        JComboBox<String> downloadChoose = new JComboBox<>();
        JLabel downloadLabel = new JLabel("<html><b>Version To Download</b></html>");
        downloadChoose.setMaximumSize(new Dimension(350, 30));
        downloadLabel.setHorizontalAlignment(SwingConstants.CENTER);

        for (Map.Entry<String, WebUtil.remoteJar> jar : options.entrySet()) {
            downloadChoose.addItem(jar.getKey());
        }


        // file chooser
        JTextField path = new JTextField(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        JButton chooserOpener = new JButton();
        chooserOpener.setText("Open");

        JLabel downloadToLabel = new JLabel("<html><b>Download To</b></html>");


        JLabel downloadToRecommendationLabel = new JLabel("<html>If you already have a Voices of wynn jar downloaded, <br>please choose it, because it will profusely speed up <br>your download!</html>");
        downloadToRecommendationLabel.setHorizontalAlignment(SwingConstants.HORIZONTAL);

        path.addActionListener(e -> {
            path.setText(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        });

        downloadChoose.addActionListener(a -> {
            path.setText(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        });

        chooserOpener.addActionListener(e -> {
            JFrame open = new JFrame("Browse");

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

        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.X_AXIS));
        pathPanel.setMaximumSize(new Dimension(350, 30));
        path.setBounds(new Rectangle(300, 40));
        downloadChoose.setBounds(new Rectangle(50, 30));
        pathPanel.add(path);
        pathPanel.add(chooserOpener);

        // install button
        JButton install = new JButton();
        install.setText("Install");
        install.setMaximumSize(new Dimension(350, 30));

        // feedback
        JLabel feedback = new JLabel();
        JProgressBar progress = new JProgressBar();
        progress.setMaximumSize(new Dimension(330, 10));
        feedback.setHorizontalAlignment(SwingConstants.CENTER);

        downloadToLabel.setHorizontalAlignment(SwingConstants.CENTER);

        //footing
        JPanel footing = new JPanel();
        footing.setMaximumSize(new Dimension(15, 10));


        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadChoose.setAlignmentX(Component.CENTER_ALIGNMENT);
        path.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooserOpener.setAlignmentX(Component.CENTER_ALIGNMENT);
        install.setAlignmentX(Component.CENTER_ALIGNMENT);
        feedback.setAlignmentX(Component.CENTER_ALIGNMENT);
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadToLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadToRecommendationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String os = System.getProperty("os.name");

        if (os.toLowerCase().contains("windows") || os.toLowerCase().contains("linux")) {
            Color bc = new Color(170, 170, 170);

            downloadChoose.setBackground(bc);
            install.setBackground(bc);
            chooserOpener.setBackground(bc);

            logo.setFocusPainted(false);
        }


        Container contentPane = jFrame.getContentPane();
        BoxLayout l = new BoxLayout(contentPane, BoxLayout.Y_AXIS);

        contentPane.setLayout(l);

        contentPane.add(logo);
        contentPane.add(downloadLabel);
        contentPane.add(downloadChoose);
        contentPane.add(new JPanel());
        contentPane.add(downloadToLabel);
        contentPane.add(pathPanel);
        contentPane.add(downloadToRecommendationLabel);
        contentPane.add(new JPanel());
        contentPane.add(feedback);
        contentPane.add(progress);
        contentPane.add(install);
        contentPane.add(footing);


        jFrame.setSize(370, 700);

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
