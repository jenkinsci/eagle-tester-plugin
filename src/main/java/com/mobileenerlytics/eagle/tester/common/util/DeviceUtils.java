package com.mobileenerlytics.eagle.tester.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DeviceUtils {
    public static void pullFromDevice(final String adb, final String device, final String fromPath, final File toFile) {
        runAdb(adb, device, "pull", fromPath, toFile.getAbsolutePath());
    }

    private static String runAdb(String adb, String device, String... cmd) {
        final StringBuilder stringBuilder = new StringBuilder();

        String line;
        try {
            final List<String> adbCmd = new LinkedList<>();
            adbCmd.add(adb);
            if(!device.equals("")) {
                adbCmd.add("-s");
                adbCmd.add(device);
            }
            adbCmd.addAll(Arrays.asList(cmd));
            StringBuilder flatten = new StringBuilder();
            for(String s : adbCmd) {
                flatten.append(s);
                flatten.append(" ");
            }
            Log.d(flatten.toString());
            ProcessBuilder processBuilder = new ProcessBuilder(adbCmd);
            processBuilder.redirectErrorStream(true);
            final Process pr = processBuilder.start();
            try(BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream(), UTF_8))) {
                while ((line = buf.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
                pr.waitFor();
            }
        } catch (IOException e) {
            Log.d(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.d(e.getMessage());
            e.printStackTrace();
        }
        final String cmdreturn = stringBuilder.toString();
        Log.d("adb output: " + cmdreturn);
        return cmdreturn;

    }

    public static void rmFromDevice(String adb, String device, String folderPath) {
        runAdb(adb, device,"shell", "rm", "-rf ", folderPath);
    }

    public static void installApp(String adb, String device, String appPkgName, File apkFile) {
        //Uninstall app first
        runAdb(adb, device, "uninstall", appPkgName);
        runAdb(adb, device, "install", "-g", "-r", apkFile.getAbsolutePath());
    }

    public static void forceStopApp(String adb, String device, String appPkgName) {
        runAdb(adb, device, "shell", "am", "force-stop", appPkgName);
    }

    public static String getExternalStorage(String adb, String device) {
        String storage = runAdb(adb, device, "shell", "echo", "$EXTERNAL_STORAGE");
        return storage.replace("\n", "").replace("\r", "");
    }

    public static List<String> getDevices(String adb) {
        String result = DeviceUtils.runAdb(adb, "", "devices");
        String[] lines = result.split("\n");
        List<String> devices = new ArrayList<String>();
        for (String line : lines) {
            String tokens[] = line.split("[ \t]+");
            if (tokens.length < 2) {
                continue;
            }
            if (tokens[1].equals("device")) {
                devices.add(tokens[0]);
            }
        }
        return devices;
    }
}
