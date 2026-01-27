package org.example.cobraflex.clients;

import lombok.Getter;
import lombok.Setter;

/*
 * References:
 *  - https://www.waveshare.com/wiki/ESP32-S3-DEV-KIT-N8R8
 *  - https://www.waveshare.com/wiki/Cobra_Flex
 *  - https://www.waveshare.com/wiki/2-Axis_Pan-Tilt_Camera_Module
 */
public class CobraFlexClient {

  @Getter
  private static final int DEFAULT_SPEED = 500;

  @Setter
  private int speed_level;
  private int actPan;
  private int actTilt;

  public CobraFlexClient() {
    this.speed_level = getDEFAULT_SPEED();
    this.actPan = 0;
    this.actTilt = 0;
  }

  /*
   * CMD_FEEDBACK
   * * Output:
   *  - M1: speed of the left front wheel
   *  - M2: speed of the right front wheel
   *  - M3: speed of the left rear wheel
   *  - M4: speed of the right rear wheel
   *  - odl: mileage of the left wheel in cm after last start of the chassis
   *  - odr: mileage of the right wheel in cm after the last start of the chassis
   *  - v: voltage in mV
   */
  public String cmd_feedback() {
    return "{\"T\":130}";
  }

  /*
   * CMD_SPEED_CTRL
   * Input:
   *  - L, R: speed of the wheel, value range 0.5 - -0.5
   */
  public String cmd_speed_control(MovingDirection direction ) {
    int leftF = 0;
    int rightF = 0;
    int leftR = 0;
    int rightR = 0;
    switch (direction) {
      case NORTH -> {
        leftF = speed_level;
        rightF = speed_level;
        leftR = speed_level;
        rightR = speed_level;
      }
      case NORTHEAST -> {
        leftF = speed_level;
        rightF = speed_level / 2;
        leftR = speed_level;
        rightR = speed_level / 2;
      }
      case EAST -> {
        leftF = speed_level;
        rightF = -speed_level;
        leftR = speed_level;
        rightR = -speed_level;
      }
      case SOUTHEAST -> {
        leftF = -speed_level;
        rightF = -speed_level / 2;
        leftR = -speed_level;
        rightR = -speed_level / 2;
      }
      case SOUTH -> {
        leftF = -speed_level;
        rightF = -speed_level;
        leftR = -speed_level;
        rightR = -speed_level;
      }
      case SOUTHWEST -> {
        leftF = -speed_level / 2;
        rightF = -speed_level;
        leftR = -speed_level / 2;
        rightR = -speed_level;
      }
      case WEST -> {
        leftF = -speed_level;
        rightF = speed_level;
        leftR = -speed_level;
        rightR = speed_level;
      }
      case NORTHWEST -> {
        leftF = speed_level / 2;
        rightF = speed_level;
        leftR = speed_level / 2;
        rightR = speed_level;
      }
      case STOP -> {
      }
    }
    return "{\"T\":11,\"M1\":" + leftF + ",\"M2\":" + rightF + ",\"M3\":" + rightR + ",\"M4\":"
        + leftR + "}";
  }

  /*
   * CMD_GIMBAL_CTRL_SIMPLE
   * Input:
   *  - X: PAN, value range -180 to 180
   *  - Y: Tilt, value range -30 to 90
   *  - SPD: Speed, 0 means fastest
   *  - ACC: Acceleration, 0 means fastest
   */
  public String cmd_gimbal_ctrl_simple(int pan, int tilt) {
    String cmd = "{\"T\":133,\"X\":" + pan + ",\"Y\":" + tilt + ",\"SPD\":0,\"ACC\":0} ";
    actPan = pan;
    actTilt = tilt;
    return cmd;
  }

  /*
   * CMD_GIMBAL_CTRL_STOPE
   * Stops the pan-tilt movement at any time
   */
  public String cmd_gimbal_ctrl_stop() {
    return "{\"T\":135} ";
  }

  /*
   * delta_pan: -1 -> step left, 0 -> none, 1 -> step right
   * delta_tilt: -1 -> step down, 0 -> none, 1 -> step up
   */
  public String gimbal_step(int delta_pan, int delta_tilt) {
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
    return cmd_gimbal_ctrl_simple(new_pan, new_tilt);
  }

  /*
   *  CMD_LED_CTRL
   *  IO1: chassis front led left and right
   *  IO2: no function (difference to wiki description)
   *  IO5 controls pan-tilt LED
   */
  public String ctrl_cobraflex_led(int brightness) {
    return "{\"T\":132, \"IO1\":" + brightness + ",\"IO2\": 0}";
  }
}
