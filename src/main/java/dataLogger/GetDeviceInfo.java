package dataLogger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import provisioner.Command;
import provisioner.MainMenu;

/**
 *
 * @author andrew
 */
public class GetDeviceInfo {
    /**
     * Get all necessary device information, used for reporting to S3
     *
     * @param devicePort The USB port identifier for the device
     * @return device's android_id
     * @throws IOException
     */
    public static String GetDeviceInfo(String devicePort) throws IOException {
        //TODO: redo the device gathering info and try again to write it, if the device disconnects

        String[] cmd = new String[]{"adb", "-s", devicePort, "shell", "settings", "get", "secure", "android_id"};
        Command c = new Command();
        StringBuilder builder = c.Command(cmd, true);
        String deviceIdCom = builder.toString();
        String idArray[] = deviceIdCom.split("\\r?\\n");
        String deviceId = idArray[idArray.length - 1];

        List<String> deviceData = new ArrayList<>();
        deviceData.add(deviceId);

        Date date = new Date();
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.of("UTC+0")).toLocalDateTime();
        String currentTime = localDateTime.toString();
        deviceData.add(currentTime);//first action time
        deviceData.add(currentTime);//default last action time

        getProps(deviceData, devicePort);  //TODO: Why are these two separate methods?

        String[] dataLog = deviceData.toArray(new String[]{});

        PreparedInfo database = new PreparedInfo();
        try {
            database.execute(dataLog, devicePort);
        } catch (IOException ex) {
            Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
        }

        return deviceId;
    }

    /**
     * Get and log all relevant info for the device being provisioned
     *
     * @param listOfDevices All USB port identifiers
     * @param dc The index of listOfDevices to target
     * @return android_id for the targeted device
     * @throws IOException
     */
    public static String getDeviceInfo(String[] listOfDevices, int dc) throws IOException {
        //TODO: redo the device gathering info and try again to write it, if the device disconnects
        String devicePort = listOfDevices[dc];
        String[] cmd = new String[]{"adb", "-s", devicePort, "shell", "settings", "get", "secure", "android_id"};
        Command c = new Command();
        StringBuilder builder = c.Command(cmd, true);
        String deviceIdCom = builder.toString();
        String idArray[] = deviceIdCom.split("\\r?\\n");
        String deviceId = idArray[idArray.length - 1];

        List<String> deviceData = new ArrayList<>();
        deviceData.add(deviceId);

        Date date = new Date();
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.of("UTC+0")).toLocalDateTime();
        String currentTime = localDateTime.toString();
        deviceData.add(currentTime);//first action time
        deviceData.add(currentTime);//default last action time

        getProps(deviceData, devicePort);

        String[] dataLog = deviceData.toArray(new String[]{});
        PreparedInfo database = new PreparedInfo();
        database.execute(dataLog, devicePort);

        return deviceId;
    }

    /**
     * Get additional device properties for the device_info files
     *
     * @param deviceData The list of existing data for that device
     * @param device The USB port identifier for that device
     * @throws IOException
     */
    public static void getProps(List<String> deviceData, String device) throws IOException {
        String[] properties = new String[]{"gsm.version.baseband", "gsm.version.ril-impl", "ro.boot.serialno", "ro.build.version.sdk",
            "ro.product.manufacturer", "ro.product.model", "ro.product.name", "ro.revision", "ro.runtime.firstboot", "ro.secure", "ro.serialno"};
        Command c = new Command();
        for (String p : properties) {

            String[] cmd = new String[]{"adb", "-s", device, "shell", "getprop", p};
            StringBuilder builder = c.Command(cmd, true);
            String a[] = builder.toString().split("\\r?\\n");
            String prop = a[a.length - 1];
            deviceData.add(prop);
        }
    }
}
