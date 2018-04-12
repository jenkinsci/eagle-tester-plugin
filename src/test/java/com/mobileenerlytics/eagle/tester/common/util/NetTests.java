package com.mobileenerlytics.eagle.tester.common.util;

import com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins.EagleWrapper;
//import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NetTests {
//    @Test
    public void uploadTest() throws IOException {
        Path filePath = Files.createTempFile("upload", ".zip");
        File file = filePath.toFile();
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        long length = 1024 * 1024 * 1024;
//        length *= 4;   // 4 GB file
        f.setLength(length);
        final String url = "http://localhost:38673/api/upload/version_energy";

        Map<String, String> fields = new HashMap<>();
        fields.put("device", "d");
        fields.put("pkg", "pkg");
        fields.put("project_name", "pn");
        fields.put("author_name", "an");
        fields.put("author_email", "ae");
        fields.put("branch", "b");
        fields.put("commit", "c");
        fields.put("cur_version", "cv");

        NetUtils.upload(file, url, new EagleWrapper.DescriptorImpl(), fields);
    }
}
