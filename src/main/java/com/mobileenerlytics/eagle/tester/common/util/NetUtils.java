package com.mobileenerlytics.eagle.tester.common.util;

import com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins.EagleWrapper;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;

public class NetUtils {
    public static CloseableHttpResponse upload(File fileToUpload, final String url, EagleWrapper.DescriptorImpl desc,
                                               Map<String, String> fields) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(url);
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

    public static boolean authenticate(EagleWrapper.DescriptorImpl desc) {
        HttpGet httpget = new HttpGet("https://tester.mobileenerlytics.com/api/auth/");
        setAuthHeader(httpget, desc);

        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httpget)) {
            Log.d(response.getStatusLine().toString());
            Log.d(EntityUtils.toString(response.getEntity()));
            if (200 == response.getStatusLine().getStatusCode()) {
                Log.i("Authed ~");
                return true;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.w("Failed to authenticate. Check username, password");
        return false;
    }

    private static void setAuthHeader(HttpRequestBase requestBase, EagleWrapper.DescriptorImpl desc) {
        String encoding = Base64.getEncoder().encodeToString((desc.getUsername() + ":" + desc.getPassword()).
                getBytes(Charset.forName("UTF-8")));
        requestBase.setHeader("Authorization", "Basic " + encoding);
    }
}
