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
    private static final String TEST_APP_PKG_NAME = "com.mobileenerlytics.eagle.tester.app";
    private static final String TRACES_FOLDER = "traces";

    public LocalOperation(String adb) {
        this.adb = adb;
    }

    public void before() {
        List<String> devices = getDevices();
        for (String device : devices) {
            // An app dev might run battery tests locally with Android studio
            // without using Eagle Tester Jenkins plugin. This might leave leftover traces folder.
            // We thus clear such folder before starting the build for this commit.
            final String phoneTracesFolder = getExternalFilePath(device, TRACES_FOLDER);
            rmFolderFromDevice(device, phoneTracesFolder);
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
                final String phoneTracesFolder = getExternalFilePath(device, TRACES_FOLDER);
                pullFolderFromDevice(device, phoneTracesFolder, outputFolder);
                rmFolderFromDevice(device, phoneTracesFolder);
                forceStopApp(device, TEST_APP_PKG_NAME);

                if (tracesDir.exists()) {
                    // Compress the trace folder
                    Path outputFolderPath = outputFolder.toPath();
                    Path zipFilePath = outputFolderPath.resolve("traces.zip");
                    File zipFile = zipFilePath.toFile();
                    int ctr = 0;
                    while (zipFile.exists()) {
                        if(zipFile.delete())
                            break;
                        zipFilePath = outputFolderPath.resolve("traces-" + (++ctr) + ".zip");
                        zipFile = zipFilePath.toFile();
                    }
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

    protected abstract String getExternalFilePath(String device, String relativeFilePath);

    protected abstract List<String> getDevices();
}
