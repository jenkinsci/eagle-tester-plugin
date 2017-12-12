package com.mobileenerlytics.eagle.tester;

import com.mobileenerlytics.eagle.tester.common.util.Log;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public abstract class LocalOperation {
    protected String adb;
    private static final String TEST_APP_APK_NAME = "TesterApp.apk";
    private static final String TESTER_APK_URL = "https://tester.mobileenerlytics.com/eagle/" + TEST_APP_APK_NAME;
    private static final String TEST_APP_PKG_NAME = "com.mobileenerlytics.eagle.tester.app";
    private static final String PHONE_TRACES_FOLDER = "/data/data/" + TEST_APP_PKG_NAME + "/cache/traces";

    public LocalOperation(String adb) {
        this.adb = adb;
    }

    public void prepareDevice() throws IOException {
        List<String> devices = getDevices();

        try {
            Path apkPath = Files.createTempFile("app", ".apk");

            // Download app apk
            try (ReadableByteChannel in = Channels.newChannel(new URL(TESTER_APK_URL).openStream());
                 FileChannel out = FileChannel.open(apkPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                out.transferFrom(in, 0, Long.MAX_VALUE);
            }
            File apkFile = apkPath.toFile();
            for (String device : devices) {
                installApp(device, TEST_APP_PKG_NAME, apkFile);
            }
        } catch (IOException ioe) {
            Log.e(TEST_APP_APK_NAME + " wasn't found. Tester may not produce output.");
            throw ioe;
        }
    }

    public void before() {
        List<String> devices = getDevices();
        for (String device : devices) {
            // An app dev might run battery tests locally with Android studio
            // without using Eagle Tester Jenkins plugin. This might leave leftover traces folder.
            // We thus clear such folder before starting the build for this commit.
            rmFolderFromDevice(device, PHONE_TRACES_FOLDER);
        }
    }

    public File after(File outputFolder) {
        List<String> devices = getDevices();
        // TODO: Investigate why we have multiple devices here??
        // WARN: Doesn't handle multiple devices. Just returns from first device
        for (String device : devices) {
            try {
                Log.i("Pull files from " + device);
                File tracesDir = outputFolder.toPath().resolve("traces").toFile();
                if (tracesDir.exists()) {
                    FileUtils.deleteDirectory(tracesDir);
                }
                pullFolderFromDevice(device, PHONE_TRACES_FOLDER, outputFolder);
                rmFolderFromDevice(device, PHONE_TRACES_FOLDER);
                forceStopApp(device, TEST_APP_PKG_NAME);

                if (tracesDir.exists()) {
                    // Compress the trace folder
                    Path outputFolderPath = outputFolder.toPath();
                    Path zipFilePath = outputFolderPath.resolve("traces.zip");
                    File zipFile = zipFilePath.toFile();
                    if (zipFile.exists())
                        zipFile.delete();
                    ZipUtil.pack(tracesDir, zipFile);
                    return zipFile;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected abstract void rmFolderFromDevice(String device, String phoneTracesFolder);

    protected abstract void pushToDevice(String device, String pkgName, File file);

    protected abstract void pullFolderFromDevice(String device, String folderPath, File outputFolder);

    protected abstract void installApp(String device, String appPkgName, File apkFile);

    protected abstract void forceStopApp(String device, String appPkgName);

    protected abstract List<String> getDevices();
}
