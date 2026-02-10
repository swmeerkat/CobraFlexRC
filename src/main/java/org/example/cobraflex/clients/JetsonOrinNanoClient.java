package org.example.cobraflex.clients;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

@Slf4j
public class JetsonOrinNanoClient {

  private final String host;

  public JetsonOrinNanoClient() {
    Properties properties = loadProperties();
    this.host = properties.get("Jetson.host").toString();
    log.info("Jetson.host: {}", this.host);
  }

  public JsonNode get(String path) throws RuntimeException {
    JsonFactory jsonFactory = new JsonFactory();
    ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
    JsonNode responseData = JsonNodeFactory.instance.objectNode();
    PoolingHttpClientConnectionManager connManager = getConnManager();
    if (connManager != null) {
      try (CloseableHttpClient client = HttpClients.custom()
          .setConnectionManager(connManager).build()) {
        ClassicHttpRequest httpGet = ClassicRequestBuilder.get()
            .setScheme("http")
            .setHttpHost(new HttpHost(host, 8000))
            .setPath(path)
            .build();
        log.info("Get: {}", path);
        responseData = client.execute(httpGet, response -> {
          if (response.getCode() >= 300) {
            log.error(new StatusLine(response).toString());
          }
          final HttpEntity responseEntity = response.getEntity();
          if (responseEntity == null) {
            return JsonNodeFactory.instance.objectNode();
          }
          try (InputStream inputStream = responseEntity.getContent()) {
            return objectMapper.readTree(inputStream);
          }
        });
        if (responseData != null) {
          if (!responseData.isEmpty()) {
            log.info("Get response: {}", responseData);
          }
        }
      } catch (IOException e) {
        log.error("Get: {}", e.getMessage());
      }
    }
    return responseData;
  }

  public JsonNode post(String path, String cmd) throws RuntimeException {
    JsonFactory jsonFactory = new JsonFactory();
    ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
    JsonNode responseData = JsonNodeFactory.instance.objectNode();
    PoolingHttpClientConnectionManager connManager = getConnManager();
    if (connManager != null) {
      try (CloseableHttpClient client = HttpClients.custom()
          .setConnectionManager(connManager).build()) {
        ClassicHttpRequest httpPost = ClassicRequestBuilder.post()
            .setScheme("http")
            .setHttpHost(new HttpHost(host, 8000))
            .setPath(path)
            .setEntity(new StringEntity(cmd))
            .build();
        log.info("Post: {} {}", path, cmd);
        responseData = client.execute(httpPost, response -> {
          if (response.getCode() >= 300) {
            log.error(new StatusLine(response).toString());
          }
          final HttpEntity responseEntity = response.getEntity();
          if (responseEntity == null) {
            return JsonNodeFactory.instance.objectNode();
          }
          try (InputStream inputStream = responseEntity.getContent()) {
            return objectMapper.readTree(inputStream);
          }
        });
        if (responseData != null) {
          if (!responseData.isEmpty()) {
            log.info("Post response: {}", responseData);
          }
        }
      } catch (IOException e) {
        log.error("Post: {}", e.getMessage());
      }
    }
    return responseData;
  }

  PoolingHttpClientConnectionManager getConnManager() {
    PoolingHttpClientConnectionManager connManager;
    try {
      connManager = PoolingHttpClientConnectionManagerBuilder.create().build();
      connManager.setDefaultConnectionConfig(ConnectionConfig.custom()
          .setConnectTimeout(Timeout.ofSeconds(1))
          .setSocketTimeout(Timeout.ofSeconds(1))
          .setTimeToLive(TimeValue.ofHours(1))
          .build());
    } catch (RuntimeException e) {
      log.error("PoolingHttpClientConnectionManager: {}", e.getMessage());
      connManager = null;
    }
    return connManager;
  }

  private Properties loadProperties() {
    Properties properties = new Properties();
    InputStream stream =
        Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("application.properties");
    try {
      properties.load(stream);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException(e);
    }
    return properties;
  }
}
