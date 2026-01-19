package org.example.cobraflex;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
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

  private final ESP32S3Client esp32S3Client;
  private boolean optionKeyPressed = false;

  public KeyboardController(ESP32S3Client esp32S3Client) {
    this.esp32S3Client = esp32S3Client;
  }

  public void keyPressed(KeyEvent e) {
    switch (e.getCode()) {
      case KeyCode.SHIFT -> optionKeyPressed = true;
      case KeyCode.H -> {
        if (optionKeyPressed) {
          esp32S3Client.gimbal_step(-1, 0);
        } else {
          esp32S3Client.cmd_speed_control(MovingDirection.WEST);
        }
      }
      case KeyCode.J -> {
        if (optionKeyPressed) {
          esp32S3Client.gimbal_step(0, 1);
        } else {
          esp32S3Client.cmd_speed_control(MovingDirection.NORTH);
        }
      }
      case KeyCode.K -> {
        if (optionKeyPressed) {
          esp32S3Client.gimbal_step(0, -1);
        } else {
          esp32S3Client.cmd_speed_control(MovingDirection.SOUTH);
        }
      }
      case KeyCode.L -> {
        if (optionKeyPressed) {
          esp32S3Client.gimbal_step(1, 0);
        } else {
          esp32S3Client.cmd_speed_control(MovingDirection.EAST);
        }
      }
      case KeyCode.A -> esp32S3Client.turn_pan_tilt_led();
      case KeyCode.SPACE -> {
        esp32S3Client.cmd_speed_control(MovingDirection.STOP);
        esp32S3Client.cmd_gimbal_ctrl_stop();
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
