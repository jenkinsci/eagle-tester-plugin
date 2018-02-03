package com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins;


import com.mobileenerlytics.eagle.tester.LocalOperation;
import com.mobileenerlytics.eagle.tester.common.util.DeviceUtils;
import com.mobileenerlytics.eagle.tester.common.util.Log;
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

import java.io.File;
import java.util.List;

public class JenkinsLocalOperation extends LocalOperation {
    private JenkinsLocalOperation(String adb) {
        super(adb);
    }

    static JenkinsLocalOperation localOperation = null;

    public static JenkinsLocalOperation getInstance(String adb) {
        DescribableList<NodeProperty<?>,NodePropertyDescriptor> nodes = Jenkins.getInstance().getGlobalNodeProperties();
        String expandedAdb = adb;
        for(EnvironmentVariablesNodeProperty tmp : nodes.getAll(EnvironmentVariablesNodeProperty.class)){
            EnvVars env = tmp.getEnvVars();
            expandedAdb = env.expand(expandedAdb);
        }
        Log.i("Use adb: " + expandedAdb);
        if (localOperation == null) {
            localOperation = new JenkinsLocalOperation(expandedAdb);
        }
        localOperation.adb = expandedAdb;
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
    protected String getExternalFilePath(String device, String relativeFilePath) {
        return String.format("%s/%s", DeviceUtils.getExternalStorage(adb, device), relativeFilePath);
    }

    @Override
    public List<String> getDevices() {
        return DeviceUtils.getDevices(adb);
    }
}
