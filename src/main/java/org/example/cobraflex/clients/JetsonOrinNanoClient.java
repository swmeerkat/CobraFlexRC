package org.example.cobraflex.clients;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.InputStream;
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

@Slf4j
public class JetsonOrinNanoClient {

  private final String host;

  public JetsonOrinNanoClient(String host) {
    this.host = host;
  }

  public JsonNode get(String path) throws RuntimeException {
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
              .setHttpHost(new HttpHost(host, 8000))
              .setPath(path)
              .build();
          log.info("Request: {}", path);
          responseData = client.execute(httpGet, response -> {
            if (response.getCode() >= 300) {
              log.error(new StatusLine(response).toString());
              client.close();
              throw new RuntimeException("JetsonClientError");
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
              log.info("Response: {}", responseData);
            }
          }
        } catch (IOException e) {
          log.error("Jetson Orin error: {}", e.getMessage());
        }
      }
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    }
    return responseData;
  }
}
