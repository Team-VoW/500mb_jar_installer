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
import java.util.Calendar;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        JFrame jFrame = new JFrame("Voices Of Wynn Installer");

        // killswitch of the Alpha version
        int year = Calendar.getInstance().get(Calendar.YEAR);
        if (year > 2022) {
            System.out.println("2022 has passed and this installer jar is now dead.");
            JFrame f = new JFrame();
            JOptionPane.showMessageDialog(f, "This installer jar does not work anymore. Please update.");
            return;
        }

        try {
            Image img = ImageIO.read(Main.class.getResource("/wynnvplogo.png"));
            jFrame.setIconImage(img);

        } catch (Exception e) {
            System.out.println("No taskbar?");
        }

        // load the list of available versions
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
            logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        // download chooser select box
        JComboBox<String> downloadChoose = new JComboBox<>();
        JLabel downloadLabel = new JLabel("<html><strong>Version To Download</strong></html>");
        downloadChoose.setMaximumSize(new Dimension(430, 30));
        downloadLabel.setHorizontalAlignment(SwingConstants.CENTER);

        for (Map.Entry<String, WebUtil.remoteJar> jar : options.entrySet()) {
            downloadChoose.addItem(jar.getKey());
        }

        // file chooser text box
        JTextField path = new JTextField(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        JButton chooserOpener = new JButton();
        chooserOpener.setText("Open");

        JLabel downloadToLabel = new JLabel("<html><strong>Download To</strong></html>");

        // note label
        JLabel downloadToRecommendationLabel = new JLabel("<html>If you already have a Voices of Wynn jar downloaded, please choose it, because it will profusely speed up your download!</html>");
        downloadToRecommendationLabel.setHorizontalAlignment(SwingConstants.HORIZONTAL);
        downloadToRecommendationLabel.setMaximumSize(new Dimension(430, 50));

        path.addActionListener(e -> {
            path.setText(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
        });

        downloadChoose.addActionListener(a -> {
            File f = new File(path.getText());
            if (!f.exists()) {
                path.setText(FileUtils.getPreferredFileLocation(null, options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
            }
        });

        // file select dialogue
        chooserOpener.addActionListener(e -> {
            JFrame open = new JFrame("Browse");

            JFileChooser chooser = new JFileChooser(new File(path.getText()));
            File loc = new File(path.getText());
            loc = loc.isFile() ? loc.getParentFile() : loc;

            chooser.setCurrentDirectory(loc.isDirectory() ? loc : loc.getParentFile());

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
                if (actionEvent.getActionCommand().equals("ApproveSelection"))
                    path.setText(FileUtils.getPreferredFileLocation(chooser.getSelectedFile().getPath(), options.get((String) downloadChoose.getSelectedItem()).recommendedFileName()));
            });

            chooser.showOpenDialog(open);

        });

        // path text box settings
        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.X_AXIS));
        pathPanel.setMaximumSize(new Dimension(430, 30));
        path.setBounds(new Rectangle(400, 40));
        pathPanel.add(path);
        pathPanel.add(chooserOpener);

        // install button
        JButton install = new JButton();
        install.setText("Install");
        install.setMaximumSize(new Dimension(430, 30));

        // feedback
        JLabel feedback = new JLabel();
        JProgressBar progress = new JProgressBar();
        progress.setMaximumSize(new Dimension(430, 10));
        feedback.setHorizontalAlignment(SwingConstants.CENTER);

        downloadToLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // footing
        JPanel footing = new JPanel();
        footing.setMaximumSize(new Dimension(15, 10));

        // align everything to center
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
            Color bc = new Color(204, 208, 207);

            downloadChoose.setBackground(bc);
            install.setBackground(bc);
            chooserOpener.setBackground(bc);

            logo.setFocusPainted(false);
        }


        // put everything together
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


        jFrame.setSize(450, 650);

        jFrame.setResizable(false);

        jFrame.setVisible(true);

        // install button action
        final Thread[] t = new Thread[1];
        install.addActionListener(ev -> {
            if (t[0] != null && t[0].isAlive()) {
                // cancel current operation and reenable controls
                t[0].stop();
                path.setEnabled(true);
                chooserOpener.setEnabled(true);
                downloadChoose.setEnabled(true);
                install.setText("Install");
                feedback.setText("Cancelled, (The download cache folder still exists in that folder.)");
                return;
            }

            // disable controls
            path.setEnabled(false);
            chooserOpener.setEnabled(false);
            downloadChoose.setEnabled(false);
            install.setText("Cancel");

            // progress reporting
            InstallerOut out = new InstallerOut() {
                @Override
                public void outState(String str, int done, int needed) {
                    feedback.setText(str);
                    progress.setMaximum(needed);
                    progress.setValue(done);
                }

                @Override
                public void corruptJar() {
                    JOptionPane.showMessageDialog(jFrame, "The jar provided is corrupt so the mod will have to be downloaded from 0.");
                }
            };

            File f = new File(path.getText());
            if (f.exists()) {
                int o = JOptionPane.showConfirmDialog(jFrame, "By proceeding, the file [" + f.getPath() + "] will be overwritten by the update.", "Confirm Update", JOptionPane.YES_NO_OPTION);

                if (o != JOptionPane.OK_OPTION) {
                    // overwrite denied – reenable controls and don't start the installation
                    path.setEnabled(true);
                    chooserOpener.setEnabled(true);
                    downloadChoose.setEnabled(true);
                    install.setText("Install");
                    return;
                }
            }
            WebUtil.remoteJar jar = options.get((String) downloadChoose.getSelectedItem()); // selected version to install

            t[0] = new Thread(() -> { //proceed on a separate thread
                try {
                    Installer.install(f, out, jar.id()); // start the installation
                    feedback.setText(""); // instalation complete – clear the output and set the progress bar to 100 %
                    progress.setValue(100);
                    progress.setMaximum(100);
                    String rec = jar.recommendedFileName();
                    if (!f.getName().equals(rec)) {
                        // update rather than a clean installation, file should be renamed
                        int o = JOptionPane.showConfirmDialog(jFrame, "Update successful.\nShould " + f.getName() + " be renamed to " + rec + "?", "Rename Updated File?", JOptionPane.YES_NO_OPTION);

                        if (o == JOptionPane.OK_OPTION) {
                            // renaming approved
                            f.renameTo(new File(f.getParent() + "/" + rec));
                            path.setText(FileUtils.getPreferredFileLocation(null, jar.recommendedFileName()));
                        }
                    }

                    JOptionPane.showMessageDialog(jFrame, "Done!");

                } catch (Exception e) {
                    e.printStackTrace();
                    out.outState("Something went wrong, please retry", 1, 1);
                    feedback.setText("Failed");
                    JOptionPane.showMessageDialog(jFrame, "Failed to download the file :( \nPlease retry in a bit.");
                }

                // operation finished, reenable controls
                path.setEnabled(true);
                chooserOpener.setEnabled(true);
                downloadChoose.setEnabled(true);
                install.setText("Install");
            });
            t[0].start();
        });


        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
