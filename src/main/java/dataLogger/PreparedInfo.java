package dataLogger;

import static dataLogger.CreateFolder.CreateFolder;
import static dataLogger.GetDeviceInfo.GetDeviceInfo;
import provisioner.MainMenu;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONObject;

public class PreparedInfo {

    public PreparedInfo() {
    }

    /**
     * Write device info data to file
     * @param dataLog An array with all the data for the device
     * @param devicePort The device's USB port identifier
     * @throws IOException 
     */
    public void execute(String[] dataLog, String devicePort) throws IOException {

        System.out.println("Trying to execute...");

        //check for errors -- only write to database if data is valid
        Boolean cont = true;
        for (String d : dataLog) {
            if (cont && (d.length() > 5)) {
                String first = d.substring(0, 6);
                if (first.equals("error:") || first.equals("[adb, ")) {
                    cont = false;
                }
            }
        }

        //If data was error free, build the JSON object and write it to local file
        if (cont) {

            JSONObject obj = new JSONObject();
            obj.put("android_id", dataLog[0]);
            obj.put("first_action", dataLog[1]);
            obj.put("last_action", dataLog[2]);
            obj.put("gsm_version_baseband", dataLog[3]);
            obj.put("gsm_version_rilimpl", dataLog[4]);
            obj.put("ro_boot_serialno", dataLog[5]);
            obj.put("ro_build_version_sdk", dataLog[6]);
            obj.put("ro_product_manufacturer", dataLog[7]);
            obj.put("ro_product_model", dataLog[8]);
            obj.put("ro_product_name", dataLog[9]);
            obj.put("ro_revision", dataLog[10]);
            obj.put("ro_runtime_firstboot", dataLog[11]);
            obj.put("ro_secure", dataLog[12]);
            obj.put("ro_serialno", dataLog[13]);

            String folder = System.getProperty("user.dir") + "/.data/";
            CreateFolder(folder);
            folder = folder + ".device" + "/";
            CreateFolder(folder);
            try (FileWriter file = new FileWriter(folder + System.currentTimeMillis() + Math.random() * 10 + ".json")) {
                file.write(obj.toJSONString());
            }
            MainMenu.uploader.putRequest("device");

        } else {
            //If data had errors, try again to get the device's info
            GetDeviceInfo(devicePort);
        }

    }

   
}