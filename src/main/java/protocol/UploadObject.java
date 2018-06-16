package protocol;

/**
 *
 * @author andrew
 */
import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class UploadObject {

    /**
     * Hardcoded S3 credentials
     */
    private static String awsAccessKey = "ACCESSKEY";
    private static String awsSecretKey = "SECRETKEY";
    private static String bucketName = "BUCKETNAME";
    private static AmazonS3 s3client;

    /**
     * Create an S3 client connection with the specified credentials
     *
     * @throws IOException
     */
    public UploadObject() throws IOException {

        s3client = new AmazonS3Client(new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return awsAccessKey;
            }

            @Override
            public String getAWSSecretKey() {
                return awsSecretKey;
            }
        });

    }

    /**
     * Start a put request for the specified folders
     */
    public static void upload() {
        putRequest("device");
        putRequest("provisioning");
    }

    /**
     * Sends all local data files in the specified directory to S3
     * In S3, places files in appropriate folders based on time sent and data type (device or provisioning)
     * Example file structure:
     * 2016
     * ├── 07
     * ├── 08
     * │   ├── 01
     * │   ├── 02
     * │   │   ├── device
     * │   │   ├── provisioning
     * │   │   │   ├── 15234451567.87875415787.JSON
     * │   │   │   ├── 54578564515.45678787457.JSON
     * │   │   │   ├── 78789751222.87963545754.JSON
     * @param folderName The directory to upload to S3
     */
    public static void putRequest(String folderName) {
        Date date = new Date();
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.of("UTC+0")).toLocalDateTime();
        int year = localDateTime.getYear();
        int month = localDateTime.getMonthValue();
        int day = localDateTime.getDayOfMonth();
        int hour = localDateTime.getHour();

        //Move logs to S3
        String folder = System.getProperty("user.dir") + "/.data/" + "." + folderName + "/";
        System.out.println(folder);
        File theDir = new File(folder);
        if (theDir.exists()) {
            System.out.println("Uploading " + folderName + " files to S3\n");
            for (File f : theDir.listFiles()) {
                Boolean del = true;
                try {

                    s3client.putObject(new PutObjectRequest(
                            bucketName, year + "/" + String.format("%02d", month) + "/" + String.format("%02d", day) + "/" + String.format("%02d", hour) + "/" + folderName + "/" + f.getName(), f));
                } //<editor-fold defaultstate="collapsed" desc="Catch exceptions">
                catch (AmazonServiceException ase) {
                    del = false;
                    System.out.println("Caught an AmazonServiceException, which "
                            + "means your request made it "
                            + "to Amazon S3, but was rejected with an error response"
                            + " for some reason.");
                    System.out.println("Error Message:    " + ase.getMessage());
                    System.out.println("HTTP Status Code: " + ase.getStatusCode());
                    System.out.println("AWS Error Code:   " + ase.getErrorCode());
                    System.out.println("Error Type:       " + ase.getErrorType());
                    System.out.println("Request ID:       " + ase.getRequestId());
                } catch (AmazonClientException ace) {
                    del = false;
                    System.out.println("Caught an AmazonClientException, which "
                            + "means the client encountered "
                            + "an internal error while trying to "
                            + "communicate with S3, "
                            + "such as not being able to access the network.");
                    System.out.println("Error Message: " + ace.getMessage());
                }
                // </editor-fold>
                /**
                 * If the file does not upload to S3 successfully, an exception will be caught
                 * If there is no exception caught, we know the upload succeeded, so we can
                 * delete the local copy of that file
                 */
                if (del) {
                    f.delete();
                }
            }
        }

    }
}
