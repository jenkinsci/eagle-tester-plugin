package com.mobileenerlytics.eagle.tester.common.util;

import com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins.EagleWrapper;
//import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LargeFileUploadTest {
//    @Test
    public void uploadTest() throws Exception {
        final Map<String, String> fields = new HashMap<>();
        fields.put("device", "d");
        fields.put("pkg", "pkg");
        fields.put("project_name", "pn");
        fields.put("author_name", "an");
        fields.put("author_email", "ae");
        fields.put("branch", "b");
        fields.put("commit", "c");
        fields.put("cur_version", "cv");

        final URL url = new URL("http://localhost:38673/api/upload/version_energy");

        Path filePath = Files.createTempFile("upload", ".zip");
        File file = filePath.toFile();
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        long length = 1024 * 1024 * 1024;
        length *= 4;   // 4 GB file
        f.setLength(length);
        EagleWrapper.DescriptorImpl desc = new EagleWrapper.DescriptorImpl();
        desc.setUsername("user");
        desc.setPassword("pass");
        NetUtils.upload(file, url, desc, fields);
    }
}
