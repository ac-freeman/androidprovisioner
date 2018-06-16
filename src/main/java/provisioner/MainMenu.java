package provisioner;

import static dataLogger.CreateFolder.CreateFolder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import protocol.DownloadApps;
import provisioner.ProvisioningActions;

import protocol.UploadObject;
import static junit.framework.Assert.assertEquals;
import static provisioner.ProvisioningActions.ProvisioningActions;

/**
 * Main backbone of the program. MainMenu instantiated once LoadingScreen has
 * displayed JFrame
 *
 * @author andrew
 */
public class MainMenu extends javax.swing.JFrame {

    public static int PANEL_WIDTH = 600;
    public static int PANEL_HEIGHT = 300;

    /**
     * How many devices should be provisioned at once. This limits ADB and USB
     * overload.
     */
    public static int SIMULATANEOUS_DEVICES = 3;
    public static String PROVISIONER_VERSION = "0.11";

    public static int lastDeviceCount;

    MainMenu() throws IOException, URISyntaxException, Exception {
        boolean cont = true;
        try {
            final URL url = new URL("http://www.google.com");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        } catch (IOException e) {
            System.err.println("Error creating HTTP connection");
            e.printStackTrace();
            cont = false;
            Object[] options = {"OK"};
            int confirmed = JOptionPane.showOptionDialog(null,
                    "For security purposes, please connect to the Internet to use this program.", "WARNING",
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (confirmed == JOptionPane.YES_OPTION) {
                System.exit(0);
                throw e;
            }
        }
        if (cont) {
            initComponents(null);
        }

    }

    /**
     * Called when LoadingScreen has been made visible
     *
     * Tests Web connection by trying to reach google.com Only continues with
     * provisioning if computer is connected to the Internet
     *
     * @param loader The loadingScreen instance passed in the constructor
     * @throws URISyntaxException TODO
     * @throws Exception TODO
     */
    MainMenu(provisioner.LoadingScreen loader) throws URISyntaxException, Exception {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        boolean cont = true;
        try {
            final URL url = new URL("http://www.google.com");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        } catch (IOException e) {
            System.err.println("Error creating HTTP connection");
            e.printStackTrace();
            cont = false;
            Object[] options = {"OK"};
            int confirmed = JOptionPane.showOptionDialog(null,
                    "For security purposes, please connect to the Internet to use this program.", "WARNING",
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (confirmed == JOptionPane.YES_OPTION) {
                System.exit(0);
                throw e;
            }
        }

        /**
         * If connected to the Internet, continue in the program
         */
        if (cont) {
            initComponents(loader);
            this.setVisible(true);
            loader.setVisible(false);
        }
    }

    private javax.swing.JComboBox<String> jComboBox1;       //The dropdown menu for selecting a preset
    public static String selectedPreset = null;             //The currently selected preset
    public static Report reportFrame = null;                //The report frame (instantiated after provisioning begins) [TODO]
    public String deviceId;                                 //A connected device's android_id
    public int activeInstalls;                              //The number of active installs (connected devices * apps installing) [TODO: correct?]
    public static UploadObject uploader;                    //Will upload any existing local files to S3 landing zone
    public static DownloadApps downloader;                  //Will download all appropriate files from S3 app bucket

    /**
     *
     * @param loader The loadingScreen instance passed in the constructor
     * @throws IOException
     * @throws URISyntaxException
     * @throws Exception
     */
    private void initComponents(LoadingScreen loader) throws IOException, URISyntaxException, Exception {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        /**
         * Instantiate an UploadObject -- Upload any local log files to S3
         */
        uploader = new UploadObject();

        /**
         * Instantiate a DownloadApps -- Downlaod all appropriate files from S3
         */
        downloader = new DownloadApps(loader);

        while (!downloader.finished) {
            //stall and wait for all apps to be completely downloaded
        };

        /**
         * Create an array of all local files (.apk files, and presets.txt)
         */
        String path = System.getProperty("user.dir") + "/.all_apps/";
        File folder = CreateFolder(path);
        File[] listOfFiles = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".apk");
            }
        });

        /**
         * Create the app selection pane (on the left) of the JFrame
         */
        CheckModel model = new CheckModel(listOfFiles.length);
        Command c = new Command();
        JTable table = new JTable(model) {

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
            }

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                JCheckBox jcb = (JCheckBox) super.prepareRenderer(renderer, row, column);
                jcb.setHorizontalTextPosition(JCheckBox.RIGHT);
                jcb.setHorizontalAlignment(SwingConstants.LEFT);
                jcb.setText(listOfFiles[row].getName());
                return jcb;
            }
        };
        JScrollPane left = new JScrollPane(table);

        /**
         * Create the selected app list pane (on the right) of the JFrame
         */
        DisplayPanel right = new DisplayPanel(model, listOfFiles);

        /**
         * Create the dropdown menu for presets, and define its behavior
         */
        jComboBox1 = new javax.swing.JComboBox<>(getPresetNames());
        jComboBox1.addActionListener((ActionEvent e) -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(false, i, 0);
            }
            JComboBox box = (JComboBox) e.getSource();
            int index = box.getSelectedIndex();
            selectedPreset = (String) box.getItemAt(index);
            String[] presetApps = null;
            try {
                //Get and split text line at this index
                presetApps = getPresetApps(index);

            } catch (IOException ex) {
                Logger.getLogger(MainMenu.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

            /**
             * Find every checkbox with the same name, and set those boxes to
             * true
             */
            for (String app : presetApps) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (app.equals(listOfFiles[i].getName())) {
                        model.setValueAt(true, i, 0);
                    }
                }
            }
        });

        JLabel icon = new javax.swing.JLabel();
        icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/logo.png")));

        JLabel jLabel1 = new javax.swing.JLabel();
        JLabel jLabel2 = new javax.swing.JLabel();
        jLabel1.setText("Presets:");
        jLabel2.setText(countDevices() + " devices connected");
        jLabel2.setFont(jLabel2.getFont().deriveFont(16.0f));

        /**
         * Create the Begin installation button, and define its behavior
         */
        JButton jButton1 = new javax.swing.JButton();
        jButton1.setHorizontalAlignment(SwingConstants.CENTER);
        jButton1.setText("Begin installation");
        Font font = jButton1.getFont();
        font = font.deriveFont(
                Collections.singletonMap(
                        TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
        jButton1.setFont(font);
        jButton1.addActionListener((ActionEvent e) -> {
            Boolean cont = false;
            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean cur = (Boolean) model.getValueAt(i, 0);
                if (cur) {
                    cont = true;
                }

            }
            if (lastDeviceCount == 0 || !cont) {
                //display message
                Object[] options = {"OK"};
                int confirmed = JOptionPane.showOptionDialog(null,
                        "Make sure you have apps selected and devices plugged in!", "WARNING",
                        JOptionPane.PLAIN_MESSAGE,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
            } else {
                ProvisioningActions(model, listOfFiles, MainMenu.this);
            }
        }); //End of "Begin Installation" button creation
        //TODO: clean up all this code--pull it out into separate classes and methods

        /**
         * Create "Refresh devices" button, clicked by operator to verify that
         * all connected devices are seen by the program
         */
        JButton refreshButton = new javax.swing.JButton();
        refreshButton.setHorizontalAlignment(SwingConstants.CENTER);
        refreshButton.setText("Refresh devices");
        refreshButton.addActionListener((ActionEvent e) -> {
            try {
                jLabel2.setText(countDevices() + " devices connected");
                System.out.println(Thread.activeCount() + " threads");
            } catch (IOException ex) {
                Logger.getLogger(MainMenu.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        });

        setTitle("Android Provisioning Tool v" + PROVISIONER_VERSION);

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/applogo.png")));

        /**
         * Set up the GraphicReport frame's layout
         */
        // <editor-fold defaultstate="collapsed" desc="Setup GUI">
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        //.addContainerGap()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(left, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(right, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        //.addContainerGap())
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                        .addComponent(jLabel2)
                        .addGap(5, 5, 5)
                        .addComponent(refreshButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 350, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 350, Short.MAX_VALUE)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 350, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGap(5, 5, 5)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                        .addComponent(icon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(icon)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(left, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel1))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        //.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(right, javax.swing.GroupLayout.PREFERRED_SIZE, 273, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(4, 4, 4)
                                        .addComponent(jLabel2))
                                .addComponent(refreshButton)
                                .addComponent(jButton1))
                        .addContainerGap(23, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);

//      </editor-fold>
    }

    /**
     * Get the name of each preset
     *
     * @return String[] with the name of each preset (ex: "California", "Texas")
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String[] getPresetNames() throws FileNotFoundException, IOException {

        String filePath = System.getProperty("user.dir") + "/.all_apps/presets.txt";
        BufferedReader input = new BufferedReader(new FileReader(filePath));
        List<String> presets = new ArrayList<String>();
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                String arr[] = line.split("\\s+");
                String name = arr[0].substring(0, arr[0].length() - 1);
                presets.add(name);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error, file " + filePath + " didn't exist.");
        } finally {
            input.close();
        }

        String[] lineArray = presets.toArray(new String[]{});
        return lineArray;
    }

    /**
     * Get the apps included in a selected preset
     *
     * @param index The line number for the preset selected
     * @return String[] with the filename for each app in the preset
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String[] getPresetApps(int index) throws FileNotFoundException, IOException {
        //TODO: use the same BufferedReader from "getPresetNames", to reduce overhead
        String filePath = System.getProperty("user.dir") + "/.all_apps/presets.txt";
        BufferedReader input = new BufferedReader(new FileReader(filePath));
        List<String> presetApps = new ArrayList<String>();
        try {
            String line = null;
            for (int i = 0; i < index; i++) {
                input.readLine();
            }
            line = input.readLine();
            String arr[] = line.split("\\s+");

            if (arr.length > 1) { //ensures that the preset is not empty
                for (int i = 1; i < arr.length; i++) {
                    presetApps.add(arr[i]);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error, file " + filePath + " didn't exist.");
        } finally {
            input.close();
        }

        String[] appsArray = presetApps.toArray(new String[]{});
        return appsArray;
    }

    /**
     * Count the number of devices detected by ADB
     *
     * @return int with the number of devices connected
     * @throws IOException
     */
    public static int countDevices() throws IOException {
        int count = 0;
        Command c = new Command();

        String[] cmd = new String[]{"adb", "kill-server"};  //restart adb to ensure clean device list
        c.Command(cmd, false);

        cmd = new String[]{"adb", "devices", "-l"};

        StringBuilder builder = c.Command(cmd, false);
        String result = builder.toString();

        String[] cmdLines = result.split("\\r?\\n");
        for (String l : cmdLines) {
            String[] lArgs = l.split("\\s+");
            if (l != null) {
                if (lArgs.length > 0) {
                    if (lArgs[1].equals("device")) {

                        count++;
                    }
                }
            }
        }
        lastDeviceCount = count;
        return count;
    }

    /**
     * Get the name of the computer, for data logging
     *
     * @see Command
     * @return The computer's name
     */
    public static String getHost() {
        String hostname = "Unknown";

        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Hostname can not be resolved");
        }
        return hostname;
    }

    /**
     * Delete the folder specified
     *
     * @param folder The folder to delete
     */
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();

    }

    private static class DisplayPanel extends JPanel {

        private DefaultListModel dlm = new DefaultListModel();
        private JList list = new JList(dlm);

        public DisplayPanel(final CheckModel model, File[] listOfFiles) {
            super(new GridLayout());
            this.setBorder(BorderFactory.createTitledBorder("Apps selected"));
            this.add(new JScrollPane(list));
            this.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT + 20));
            model.addTableModelListener((TableModelEvent e) -> {
                dlm.removeAllElements();
                selectedPreset = null;
                model.checked.stream().forEach((num) -> {
                    dlm.addElement(listOfFiles[num].getName());
                });
            });
        }
    }
}
