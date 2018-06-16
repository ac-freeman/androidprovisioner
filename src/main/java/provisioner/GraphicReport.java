package provisioner;

import static provisioner.MainMenu.SIMULATANEOUS_DEVICES;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 *
 * @author andrew
 */
public class GraphicReport extends javax.swing.JFrame {

    private final MainMenu menu;

    public GraphicReport(String[] devices, File[] files, String[] packageNames, String[] versionCodes, Report reportFrame, MainMenu menu) {
        this.menu = menu;
        initComponents(devices.length + 1, files.length + 1, devices, files, packageNames, versionCodes, reportFrame);
    }

    public int N;
    public String installPath;
    private long timeStart;
    public boolean finished = false;

    private void initComponents(int rows, int columns, String[] devices, File[] files, String[] packageNames, String[] versionCodes, Report reportFrame) {

        installPath = System.getProperty("user.dir") + "/.apps_installing/";
        N = columns - 1;    //for calculating index in getGridButton method

        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;   //fills whole area?

        GridLayout g = new GridLayout(0, columns, 5, 5);
        JPanel panel = new JPanel();
        panel.setLayout(bag);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {

                if (i == 0) {

                    if (j == 0) {
                        JLabel l = new JLabel();
                        c.gridx = j;
                        c.gridy = i;
                        label_list.add(l);
                        panel.add(l, c);
                    }
                    if (j > 0) {
                        JLabel l = createGridLabel(files[j - 1].getName(), i, j);
                        l.setUI(new VerticalLabelUI(false));
                        c.gridx = j;
                        c.gridy = i;
                        label_list.add(l);
                        panel.add(l, c);
                    }

                } else if (j == 0) {
                    JLabel d = createGridLabel(devices[i - 1], i, j);
                    c.gridx = j;
                    c.gridy = i;
                    label_list.add(d);
                    panel.add(d, c);

                } else {
                    JButton gb = createGridButton(i, j, packageNames, versionCodes, reportFrame);
                    c.gridx = j;
                    c.gridy = i;
                    button_list.add(gb);
                    panel.add(gb, c);

                }
            }
        }

        timeStart = System.currentTimeMillis();
        c.gridx = 1;
        c.gridy = rows;
        c.gridwidth = columns - 1;
        c.fill = GridBagConstraints.NONE;
        JLabel time = new JLabel("Time Passed:  " + timePassed() + " sec");
        time.setFont(new Font("Plain", Font.BOLD, 14));
        panel.add(time, c);

        // <editor-fold defaultstate="collapsed" desc="Create GUI">
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int confirmed = JOptionPane.showConfirmDialog(null,
                        "Would you like to provision additional devices?", "Exit",
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {
                    Command c = new Command();
                    StringBuilder builder = new StringBuilder();
                    String installPath = System.getProperty("user.dir");
                    String[] cmd = new String[]{"java", "-jar", installPath + "/provisioner.jar"};
                    GraphicReport.this.setVisible(false);
                    GraphicReport.this.dispose();
                    menu.setVisible(false);
                    menu.dispose();
                    reportFrame.setVisible(false);
                    reportFrame.dispose();

                    try {
                        builder = c.Command(cmd, false);
                    } catch (IOException ex) {
                        Logger.getLogger(GraphicReport.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    System.exit(0);

                } else {
                    System.exit(0);
                }
            }
        });

//        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setTitle("Android Provisioning Tool v" + MainMenu.PROVISIONER_VERSION);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/applogo.png")));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        //.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panel)
                //.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                ));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        //.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panel)
                //.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                ));

        pack();
        setLocationRelativeTo(null);
        //</editor-fold>

