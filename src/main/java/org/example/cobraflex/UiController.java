package org.example.cobraflex;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.cobraflex.clients.CobraFlexClient;
import org.example.cobraflex.clients.JetsonOrinNanoClient;
import org.example.cobraflex.clients.MovingDirection;

@Slf4j
public class UiController {

  private static final String FEEDBACK_PATH = "/cobraflex/feedback";
  private static final String CMD_PATH = "/cobraflex/cmd";

  @FXML
  public Slider front_light;
  @FXML
  public Slider chassis_speed;
  @FXML
  public TextArea console;

  @Setter
  private Stage stage;

  private CobraFlexClient cobraflex;
  private JetsonOrinNanoClient jetson;
  private KeyboardController keyboardController;
  private Timer gimbalTimer;
  private Timer chassisTimer;
  private MovingDirection currentDirection = MovingDirection.STOP;


  @FXML
  public void initialize() {
    this.cobraflex = new CobraFlexClient();
    this.jetson = new JetsonOrinNanoClient();
    keyboardController = new KeyboardController(jetson, cobraflex);
    ctrl_cobraflex_led(0);
    front_light.valueProperty().addListener(
        (_, _, newValue) ->
            ctrl_cobraflex_led(newValue.intValue()));
    chassis_speed.valueProperty().addListener(
        (_, _, newValue) ->
            cobraflex.setSpeedLevel(newValue.intValue()));
    Platform.runLater(() -> stage.setOnCloseRequest(_ -> exitApplication()));
    log.info("CobraFlex RC initialized");
  }

  @FXML
  public void getFeedback() {
    JsonNode result = jetson.get(FEEDBACK_PATH);
    console.appendText(result + "\n");
  }

  // gimbal upper left button
  @FXML
  public void gul_pressed() {
    repeat_gimbal_cmd(-1, 1);
  }

  // gimbal upper middle button
  @FXML
  public void gum_pressed() {
    repeat_gimbal_cmd(0, 1);
  }

  // gimbal upper right button
  @FXML
  public void gur_pressed() {
    repeat_gimbal_cmd(1, 1);
  }

  // gimbal middle left button
  @FXML
  public void gml_pressed() {
    repeat_gimbal_cmd(-1, 0);
  }

  // gimbal middle middle button
  @FXML
  public void gmm_pressed() {
    String cmd = cobraflex.cmd_gimbal_ctrl_simple(0, 0);
    jetson.post(CMD_PATH, cmd);
  }

  // gimbal middle right button
  @FXML
  public void gmr_pressed() {
    repeat_gimbal_cmd(1, 0);
  }

  // gimbal bottom left button
  @FXML
  public void gbl_pressed() {
    repeat_gimbal_cmd(-1, -1);
  }

  // gimbal bottom middle button
  @FXML
  public void gbm_pressed() {
    repeat_gimbal_cmd(0, -1);
  }

  // gimbal bottom right button
  @FXML
  public void gbr_pressed() {
    repeat_gimbal_cmd(1, -1);
  }

  // chassis upper left button
  @FXML
  public void cul_pressed() {
    repeat_chassis_cmd(MovingDirection.NORTHWEST);
  }

  // chassis upper middle button
  @FXML
  public void cum_pressed() {
    currentDirection = MovingDirection.NORTH;
    repeat_chassis_cmd(MovingDirection.NORTH);
  }

  // chassis upper right button
  @FXML
  public void cur_pressed() {
    currentDirection = MovingDirection.NORTHEAST;
    repeat_chassis_cmd(MovingDirection.NORTHEAST);
  }

  // chassis middle left button
  @FXML
  public void cml_pressed() {
    currentDirection = MovingDirection.WEST;
    repeat_chassis_cmd(MovingDirection.WEST);
  }

  // chassis middle middle button
  @FXML
  public void cmm_pressed() {
    currentDirection = MovingDirection.STOP;
    String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
    jetson.post(CMD_PATH, cmd);
  }

  // chassis middle right button
  @FXML
  public void cmr_pressed() {
    currentDirection = MovingDirection.EAST;
    repeat_chassis_cmd(MovingDirection.EAST);
  }

  // chassis bottom left button
  @FXML
  public void cbl_pressed() {
    currentDirection = MovingDirection.SOUTHWEST;
    repeat_chassis_cmd(MovingDirection.SOUTHWEST);
  }

  // chassis bottom middle button
  @FXML
  public void cbm_pressed() {
    currentDirection = MovingDirection.SOUTH;
    repeat_chassis_cmd(MovingDirection.SOUTH);
  }

  // chassis bottom right button
  @FXML
  public void cbr_pressed() {
    currentDirection = MovingDirection.SOUTHEAST;
    repeat_chassis_cmd(MovingDirection.SOUTHEAST);
  }

  @FXML
  public void keyPressed(KeyEvent event) {
    keyboardController.keyPressed(event);
  }

  @FXML
  public void keyReleased(KeyEvent event) {
    keyboardController.keyReleased(event);
  }

  @FXML
  public void enterKeyboardControl() {
    // focus on button is sufficient
  }

  private void repeat_gimbal_cmd(int delta_pan, int delta_tilt) {
    if (gimbalTimer != null) {
      gimbalTimer.cancel();
    }
    gimbalTimer = new Timer();
    gimbalTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        String cmd = cobraflex.gimbal_step(delta_pan, delta_tilt);
        jetson.post(CMD_PATH, cmd);
      }
    }, 0, 50);
  }

  @FXML
  public void gimbal_released() {
    if (gimbalTimer != null) {
      gimbalTimer.cancel();
    }
    String cmd = cobraflex.cmd_gimbal_ctrl_stop();
    jetson.post(CMD_PATH, cmd);
    getFeedback();
  }

  private void repeat_chassis_cmd(MovingDirection direction) {
    if (chassisTimer != null) {
      chassisTimer.cancel();
    }
    int speedLevel = cobraflex.getSpeedLevel();
    int i = 100;
    while (i < speedLevel) {
      cobraflex.setSpeedLevel(i);
      String cmd = cobraflex.cmd_speed_control(direction);
      jetson.post(CMD_PATH, cmd);
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        log.error(e.getMessage());
      }
      i += 50;
    }
    cobraflex.setSpeedLevel(speedLevel);
    chassisTimer = new Timer();
    chassisTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        String cmd = cobraflex.cmd_speed_control(direction);
        jetson.post(CMD_PATH, cmd);
      }
    }, 0, 1200);
  }

  @FXML
  public void chassis_released() {
    if (chassisTimer != null) {
      chassisTimer.cancel();
    }
    if (currentDirection != MovingDirection.STOP) {
      int last_speedLevel = cobraflex.getSpeedLevel();
      int i = last_speedLevel;
      while (i > 200) {
        i -= 200;
        cobraflex.setSpeedLevel(i);
        String cmd = cobraflex.cmd_speed_control(currentDirection);
        jetson.post(CMD_PATH, cmd);
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          log.error(e.getMessage());
        }
      }
      cobraflex.setSpeedLevel(last_speedLevel);
      currentDirection = MovingDirection.STOP;
      String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
      jetson.post(CMD_PATH, cmd);
    }
    getFeedback();
  }

  private void ctrl_cobraflex_led(int brightness) {
    String cmd = cobraflex.ctrl_cobraflex_led(brightness);
    jetson.post(CMD_PATH, cmd);
  }

  private void exitApplication() {
    chassis_released();
    gimbal_released();
    ctrl_cobraflex_led(0);
  }
}
