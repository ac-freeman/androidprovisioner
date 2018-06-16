package provisioner;

import dataLogger.PreparedProvisioning;
import static provisioner.MainMenu.getHost;
import static provisioner.MainMenu.selectedPreset;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

/**
 *
 * @author andrew
 */
public class Command {

    public Command() {
    }

    /**
     * Runs the given command in an invisible terminal
     *
     * @param cmd The command to run
     * @param warningCheck Whether or not to check this command's output for the
     * extraneous and meaningless lib warning
     * @return The StringBuilder response from running the command
     * @throws IOException
     */
    public StringBuilder Command(String[] cmd, boolean warningCheck) throws IOException {

        Process pr = new ProcessBuilder(cmd).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        StringBuilder builder = new StringBuilder();
        if (warningCheck) {
            String line = null;
            builder.append(Arrays.toString(cmd)).append("\n");
            while ((line = reader.readLine()) != null) {
                String linecheck = line;
                if (linecheck.length() > 6) {
                    if (!linecheck.substring(0, 7).equals("WARNING")) {
                        builder.append(line);
                        builder.append(System.getProperty("line.separator"));
                    }
                } else {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
            }
        } else {
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder;
    }

    /**
     * If the user manually clicks a box to re-initiate the install
     *
     * @param row The row of the box in layout
     * @param col The column of the box in layout
     * @param gr The graphicReport instance to change
     * @param packageName
     * @param versionCode
     * @param reportFrame
     * @throws IOException
     */
    public void manualRetry(int row, int col, GraphicReport gr, String packageName, String versionCode, Report reportFrame) throws IOException {
        String devicePort = gr.getGridLabel(row, 0).getText();
        String appName = gr.getGridLabel(0, col).getText();
        String installPath = System.getProperty("user.dir") + "/.apps_installing/";

        int dev = row - 1;
        int num = col - 1;

        String[] cmd = new String[]{"adb", "-s", devicePort, "shell", "settings", "get", "secure", "android_id"};
        StringBuilder builder = Command(cmd, true);
        String deviceIdCom = builder.toString();
        String idArray[] = deviceIdCom.split("\\r?\\n");
        String deviceId = idArray[idArray.length - 1];

        installApp(appName, packageName, versionCode, devicePort, installPath, gr, dev, num, reportFrame);

    }

    public Report rf;

    /**
     * Run a command to install an app
     *
     * @param appName
     * @param packageName
     * @param versionCode
     * @param devicePort
     * @param installPath
     * @param grFrame
     * @param dev
     * @param num
     * @param reportFrame
     */
    public void installApp(String appName, String packageName, String versionCode, String devicePort, String installPath,
            GraphicReport grFrame, int dev, int num, Report reportFrame) {
        this.rf = reportFrame;

        List<String> provData = new ArrayList<>();
        try {
            System.out.println("Install thread Running");

            String[] cmd = new String[]{"adb", "-s", devicePort, "shell", "settings", "get", "secure", "android_id"};
            Command c = new Command();
            StringBuilder builder = c.Command(cmd, true);
            String deviceIdCom = builder.toString();
            String idArray[] = deviceIdCom.split("\\r?\\n");
            String deviceId = idArray[idArray.length - 1];

            provData.add(deviceId);

            Date date = new Date();
            LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.of("UTC+0")).toLocalDateTime();
            String currentTime = localDateTime.toString();
            provData.add(currentTime);//begin time
            provData.add("1");
            provData.add(appName);
            provData.add(packageName);
            provData.add(versionCode);
            provData.add(selectedPreset);  //null if no preset selected
            provData.add(devicePort);  //usb port id
            provData.add(getHost());  //provisioning computer name

            List<String> cmd2list = new ArrayList<>();

            cmd2list.add("adb");
            cmd2list.add("-s");
            cmd2list.add(devicePort);
            cmd2list.add("install");
            cmd2list.add("-r");
            cmd2list.add(installPath + appName);
            String[] cmd2 = cmd2list.toArray(new String[]{});

            provData.add(Arrays.toString(cmd2));  //command

            JButton b = grFrame.getGridButton(dev, num);
            b.setBackground(Color.YELLOW);
            b.setText("...");
            b.setToolTipText("Installing...");

            //Start timeout monitor
//            Thread timeoutThread = new Thread() {
//                @Override
//                public void run() {
//                    long timeStart = System.currentTimeMillis();
//                    Boolean timer = true;
//                    int count = 0;
//                    while (timer) {
//                        if (b.getBackground() == Color.YELLOW) {
//                            count = (int) ((System.currentTimeMillis() - timeStart) / 1000);
////                            if (count%10==0) System.out.println("COUNT: " + count);
//                            if (count >= 90) {
//                                try {
//                                    timer = false;
//                                    String[] cmd = new String[]{"adb", "kill-server"};
//                                    System.out.println("KILLING SERVER IN TIMEOUT THREAD");
//                                    StringBuilder builder = Command(cmd, false);
//
//                                    cmd = new String[]{"adb", "start-server"};
//                                    System.out.println("STARTING SERVER IN TIMEOUT THREAD");
//                                    builder = Command(cmd, false);
//
//                                } catch (IOException ex) {
//                                    Logger.getLogger(Command.class.getName()).log(Level.SEVERE, null, ex);
//                                }
//                            }
//                        } else {
//                            System.out.println("Background color: " + b.getBackground() + packageName);
//                            timer = false;
//                        }
//                    }
//                }
//            };
//            timeoutThread.start();

            StringBuilder b2 = Command(cmd2, false);

            String result2 = b2.toString(); //does not write until the whole command is finished?

            provData.add(result2);    //raw response

            String lines[] = result2.split("\\r?\\n");
            boolean failed = false;
            boolean retry = false;
            String status = lines[lines.length - 1];
            String statusWords[] = status.split("\\s+");

            /**
             * Change the GUI to notify to user of installation's
             * success/failure
             */
            if (status.equals("Success")) {
                b.setBackground(Color.GREEN);
                b.setText("✔");
                b.setToolTipText("Success");
                provData.add(status);
            } else {
                failed = true;
                b.setBackground(Color.RED);
                System.out.println("ERROR: " + lines[lines.length - 1]);
                if (lines[lines.length - 1].equals("- waiting for device -")) { //not an output. doesn't work.
                    b.setText("✖");
                    b.setToolTipText("Device disconnected");
                    provData.add(status);
                } else if (status.equals("Failure [INSTALL_FAILED_OLDER_SDK]")) {
                    b.setText("✖");
                    b.setToolTipText("Incompatible SDK");
                    provData.add(status);
                } else if (status.equals("Failure [INSTALL_FAILED_INVALID_APK]")) {
                    b.setText("✖");
                    b.setToolTipText("Invalid apk");
                    provData.add(status);
                } else if (status.equals("Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]")) {
                    b.setText("✖");
                    b.setToolTipText("Insufficient storage");
                    provData.add(status);
                } else if (status.equals("Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]")) {
                    b.setText("✖");
                    b.setToolTipText("Update incompatible");
                    provData.add(status);
                } else if (status.equals("Failure [INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES]")) {
                    b.setText("✖");
                    b.setToolTipText("Inconsistent certificates");
                    provData.add(status);
                } else if (status.equals("Failure {INSTALL_FAILED_DEVICE_DISCONNECTED]")) {
                    b.setText("✖");
                    b.setToolTipText("Device disconnected");
                    retry = true;
                    provData.add("Failure: Device disconnected");
                } else if (statusWords[0].equals("*")) {
                    b.setText("✖");
                    b.setToolTipText("ADB restarted");
                    retry = true;
                    provData.add("Failure: ADB restarted");
                } else {
                    b.setText("✖");
                    b.setToolTipText("Other failure -- check logs");
                    retry = true;
                    System.out.println("Other failure code: " + status);
                    provData.add("Failure: Other");
                }
            }

            date = new Date();
            localDateTime = date.toInstant().atZone(ZoneId.of("UTC+0")).toLocalDateTime();
            currentTime = localDateTime.toString();
            provData.add(currentTime);  //end time -- when the provisioning action completed
            String[] dataLog = provData.toArray(new String[]{});
            Thread provThread = new Thread() {
                @Override
                public void run() {
                    PreparedProvisioning database = new PreparedProvisioning();
                    try {
                        database.execute(dataLog);
                    } catch (IOException ex) {
                        Logger.getLogger(Command.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            provThread.start();

            if (failed && b.getBackground() != Color.GREEN && b.getBackground() != Color.YELLOW) {
                reportFrame.append(result2 + "FAILED\n\n");
                retry = true;   //Temporary patch!! remove later
                if (retry) {
                    cmd = new String[]{"adb", "-s", devicePort, "uninstall", packageName};
                    System.out.println("Uninstalling " + packageName + " from " + devicePort + " IN END CHECK");
                    builder = Command(cmd, false);
                    try {
                        Thread.sleep(5000);     //1000 milliseconds is one second
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    installApp(appName, packageName, versionCode, devicePort, installPath,
                            grFrame, dev, num, reportFrame);
                }
            } else {
                reportFrame.append(result2 + "\n");
            }

        } catch (IOException ex) {
            Logger.getLogger(Command.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