        for (int i = 1; i < rows; i++) {
            final int threadRow = i;
            //Create thread to monitor install states, and disable USB Debugging when finished
            Thread states = new Thread() {
                @Override
                public void run() {
                    Boolean finishedThread = false;
                    while (!finishedThread) {
                        Boolean finishedInstalling = true;
                        for (int c = 1; c < columns; c++) {
                            if (getGridButton(threadRow - 1, c - 1).getBackground() != Color.GREEN) {
                                finishedInstalling = false;
                            }
                        }
                        if (finishedInstalling) {
                            finishedThread = true;
                            System.out.println("FINISHED INSTALLING ROW " + threadRow);
                            disconnect(threadRow);
                            for (int c = 1; c < columns; c++) {

                                JButton b = getGridButton(threadRow - 1, c - 1);
                                b.setBackground(Color.GRAY);

                                //prevent manual retry on a successfully provisioned phone
                                b.addActionListener((ActionEvent e) -> {
                                    return;
                                });
                            }

                        }
                    }
                }
            };
            states.start();
        }

        ActionListener actionListener = (ActionEvent actionEvent) -> {
            Boolean cont = false;
            for (JButton b : button_list) {
                if (b.getBackground() != Color.GRAY) {
                    cont = true;
                }
            }
            if (cont) { //If not everything is finished provisioning, increment the timekeeper
                if (!finished) {
                    time.setText("Time Passed: " + timePassed() + " sec");
                }
            } else {    //Present dialog if all devices have finished provisioning successfully

                int confirmed = JOptionPane.showConfirmDialog(null,
                        "All devices provisioned successfully! Would you like to provision additional devices?", "Finished",
                        JOptionPane.YES_NO_OPTION);

                /**
                 * If user selects yes, relaunch the program and kill this
                 * instance
                 */
                if (confirmed == JOptionPane.YES_OPTION) {
                    Command co = new Command();
                    StringBuilder builder = new StringBuilder();
                    String installPath = System.getProperty("user.dir");
                    String[] cmd = new String[]{"java", "-jar", installPath + "/provisioner.jar"};
                    GraphicReport.this.setVisible(false);
                    GraphicReport.this.dispose();
                    menu.setVisible(false);
                    menu.dispose();
                    reportFrame.setVisible(false);
                    reportFrame.dispose();

                    try {
                        builder = co.Command(cmd, false);
                    } catch (IOException ex) {
                        Logger.getLogger(GraphicReport.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.exit(0);

                } else {
                    /**
                     * If user selects no, just kill this instance
                     */
                    System.exit(0);
                }
            }
        };
        /**
         * Send a generic action event the above listener every 1 second
         */
        Timer timer = new Timer(1000, actionListener);
        timer.start();

        /**
         * Launch the installation commands for all devices
         */
        Thread provisionThread = new Thread() {
            @Override
            public void run() {
                provision(devices, files, packageNames, versionCodes, reportFrame);
            }
        };
        provisionThread.start();

    }

    /**
     * @return Time passed since provisioning began
     */
    private long timePassed() {
        return (System.currentTimeMillis() - timeStart) / 1000;
    }

    /**
     * Disable USB debugging on the device after successful provisioning *Does
     * not work on all devices
     *
     *
     * @param row The layout row of the device to disconnect
     */
    private void disconnect(int row) {
        try {
            String device = GraphicReport.this.getGridLabel(row, 0).getText();
            Command c = new Command();
            String[] cmd1 = new String[]{"adb", "-s", device, "shell", "pm", "clear", "android.providers.settings"};
            StringBuilder b1 = c.Command(cmd1, false);

            //TODO: Find a way to set the APN, if possible (Update: it's not possible)
            //At the very least, running 'am start -n com.android.settings/.ApnSettings' will open the activity where vpn can be set
            Command c2 = new Command();
            String[] cmd2 = new String[]{"adb", "-s", device, "shell", "settings", "put", "global", "adb_enabled", "0"};
            StringBuilder b2 = c.Command(cmd2, false);
        } catch (IOException ex) {
            Logger.getLogger(GraphicReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final List<JLabel> label_list = new ArrayList<>();

    private JLabel createGridLabel(String text, final int row, final int col) {

        final JLabel l = new JLabel(text);
        if (row == 0) {
            l.setFont(new Font("Plain", Font.PLAIN, 14));
            Font font = l.getFont();
            Map attributes = font.getAttributes();
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            l.setFont(font.deriveFont(attributes));

            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setVerticalAlignment(SwingConstants.BOTTOM);
            l.setMaximumSize(new Dimension(10, 10));
        } else {
            l.setFont(new Font("Plain", Font.BOLD, 14));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return l;
    }

    /**
     *
     * @param r Row
     * @param c column
     * @return The grid label for the box at the given coordinates
     */
    JLabel getGridLabel(int r, int c) {
        int index = 0;
        if (r == 0) {
            index = c;
        } else if (c == 0) {
            index = (N) + r;
        }
        return label_list.get(index);
    }
    private final List<JButton> button_list = new ArrayList<>();

    /**
     * Create a grid button at the given coordinates, and specify onClick behavior
     * @param row
     * @param col
     * @param packageNames List of package names, used for provisioning data logging
     * @param versionCodes List of version codes, used for provisioning data logging
     * @param reportFrame
     * @return The button just created
     */
    private JButton createGridButton(final int row, final int col, final String[] packageNames, final String[] versionCodes, final Report reportFrame) {

        final JButton b = new JButton("...");
        b.setToolTipText("Initializing...");
        b.setPreferredSize(new Dimension(60, 40));
        b.addActionListener((ActionEvent e) -> {
            JButton gb = GraphicReport.this.getGridButton((row - 1), (col - 1));
            if (gb.getBackground() == Color.RED) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        Command c = new Command();
//                        try {
//                            c.manualRetry(row, col, GraphicReport.this, packageNames[col - 1], versionCodes[col - 1], reportFrame);
//                        } catch (IOException ex) {
//                            Logger.getLogger(GraphicReport.class.getName()).log(Level.SEVERE, null, ex);
//                        }
                        //DISABLED MANUAL RETRYS, FOR NOW
                        //TODO:
                    }
                };
                thread.start();
            }
        });
        return b;
    }

    /**
     * @param r Row
     * @param c Column
     * @return The button at those coordinates
     */
    JButton getGridButton(int r, int c) {
        int index = r * N + c;
        return button_list.get(index);
    }

    /**
     * Handles the provisioning of all apps onto all devices
     * @param devices
     * @param files
     * @param packageNames
     * @param versionCodes
     * @param reportFrame
     */
    private void provision(String[] devices, File[] files, String[] packageNames, String[] versionCodes, Report reportFrame) {

        System.out.println("NUMBER OF BASE THREADS: " + Thread.activeCount());
        Thread deviceThread = new Thread() {
            @Override
            public void run() {

                for (int dc = 0; dc < devices.length; dc++) {
                    //limits the number of devices installed to at a single time
                    //Prevents ADB overload, so devices don't sporadically disconnect
                    if (devices.length > 1) {
                        while (Thread.activeCount() > (files.length * SIMULATANEOUS_DEVICES + devices.length)) {
                            //stall
                            System.out.println("Stalling...");
                        }
                    }
                    final int dev = dc;
                    List<String> deviceFiles = new ArrayList<>();

                    //BUG: Failed install threads never exit?!
                    //Separate thread launched for each individual installation, to boost performance
                    for (int i = 0; i < files.length; i++) {

                        final int num = i;
                        Thread thread2 = new Thread() {
                            @Override
                            public void run() {
                                Command c = new Command();//Should pass this from earlier
                                c.installApp(files[num].getName(), packageNames[num], versionCodes[num], devices[dev], installPath,
                                        GraphicReport.this, dev, num, reportFrame);
                            }
                        };

                        thread2.start();
                        deviceFiles.add(files[num].getName());
                    }   //end of each file loop
                }   //end of each device loop
            }
        };
        deviceThread.start();
    }
}
