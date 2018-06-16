package dataLogger;

import java.io.File;

/**
 *
 * @author andrew
 */
public class CreateFolder {
        
/**
     * Create a folder at the path specified
     *
     * @param folder The folder to create
     * @return File for the newly-created folder
     */
    public static File CreateFolder(String folder) {
        File theDir = new File(folder);

        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("Creating directory");
            boolean result = false;
            try {
                theDir.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
            }
            if (result) {
                System.out.println("DIR created");
            }
        }
        return theDir;
    }
}
