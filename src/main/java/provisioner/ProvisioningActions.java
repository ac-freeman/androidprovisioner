package provisioner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static provisioner.MainMenu.deleteFolder;

/**
 *
 * @author andrew
 */
public class ProvisioningActions {

    /**
     * Handles getting the info on each device, begins the installation
     * commands, and instantiates the graphic report
     *
     * @param model Tells which apps are selected for install
     * @param listOfFiles List of files in .all_apps/
     * @param menu
     */
    public static void ProvisioningActions(provisioner.CheckModel model, File[] listOfFiles, MainMenu menu) {
        int successCount;
        int tryCount;
        List<String> packageNames = new ArrayList<>();   //The package name for all apps being installed
        List<String> versionCodes = new ArrayList<>();   //The version code for all apps being installed

        try {
            Report reportFrame1 = new Report();
            successCount = 0;
            tryCount = 0;

            /**
             * Copy every app to be installed into a new folder,
             * .apps_installing/ This makes installation syntax easier to
             * read/understand
             */
            String path1 = System.getProperty("user.dir") + "/.apps_installing/";
            File folder1 = new File(path1);
            deleteFolder(folder1);  //from MainMenu.java
            folder1.mkdirs();
            for (Integer num : model.checked) {
                try {
                    Files.copy(listOfFiles[num].toPath(), (new File(path1 + listOfFiles[num].getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(MainMenu.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }

            // TODO: When user authentication is implemented, only show the report frame for admin users
            /**
             * Get a list of every app that's been selected for installation
             */
            // reportFrame1.setVisible(true);
            String installPath = System.getProperty("user.dir") + "/.apps_installing/";
            folder1 = new File(installPath);
            File[] listOfFilesInstalling = folder1.listFiles();

            Command c = new Command();

            for (File installing : listOfFilesInstalling) {

                String[] tCmd = new String[]{"aapt", "dump", "badging", path1 + installing.getName()};
                StringBuilder builder = c.Command(tCmd, true);
                String out = builder.toString();
                String appDump[] = out.split("\\r?\\n");
                String packageName = null;
                String versionCode = null;
                if (appDump.length > 1) {
                    String firstLine[] = appDump[1].split(" ");
                    String packBreak[] = firstLine[1].split("'");
                    packageName = packBreak[1];
                    String verBreak[] = firstLine[2].split("'");
                    versionCode = verBreak[1];
                    System.out.println("packageName: " + packageName);
                    System.out.println("versionCode: " + versionCode);
                } else {
                    packageName = "invalid.package";
                    versionCode = "-1"; //incase the apk is invalid/corrupted
                }
                packageNames.add(packageName);
                versionCodes.add(versionCode);
            }

            /**
             * Get each device's USB port identifier from ADB
             */
            String[] cmd = new String[]{"adb", "devices", "-l"};
            StringBuilder builder = c.Command(cmd, false);
            String result = builder.toString();
            final int deviceCount = MainMenu.countDevices();
            String[] cmdLines = result.split("\\r?\\n");
            List<String> lod = new ArrayList<>();
            for (String cmdLine : cmdLines) {
                String[] lArgs = cmdLine.split("\\s+");
                if (cmdLine != null && lArgs[1].equals("device")) {
                    lod.add(lArgs[2]);
                }
            }
            String[] listOfDevices = lod.toArray(new String[]{});

            /**
             * Create the GraphicReport, launching the installation the apps to
             * all devices
             */
            Thread grThread = new Thread() {
                public void run() {
                    GraphicReport grFrame = new GraphicReport(listOfDevices, listOfFilesInstalling, packageNames.toArray(new String[]{}), versionCodes.toArray(new String[]{}), reportFrame1, menu);
                    grFrame.setVisible(true);
                }
            };
            grThread.start();

            /**
             * Begin gathering device identifying information in the background
             * Sent to S3
             */
            for (String devPort : listOfDevices) {
                Thread infoThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            dataLogger.GetDeviceInfo.GetDeviceInfo(devPort);
                        } catch (IOException ex) {
                            Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                };
                infoThread.start();
            }

        } catch (IOException ex) {
            Logger.getLogger(MainMenu.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
}
