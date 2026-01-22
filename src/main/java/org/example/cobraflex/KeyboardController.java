package org.example.cobraflex;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.example.cobraflex.clients.CobraFlexClient;
import org.example.cobraflex.clients.ESP32S3Client;
import org.example.cobraflex.clients.MovingDirection;

/*
 Apple key codes:
  - command: 157
  - option: 18
  - cursor up: 38
  - cursor down: 40
  - cursor right: 39
  - cursor left: 37
  - numpad '0': 96
 */
@Slf4j
public class KeyboardController {

  private final ESP32S3Client esp32;
  private final CobraFlexClient cobraflex;
  private boolean optionKeyPressed = false;

  public KeyboardController(ESP32S3Client esp32, CobraFlexClient cobraflex) {
    this.esp32 = esp32;
    this.cobraflex = cobraflex;
  }

  public void keyPressed(KeyEvent e) {
    switch (e.getCode()) {
      case KeyCode.SHIFT -> optionKeyPressed = true;
      case KeyCode.H -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(-1, 0);
          esp32.get(cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.WEST);
          esp32.get(cmd);
        }
      }
      case KeyCode.J -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(0, 1);
          esp32.get(cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.NORTH);
          esp32.get(cmd);
        }
      }
      case KeyCode.K -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(0, -1);
          esp32.get(cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.SOUTH);
          esp32.get(cmd);
        }
      }
      case KeyCode.L -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(1, 0);
          esp32.get(cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.EAST);
          esp32.get(cmd);
        }
      }
      case KeyCode.A -> {
        String cmd = cobraflex.turn_pan_tilt_led();
        esp32.get(cmd);
      }
      case KeyCode.SPACE -> {
        String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
        esp32.get(cmd);
        cmd = cobraflex.cmd_gimbal_ctrl_stop();
        esp32.get(cmd);
      }
      default -> log.info("unexpected key pressed: char={} code={}, ignored",
          e.getText(), e.getCode());
    }
  }

  public void keyReleased(KeyEvent e) {
    if (e.getCode() == KeyCode.SHIFT) {
      optionKeyPressed = false;
    }
  }
}
