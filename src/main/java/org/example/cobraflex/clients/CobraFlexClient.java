package org.example.cobraflex.clients;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/*
 * References:
 *  - https://www.waveshare.com/wiki/ESP32-S3-DEV-KIT-N8R8
 *  - https://www.waveshare.com/wiki/Cobra_Flex
 *  - https://www.waveshare.com/wiki/2-Axis_Pan-Tilt_Camera_Module
 */
public class CobraFlexClient {

  private static final String FEEDBACK_PATH = "/cobraflex/feedback";
  private static final String CMD_PATH = "/cobraflex/cmd";
  private static final String GIMBAL_STEP_PATH = "/gimbal/step";
  private static final String GIMBAL_MIDDLE_POS_PATH = "/gimbal/middle_position";
  private static final String GIMBAL_CAMERA_PATH = "/gimbal/camera";

  @Getter
  private final int DEFAULT_SPEED = 600;
  private final JetsonOrinNanoClient jetson;
  @Getter
  private int speedLevel;
  @Setter
  @Getter
  private int actPan;
  @Setter
  @Getter
  private int actTilt;
  private int actualChassisLight = 0;
  private int actualGimbalLight = 0;
  private String gimbal_cam_pid = null;


  public CobraFlexClient() {
    this.jetson = new JetsonOrinNanoClient();
    this.speedLevel = getDEFAULT_SPEED();
    this.actPan = 0;
    this.actTilt = 0;
  }

  public void setSpeedLevel(int speedLevel) {
    if (speedLevel < 0) {
      speedLevel = 0;
    }
    this.speedLevel = speedLevel;
  }

  /*
   * CMD_FEEDBACK
   * * Output:
   *  - M1: speed of the left front wheel
   *  - M2: speed of the right front wheel
   *  - M3: speed of the right rear wheel
   *  - M4: speed of the left rear wheel
   *  - odl: mileage of the left wheel in cm after last start of the chassis
   *  - odr: mileage of the right wheel in cm after the last start of the chassis
   *  - v: voltage in mV
   */
  public JsonNode get_feedback() {
    return jetson.get(FEEDBACK_PATH);
  }

  /*
   * CMD_SPEED_CTRL
   * Input:
   *  - L, R: speed of the wheel, value range 0.5 - -0.5
   */
  public void cmd_speed_control(MovingDirection direction) {
    int left = 0;
    int right = 0;
    int reducedSpeed = speedLevel - (speedLevel / 3);
    switch (direction) {
      case NORTH -> {
        left = speedLevel;
        right = speedLevel;
      }
      case NORTHEAST -> {
        left = speedLevel;
        right = reducedSpeed;
      }
      case EAST -> {
        left = speedLevel;
        right = -speedLevel;
      }
      case SOUTHEAST -> {
        left = -speedLevel;
        right = -reducedSpeed;
      }
      case SOUTH -> {
        left = -speedLevel;
        right = -speedLevel;
      }
      case SOUTHWEST -> {
        left = -reducedSpeed;
        right = -speedLevel;
      }
      case WEST -> {
        left = -speedLevel;
        right = speedLevel;
      }
      case NORTHWEST -> {
        left = reducedSpeed;
        right = speedLevel;
      }
      case STOP -> {
      }
    }
    String cmd = "{\"T\":11,\"M1\":" + left + ",\"M2\":" + right + ",\"M3\":" + right + ",\"M4\":" + left + "}";
    jetson.post(CMD_PATH, cmd);
  }

  public void gimbal_middle_pos() {
    jetson.post(GIMBAL_MIDDLE_POS_PATH, "{}");
  }

  /*
   * delta_pan: -100 -> left, 0 -> no step, 100 -> right
   * delta_tilt: -100 -> up, 0 -> no step, 100 -> up
   */
  public void gimbal_step(int delta_pan, int delta_tilt) {
    String cmd = "{\"pan\":" + delta_pan + ",\"tilt\":" + delta_tilt + "}";
    jetson.post(GIMBAL_STEP_PATH, cmd);
  }

  /*
   *  CMD_LED_CTRL
   *  IO1: chassis front led left and right
   */
  public void ctrl_chassis_led(int brightness) {
    if (brightness < 0) {
      brightness = 0;
    } else if (brightness > 255) {
      brightness = 255;
    }
    if (brightness != actualChassisLight) {
      actualChassisLight = brightness;
    }
    String cmd = "{\"T\":132, \"IO1\":" + actualChassisLight + ",\"IO2\": " + actualGimbalLight + "}";
    jetson.post(CMD_PATH, cmd);
  }

  /*
   *  CMD_LED_CTRL
   *  IO2: gimbal led
   */
  public void ctrl_gimbal_led(int brightness) {
    if (brightness < 0) {
      brightness = 0;
    } else if (brightness > 255) {
      brightness = 255;
    }
    if (brightness != actualGimbalLight) {
      actualGimbalLight = brightness;
    }
    String cmd = "{\"T\":132, \"IO1\":" + actualChassisLight + ",\"IO2\": " + actualGimbalLight + "}";
    jetson.post(CMD_PATH, cmd);
  }

  public void switch_gimbal_camera(boolean camera_on) {
    if (camera_on) {
      gimbal_cam_pid = jetson.post(GIMBAL_CAMERA_PATH + "/on", "{}").toString();
    } else {
      jetson.post(GIMBAL_CAMERA_PATH + "/off", gimbal_cam_pid);
    }
  }
}
