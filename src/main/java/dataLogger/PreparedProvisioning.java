package dataLogger;

import static dataLogger.CreateFolder.CreateFolder;
import provisioner.MainMenu;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONObject;

public class PreparedProvisioning {

    public PreparedProvisioning() {

    }

    /**
     * Write provisioning data to file
     *
     * @param dataLog
     * @throws IOException
     */
    public void execute(String[] dataLog) throws IOException {

        //check for errors -- only write to database if data is valid
        Boolean cont = true;
        for (int i = 0; i < dataLog.length; i++) {
            if (dataLog[i] != null) {
                if (cont && (dataLog[i].length() > 5)) {
                    String first = dataLog[i].substring(0, 6);
                    if (first.equals("error:") || (first.equals("[adb, ") && i != 9 && i != 10)) {
                        cont = false;
                    }

                }
            }
        }

        if (cont) {
            JSONObject obj = new JSONObject();
            obj.put("android_id", dataLog[0]);
            obj.put("begin_datetime", dataLog[1]);
            obj.put("action_type", dataLog[2]);
            obj.put("apk_filename", dataLog[3]);
            obj.put("apk_packagename", dataLog[4]);
            obj.put("apk_versioncode", dataLog[5]);
            obj.put("preset_name", dataLog[6]);
            obj.put("usb_port_id", dataLog[7]);
            obj.put("provisioning_comp_name", dataLog[8]);
            obj.put("command", dataLog[9]);
            obj.put("response", dataLog[10]);
            obj.put("status", dataLog[11]);
            obj.put("end_datetime", dataLog[12]);

            String folder = System.getProperty("user.dir") + "/.data/";
            CreateFolder(folder);
            folder = folder + ".provisioning" + "/";
            CreateFolder(folder);
            try (FileWriter file = new FileWriter(folder + System.currentTimeMillis() + Math.random() * 10 + ".json")) {
                file.write(obj.toJSONString());
            }
            MainMenu.uploader.putRequest("provisioning");
        }
    }
}
