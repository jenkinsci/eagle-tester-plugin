package com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins;


import com.mobileenerlytics.eagle.tester.LocalOperation;
import com.mobileenerlytics.eagle.tester.common.util.DeviceUtils;

import java.io.File;
import java.util.List;

public class JenkinsLocalOperation extends LocalOperation {
    private JenkinsLocalOperation(String adb) {
        super(adb);
    }

    static JenkinsLocalOperation localOperation = null;

    public static JenkinsLocalOperation getInstance(String adb) {
        if (localOperation == null) {
            localOperation = new JenkinsLocalOperation(adb);
        }
        localOperation.adb = adb;
        return localOperation;
    }

    @Override
    protected void rmFolderFromDevice(String device, String folderPath) {
        DeviceUtils.rmFromDevice(adb, device, folderPath);
    }

    @Override
    protected void pushToDevice(String device, String pkgName, File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pullFolderFromDevice(String device, String folderPath, File file) {
        DeviceUtils.pullFromDevice(adb, device, folderPath, file);
    }

    @Override
    protected void installApp(String device, String appPkgName, File apkFile) {
        DeviceUtils.installApp(adb, device, appPkgName, apkFile);
    }

    @Override
    protected void forceStopApp(String device, String appPkgName) {
        DeviceUtils.forceStopApp(adb, device, appPkgName);
    }

    @Override
    public List<String> getDevices() {
        return DeviceUtils.getDevices(adb);
    }
}
