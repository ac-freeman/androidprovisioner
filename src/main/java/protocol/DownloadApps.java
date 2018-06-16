package protocol;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import provisioner.LoadingScreen;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

/**
 * Handles the downloading of all .apk files and the presets file from S#
 *
 * @author andrew
 */
public class DownloadApps extends AmazonS3Client {

    final String bucket;
    private static String awsAccessKey = "ACCESSKEY";
    private static String awsSecretKey = "SECRETKEY";
    private static String bucketName = "BUCKETNAME";

    private LoadingScreen l;
    private boolean monitoring;
    public boolean finished = false;
    private File dir;
    private File f;
    private ArrayList<GetObjectRequest> requests = new ArrayList<>();

    public DownloadApps(LoadingScreen l) {
        super(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        bucket = bucketName;
        this.l = l;

        TransferManager tx = new TransferManager(this.awsCredentialsProvider);

        String path = System.getProperty("user.dir") + "/.all_apps/";
        File dir = new File(path);
        String[] localList = getLocal(dir);
        try {
            downloadChanged(tx, dir, localList);
        } catch (IOException ex) {
            Logger.getLogger(DownloadApps.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get local files
     *
     * @param dir Folder to look in
     * @return sorted of all filenames in specified directory
     */
    String[] getLocal(File dir) {
        String[] localList;
        if (dir.exists()) {
            File[] fileList = dir.listFiles();
            localList = new String[fileList.length];
            for (int i = 0; i < fileList.length; i++) {
                localList[i] = fileList[i].getName();
            }
            Arrays.sort(localList);
        } else {
            localList = new String[]{};
        }
        return localList;
    }

    String[] listRemote() {
        try {
            final ObjectListing l = listObjects(bucket);
            final List<S3ObjectSummary> L = l.getObjectSummaries();
            final int n = L.size();
            final String[] names = new String[n];
            for (int i = 0; i < n; ++i) {
                final S3ObjectSummary k = L.get(i);
                names[i] = k.getKey();  //the file name in S3
            }
            return names;
        } catch (Exception e) {
            System.out.println("Cannot list " + bucket + "/" + " on S3 because " + e);
        }
        return new String[]{};
    }

    /**
     * Download necessary files from S3 bucket
     *
     * @param tx TransferManager for connection to bucket
     * @param f Local file directory for .apk's and presets.txt
     * @param localList List of local file names in the directory f
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void downloadChanged(TransferManager tx, File f, String[] localList) throws FileNotFoundException, IOException {
        this.f = f;
        final File dir = f;
        final LoadingScreen loader = l;

        final ObjectListing l = listObjects(bucket);
        final List<S3ObjectSummary> L = l.getObjectSummaries();

        ArrayList<Thread> downloadThreads = new ArrayList<>();
        ArrayList<GetObjectRequest> requests = new ArrayList<>();
        String[] appList = listRemote();
        Date[] appS3ModDates = listModifiedS3(L);
        Date[] appLocalModDates = listModifiedLocal(dir, localList);
        String[] remoteMD5 = listMD5S3(L);
        long[] remoteSizes = listRemoteSizes(L);

        ArrayList<String> isLocal = new ArrayList<>(Arrays.asList(localList));
        ArrayList<String> isRemote = new ArrayList<>(Arrays.asList(appList));

        System.out.println("REMOTE LENGTH: " + appList.length);
        System.out.println("LOCAL LENGTH: " + localList.length);
        //compare modified dates
        for (int i = 0; i < appList.length; i++) {
            for (int j = 0; j < localList.length; j++) {
                if (appList[i].equals(localList[j])) {
                    Calendar cal1 = Calendar.getInstance();
                    Calendar cal2 = Calendar.getInstance();
                    cal1.setTime(appLocalModDates[j]);
                    cal2.setTime(appS3ModDates[i]);
                    boolean updated = cal1.get(Calendar.YEAR) <= cal2.get(Calendar.YEAR)
                            && cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
                    if (updated) {
                        final String appname = appList[i];
                        //launch download in new thread
                        Thread updatethread = new Thread() {
                            @Override
                            public void run() {
                                GetObjectRequest request = new GetObjectRequest(bucket, appname);
                                File file = new File(dir + "/" + appname);
                                Download d = tx.download(request, file);
                                requests.add(request);
                                System.out.println(requests.size());
                                String updateText = "Downloading an update for " + appname + "... ";
                                JLabel label = loader.createLabel(updateText);
                                while (!d.isDone()) {
                                    loader.updateText(label, updateText, String.format("%.0f", d.getProgress().getPercentTransferred()) + "%");
                                }
                                requests.remove(request);
                            }
                        };
                        downloadThreads.add(updatethread);
                        updatethread.start();
                    } else {

                        File file = new File(dir + "/" + localList[j]);
                        FileInputStream fis = new FileInputStream(file);
                        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                        fis.close();
                        System.out.println("Local MD5 checksum: " + localList[j] + " " + md5);
                        System.out.println("Remote MD5 checksum: " + appList[i] + " " + remoteMD5[i]);
                        final String appname = appList[i];

                        if (!md5.equals(remoteMD5[i])) {
                            //launch download in new thread
                            Thread md5thread = new Thread() {
                                @Override
                                public void run() {
                                    GetObjectRequest request = new GetObjectRequest(bucket, appname);
                                    File file = new File(dir + "/" + appname);
                                    Download d = tx.download(request, file);
                                    requests.add(request);
                                    System.out.println(requests.size());
                                    System.out.println("Different MD5 detected. Downloading " + appname + " from S3");
                                    String updateText = "Downloading an update for " + appname + "... ";
                                    JLabel label = loader.createLabel(updateText);
                                    while (!d.isDone()) {
                                        loader.updateText(label, updateText, String.format("%.0f", d.getProgress().getPercentTransferred()) + "%");
                                    }
                                    requests.remove(request);
                                }
                            };
                            downloadThreads.add(md5thread);
                            md5thread.start();
                        } else {
                            //compare sizes
                            double bytes = file.length();
                            long localSize = (long) bytes;
                            System.out.println("Local file size: " + localList[j] + " " + localSize);
                            System.out.println("Remote file size: " + appList[i] + " " + remoteSizes[i]);

                            if (localSize != remoteSizes[i]) {
                                //launch download in new thread
                                Thread sizeThread = new Thread() {
                                    @Override
                                    public void run() {
                                        GetObjectRequest request = new GetObjectRequest(bucket, appname);
                                        File file = new File(dir + "/" + appname);
                                        Download d = tx.download(request, file);
                                        requests.add(request);
                                        System.out.println(requests.size());
                                        System.out.println("Different file size detected. Downloading " + appname + " from S3");
                                        String updateText = "Downloading an update for " + appname + "... ";
                                        JLabel label = loader.createLabel(updateText);
                                        while (!d.isDone()) {
                                            loader.updateText(label, updateText, String.format("%.0f", d.getProgress().getPercentTransferred()) + "%");
                                        }
                                        requests.remove(request);
                                    }
                                };
                                downloadThreads.add(sizeThread);
                                sizeThread.start();
                            }
                        }
                    }

                    isLocal.remove(isLocal.indexOf(localList[j]));
                    isRemote.remove(isRemote.indexOf(appList[i]));
                }
            }
        }

        //Delete all local files that aren't in S3
        for (String loc : isLocal) {
            File file = new File(dir + "/" + loc);
            try {
                Files.delete(file.toPath());
            } catch (IOException ex) {
                Logger.getLogger(DownloadApps.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //Download all S3 files that don't have a local copy
        for (String rem : isRemote) {
            //launch download in new thread
            Thread remthread = new Thread() {
                @Override
                public void run() {
                    GetObjectRequest request = new GetObjectRequest(bucket, rem);
                    File file = new File(dir + "/" + rem);
                    Download d = tx.download(request, file);
                    requests.add(request);
                    System.out.println(requests.size());
                    String updateText = "Downloading " + rem + " for the first time... ";
                    JLabel label = loader.createLabel(updateText);
                    while (!d.isDone()) {
                        loader.updateText(label, updateText, String.format("%.0f", d.getProgress().getPercentTransferred()) + "%");
                    }
                    requests.remove(request);
                }
            };
            downloadThreads.add(remthread);
            remthread.start();
        }

        Boolean done = false;

        while (!done) {
            done = true;
            for (Thread t : downloadThreads) {
                if (t.isAlive()) {
                    done = false;
                }
            }
        }
        finished = true;

    }

    Date[] listModifiedLocal(File dir, String[] localList) {

        int n = localList.length;
        Date[] dates = new Date[n];
        for (int i = 0; i < n; i++) {
            File file = new File(dir + "/" + localList[i]);
            dates[i] = new Date(TimeUnit.SECONDS.toMillis(file.lastModified()));
        }

        return dates;

    }

    Date[] listModifiedS3(List<S3ObjectSummary> L
    ) {
        try {
            System.out.println("In listModifiedS3");
            final int n = L.size();
            final Date[] dates = new Date[n];
            for (int i = 0; i < n; ++i) {
                final S3ObjectSummary k = L.get(i);
                dates[i] = k.getLastModified();  //the last modified date in S3
                System.out.println("Date: " + dates[i]);
            }
            return dates;
        } catch (Exception e) {
            System.out.println("Cannot list " + bucket + "/" + " on S3 because " + e);
        }
        return new Date[]{};
    }

    String[] listMD5S3(List<S3ObjectSummary> L
    ) {
        try {
            System.out.println("In listMD5S3");
            final int n = L.size();
            final String[] md5s = new String[n];
            for (int i = 0; i < n; ++i) {
                final S3ObjectSummary k = L.get(i);
                md5s[i] = k.getETag();  //the MD5 for file in S3
                System.out.println("Date: " + md5s[i]);
            }
            return md5s;
        } catch (Exception e) {
            System.out.println("Cannot list " + bucket + "/" + " on S3 because " + e);
        }
        return new String[]{};
    }

    long[] listRemoteSizes(List<S3ObjectSummary> L) {
        try {
            System.out.println("In listRemoteSizes");

            final int n = L.size();
            final long[] sizes = new long[n];
            for (int i = 0; i < n; ++i) {
                final S3ObjectSummary k = L.get(i);
                sizes[i] = k.getSize();  //size for file in S3
                System.out.println("Date: " + sizes[i]);
            }
            return sizes;
        } catch (Exception e) {
            System.out.println("Cannot list " + bucket + "/" + " on S3 because " + e);
        }
        return new long[]{};
    }
}
