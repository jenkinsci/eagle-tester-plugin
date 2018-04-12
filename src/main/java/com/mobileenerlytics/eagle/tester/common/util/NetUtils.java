package com.mobileenerlytics.eagle.tester.common.util;

import com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins.EagleWrapper;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Map;

public class NetUtils {
    public static Response upload(File fileToUpload, final String url, EagleWrapper.DescriptorImpl desc,
                                  Map<String, String> fields) throws FileNotFoundException {
        // Upload traces to server
        Client client = getClient(desc);
        WebTarget webTarget = client.target(url);
        FormDataMultiPart multiPart = new FormDataMultiPart();
        for(Map.Entry<String, String> field: fields.entrySet()) {
            multiPart.field(field.getKey(), field.getValue());
        }
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        InputStream fileInStream = new FileInputStream(fileToUpload);

        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart(fileToUpload.getName(),
                fileToUpload, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        fileDataBodyPart.setContentDisposition(
                FormDataContentDisposition.name("file")
                        .fileName(fileToUpload.getName()).build());
        multiPart.bodyPart(fileDataBodyPart);

        return webTarget.request()
                .post(Entity.entity(multiPart, multiPart.getMediaType()));
//                .post(Entity.entity(fileInStream, MediaType.MULTIPART_FORM_DATA_TYPE));

    }

    public static boolean authenticate(EagleWrapper.DescriptorImpl desc) {
        Client client = getClient(desc);
        String url = "https://tester.mobileenerlytics.com/api/auth/";
        WebTarget webTarget = client.target(url);
        Response response = webTarget.request().get();
        if (200 == response.getStatusInfo().getStatusCode()) {
            Log.i("Authed ~");
            return true;
        }
        Log.w("Failed to authenticate. Check username, password");
        return false;
    }

    public static Client getClient(EagleWrapper.DescriptorImpl descriptor) {
        return ClientBuilder.newBuilder()
                .register(MultiPartFeature.class)
                .register(new AuthFilter(descriptor))
                .build();
    }
}
