package org.fao.geonet.handle.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.fao.geonet.utils.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fao.geonet.handle.client.HandleSettings.LOGGER_NAME;

public class HandleRestClient implements IHandleClient {
    private static final String CONTENT_TYPE = "application/json";
    private final String apiUrl;
    private final String username;
    private final String password;
    private final String defaultAdminPermissions;
    private final ObjectMapper objectMapper;

    public HandleRestClient(String apiUrl, String username, String password, String defaultAdminPermissions) {
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        this.defaultAdminPermissions = defaultAdminPermissions;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void createOrUpdate(String handleIdentifier, String targetUrl, boolean includeAdmin) throws HandleClientException {
        Map<String, Object> payload = new HashMap<>();
        List<Object> values = new ArrayList<>();

        Map<String, Object> urlValue = new HashMap<>();
        urlValue.put("index", 1);
        urlValue.put("type", "URL");
        Map<String, Object> data = new HashMap<>();
        data.put("format", "string");
        data.put("value", targetUrl);
        urlValue.put("data", data);
        values.add(urlValue);

        if (includeAdmin && defaultAdminPermissions != null && !defaultAdminPermissions.isEmpty()) {
            Map<String, Object> adminValue = new HashMap<>();
            adminValue.put("index", 100);
            adminValue.put("type", "HS_ADMIN");
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("format", "string");
            adminData.put("value", defaultAdminPermissions);
            adminValue.put("data", adminData);
            values.add(adminValue);
        }

        payload.put("values", values);

        String target = apiUrl.endsWith("/") ? apiUrl + handleIdentifier : apiUrl + "/" + handleIdentifier;

        HttpPut put = new HttpPut(target);
        put.addHeader(new BasicHeader("Content-Type", CONTENT_TYPE));
        try {
            String body = objectMapper.writeValueAsString(payload);
            Log.debug(LOGGER_NAME, "Handle request payload for " + handleIdentifier + ": " + body);
            put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new HandleClientException("Unable to build request payload", e);
        }

        Log.info(LOGGER_NAME, "Calling Handle endpoint " + target + " (admin block included: " + includeAdmin + ")");
        try (CloseableHttpClient client = buildClient();
             CloseableHttpResponse response = client.execute(put)) {
            int statusCode = response.getStatusLine().getStatusCode();
            Log.info(LOGGER_NAME, "Handle response status for " + handleIdentifier + ": " + statusCode);
            if (statusCode < 200 || statusCode >= 300) {
                throw new HandleClientException("Handle service returned status " + statusCode);
            }
        } catch (IOException e) {
            throw new HandleClientException("Error executing handle request", e);
        }
    }

    private CloseableHttpClient buildClient() {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return HttpClientBuilder.create()
            .setDefaultCredentialsProvider(provider)
            .build();
    }
}
