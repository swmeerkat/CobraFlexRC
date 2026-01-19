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

/*
 * References:
 *  - https://www.waveshare.com/wiki/ESP32-S3-DEV-KIT-N8R8
 *  - https://www.waveshare.com/wiki/Cobra_Flex
 *  - https://www.waveshare.com/wiki/2-Axis_Pan-Tilt_Camera_Module
 */

@Slf4j
public class ESP32S3Client {

  private final String host;
  private int speed;
  private int actPan;
  private int actTilt;
  private boolean panTiltLed;

  public ESP32S3Client(String host) {
    this.host = host;
    this.speed = SpeedLevel.LEVEL_ONE.getSpeed();
    this.actPan = 0;
    this.actTilt = 0;
    this.panTiltLed = false;
  }

  public void setSpeed(SpeedLevel level) {
    this.speed = level.getSpeed();
  }

  /*
   * Retrieve IMU data
   * Output:
   *  - M1: speed of the left front wheel
   *  - M2: speed of the right front wheel
   *  - M3: speed of the left rear wheel
   *  - M4: speed of the right rear wheel
   *  - odl: mileage of the left wheel in cm after last start of the chassis
   *  - odr: mileage of the right wheel in cm after the last start of the chassis
   *  - v: voltage in mV
   */
  public JsonNode get_IMU_data() {
    String cmd = "{\"T\":126}";
    return get(cmd);
  }

  /*
   * CMD_BASE_FEEDBACK
   * Output:
   *  - same like IMU data
   */
  public JsonNode cmd_base_feedback() {
    String cmd = "{\"T\":130}";
    return get(cmd);
  }

  /*
   * CMD_SPEED_CTRL
   * Input:
   *  - L, R: speed of the wheel, value range 0.5 - -0.5
   */
  public void cmd_speed_control(MovingDirection direction) {
    int leftF = 0;
    int rightF = 0;
    int leftR = 0;
    int rightR = 0;
    switch (direction) {
      case NORTH -> {
        leftF = speed;
        rightF = speed;
        leftR = speed;
        rightR = speed;
      }
      case NORTHEAST -> {
        leftF = speed;
        rightF = SpeedLevel.LEVEL_ONE.getSpeed();
        leftR = speed;
        rightR = SpeedLevel.LEVEL_ONE.getSpeed();
      }
      case EAST -> {
        leftF = speed;
        rightF = -speed / 2;
        leftR = speed / 2;
        rightR = -speed;
      }
      case SOUTHEAST -> {
        leftF = -speed;
        rightF = -speed / 2;
        leftR = -speed;
        rightR = -speed / 2;
      }
      case SOUTH -> {
        leftF = -speed;
        rightF = -speed;
        leftR = -speed;
        rightR = -speed;
      }
      case SOUTHWEST -> {
        leftF = speed / 2;
        rightF = -speed;
        leftR = -speed / 2;
        rightR = -speed;
      }
      case WEST -> {
        leftF = speed;
        rightF = speed / 2;
        leftR = -speed / 2;
        rightR = -speed;
      }
      case NORTHWEST -> {
        leftF = speed / 2;
        rightF = speed;
        leftR = speed / 2;
        rightR = speed;
      }
      case STOP -> {}
    }
    String cmd = "{\"T\":11,\"M1\":" + leftF + ",\"M2\":" + rightF + ",\"M3\":" + rightR + ",\"M4\":" + leftR + "}";
    get(cmd);
  }

  /*
   * CMD_GIMBAL_CTRL_SIMPLE
   * Input:
   *  - X: PAN, value range -180 to 180
   *  - Y: Tilt, value range -30 to 90
   *  - SPD: Speed, 0 means fastest
   *  - ACC: Acceleration, 0 means fastest
   */
  public void cmd_gimbal_ctrl_simple(int pan, int tilt) {
    String cmd = "{\"T\":133,\"X\":" + pan + ",\"Y\":" + tilt + ",\"SPD\":0,\"ACC\":0} ";
    actPan = pan;
    actTilt = tilt;
    get(cmd);
  }

  /*
   * CMD_GIMBAL_CTRL_STOPE
   * Stops the pan-tilt movement at any time
   */
  public void cmd_gimbal_ctrl_stop() {
    String cmd = "{\"T\":135} ";
    get(cmd);
  }

  /*
   * delta_pan: -1 -> step left, 0 -> none, 1 -> step right
   * delta_tilt: -1 -> step down, 0 -> none, 1 -> step up
   */
  public void gimbal_step(int delta_pan, int delta_tilt) {
    int new_pan = actPan;
    int new_tilt = actTilt;
    if (delta_pan < 0) {
      if (actPan > -180) {
        new_pan = actPan - 2;
      }
    } else if (delta_pan > 0) {
      if (actPan < 180) {
        new_pan = actPan + 2;
      }
    }
    if (delta_tilt < 0) {
      if (actTilt > -30) {
        new_tilt = actTilt - 2;
      }
    } else if (delta_tilt > 0) {
      if (actTilt < 90) {
        new_tilt = actTilt + 2;
      }
    }
    cmd_gimbal_ctrl_simple(new_pan, new_tilt);
  }

  /*
   *  CMD_LED_CTRL
   *  IO5 controls pan-tilt LED
   */
  public void turn_pan_tilt_led() {
    int brightness;
    if (!panTiltLed) {
      panTiltLed = true;
      brightness = 255;
    } else {
      panTiltLed = false;
      brightness = 0;
    }
    String cmd = "{\"T\":132, \"IO4\":0,\"IO5\":" + brightness + "}";
    get(cmd);
  }

  private JsonNode get(String cmd) throws RuntimeException {
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
}