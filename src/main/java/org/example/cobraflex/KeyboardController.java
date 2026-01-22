package org.example.cobraflex;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.example.cobraflex.clients.CobraFlexClient;
import org.example.cobraflex.clients.JetsonOrinNanoClient;
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

  private final JetsonOrinNanoClient jetson;
  private final CobraFlexClient cobraflex;
  private boolean optionKeyPressed = false;
  private static final String CMD_PATH = "/cobraflex/cmd";

  public KeyboardController(JetsonOrinNanoClient jetson, CobraFlexClient cobraflex) {
    this.jetson = jetson;
    this.cobraflex = cobraflex;
  }

  public void keyPressed(KeyEvent e) {
    switch (e.getCode()) {
      case KeyCode.SHIFT -> optionKeyPressed = true;
      case KeyCode.H -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(-1, 0);
          jetson.post(CMD_PATH, cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.WEST);
          jetson.post(CMD_PATH, cmd);
        }
      }
      case KeyCode.J -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(0, 1);
          jetson.post(CMD_PATH, cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.NORTH);
          jetson.post(CMD_PATH, cmd);
        }
      }
      case KeyCode.K -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(0, -1);
          jetson.post(CMD_PATH, cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.SOUTH);
          jetson.post(CMD_PATH, cmd);
        }
      }
      case KeyCode.L -> {
        if (optionKeyPressed) {
          String cmd = cobraflex.gimbal_step(1, 0);
          jetson.post(CMD_PATH, cmd);
        } else {
          String cmd = cobraflex.cmd_speed_control(MovingDirection.EAST);
          jetson.post(CMD_PATH, cmd);
        }
      }
      case KeyCode.A -> {
        String cmd = cobraflex.turn_pan_tilt_led();
        jetson.post(CMD_PATH, cmd);
      }
      case KeyCode.SPACE -> {
        String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
        jetson.post(CMD_PATH, cmd);
        cmd = cobraflex.cmd_gimbal_ctrl_stop();
        jetson.post(CMD_PATH, cmd);
      }
      default -> log.info("unexpected key pressed: char={} code={}, ignored",
          e.getText(), e.getCode());
    }
  }

  public void keyReleased(KeyEvent e) {
    if (e.getCode() == KeyCode.SHIFT) {
      optionKeyPressed = false;
    } else {
      String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
      jetson.post(CMD_PATH, cmd);
    }
  }
}
