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
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/*
 * References:
 *  - https://www.waveshare.com/wiki/ESP32-S3-DEV-KIT-N8R8
 *  - https://www.waveshare.com/wiki/Cobra_Flex
 */

@Slf4j
public class ESP32S3Client {

  private final String host;

  public ESP32S3Client() {
    Properties properties = loadProperties();
    this.host = properties.get("ESP32.host").toString();
    log.info("ESP32.host: {}", this.host);
  }

  public JsonNode get(String cmd) throws RuntimeException {
    JsonFactory jsonFactory = new JsonFactory();
    ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
    JsonNode responseData = JsonNodeFactory.instance.objectNode();
    PoolingHttpClientConnectionManager connManager;
    try {
      connManager = PoolingHttpClientConnectionManagerBuilder.create()
          .build();
      {
        connManager.setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(2))
            .setSocketTimeout(Timeout.ofSeconds(2))
            .setTimeToLive(TimeValue.ofHours(1))
            .build());
        try (CloseableHttpClient client = HttpClients.custom()
            .setConnectionManager(connManager).build()) {
          ClassicHttpRequest httpGet = ClassicRequestBuilder.get()
              .setScheme("http")
              .setHttpHost(new HttpHost(host))
              .setPath("/js")
              .addParameter("json", cmd)
              .build();
          log.info("Request: {}", cmd);
          responseData = client.execute(httpGet, response -> {
            if (response.getCode() >= 300) {
              log.error(new StatusLine(response).toString());
              client.close();
              throw new RuntimeException("ESPClientError");
            }
            final HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
              return JsonNodeFactory.instance.objectNode();
            }
            try (InputStream inputStream = responseEntity.getContent()) {
              return objectMapper.readTree(inputStream);
            }
          });
          log.info("Response: {}", responseData);
        } catch (IOException e) {
          log.error("ESP32Client error: {}", e.getMessage());
        }
        return responseData;
      }
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    }
    return responseData;
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