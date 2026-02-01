package org.example.cobraflex;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.example.cobraflex.clients.CobraFlexClient;
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

  private final CobraFlexClient cobraflex;
  private boolean optionKeyPressed = false;

  public KeyboardController(CobraFlexClient cobraflex) {
    this.cobraflex = cobraflex;
  }

  public void keyPressed(KeyEvent e) {
    switch (e.getCode()) {
      case KeyCode.SHIFT -> optionKeyPressed = true;
      case KeyCode.H -> {
        if (optionKeyPressed) {
          cobraflex.gimbal_step(-100, 0);
        } else {
          cobraflex.cmd_speed_control(MovingDirection.WEST);
        }
      }
      case KeyCode.J -> {
        if (optionKeyPressed) {
          cobraflex.gimbal_step(0, -100);
        } else {
          cobraflex.cmd_speed_control(MovingDirection.NORTH);
        }
      }
      case KeyCode.K -> {
        if (optionKeyPressed) {
          cobraflex.gimbal_step(0, 100);
        } else {
          cobraflex.cmd_speed_control(MovingDirection.SOUTH);
        }
      }
      case KeyCode.L -> {
        if (optionKeyPressed) {
          cobraflex.gimbal_step(100, 0);
        } else {
          cobraflex.cmd_speed_control(MovingDirection.EAST);
        }
      }
      case KeyCode.SPACE -> cobraflex.cmd_speed_control(MovingDirection.STOP);
      default -> log.info("unexpected key pressed: char={} code={}, ignored",
          e.getText(), e.getCode());
    }
  }

  public void keyReleased(KeyEvent e) {
    if (e.getCode() == KeyCode.SHIFT) {
      optionKeyPressed = false;
    } else {
      cobraflex.cmd_speed_control(MovingDirection.STOP);
    }
  }
}
