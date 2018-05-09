package com.mobileenerlytics.eagle.tester.common.util;

import com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins.EagleWrapper;
import hudson.util.FormValidation;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import hudson.ProxyConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;

public class NetUtils {
    public static CloseableHttpResponse upload(File fileToUpload, final URL url, EagleWrapper.DescriptorImpl desc,
                                               Map<String, String> fields) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            URLConnection urlConnection = ProxyConfiguration.open(url);
            HttpPost httppost = new HttpPost(urlConnection.getURL().toURI());
            setAuthHeader(httppost, desc);

            MultipartEntity multipartEntity = new MultipartEntity() {
                @Override
                public boolean isChunked() {
                    return true;
                }
            };

            FileBody fileBody = new FileBody(fileToUpload);
            multipartEntity.addPart("file", fileBody);

            for (Map.Entry<String, String> field : fields.entrySet()) {
                multipartEntity.addPart(field.getKey(), new StringBody(field.getValue()));
            }

            httppost.setEntity(multipartEntity);

            Log.d("Executing request: " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            Log.d(response.getStatusLine().toString());
            Log.d(EntityUtils.toString(response.getEntity()));
            return response;
        }
    }

    public static boolean authenticate(EagleWrapper.DescriptorImpl desc) throws IOException, URISyntaxException {
        return formAuthenticate(desc).kind.equals(FormValidation.Kind.OK);
    }

    public static FormValidation formAuthenticate(EagleWrapper.DescriptorImpl desc) throws IOException, URISyntaxException {
        if(desc.getLicense() != null && !desc.getLicense().equals("")) {
            return LicenseVerifier.getInstance().verify(desc.getLicense(), desc.getUsername());
        }
        URL url = new URL("https://tester.mobileenerlytics.com/api/auth/");
        URLConnection urlConnection = ProxyConfiguration.open(url);
        HttpGet httpget = new HttpGet(urlConnection.getURL().toURI());
        setAuthHeader(httpget, desc);

        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httpget)) {
            String status = response.getStatusLine().toString();
            String entity = EntityUtils.toString(response.getEntity());
            Log.d(status);
            Log.d(entity);
            if (200 == response.getStatusLine().getStatusCode()) {
                return FormValidation.ok("Authed ~");
            }
            return FormValidation.error("Failed to authenticate: " + status + " : " + entity);
        }
    }

    private static void setAuthHeader(HttpRequestBase requestBase, EagleWrapper.DescriptorImpl desc) {
        String encoding = Base64.getEncoder().encodeToString((desc.getUsername() + ":" + desc.getPassword()).
                getBytes(Charset.forName("UTF-8")));
        requestBase.setHeader("Authorization", "Basic " + encoding);
    }
}
