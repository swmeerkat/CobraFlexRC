package org.example.cobraflex;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
  public TextField bf_odl;
  @FXML
  public TextField bf_odr;
  @FXML
  public TextField bf_voltage;
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
  private Timer feedbackTimer;


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
            ctrl_chassis_speed(newValue.intValue()));
    feedbackTimer = new Timer();
    feedbackTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        getFeedback();
      }
    }, 0, 10000);
    Platform.runLater(() -> stage.setOnCloseRequest(_ -> exitApplication()));
    log.info("CobraFlex RC initialized");
  }

  @FXML
  public void getFeedback() {
    JsonNode result = jetson.get(FEEDBACK_PATH);
    bf_odl.setText(getParamValue("odl", result));
    bf_odr.setText(getParamValue("odr", result));
    bf_voltage.setText(roundParamValue(result));
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
    repeat_chassis_cmd(MovingDirection.NORTH);
  }

  // chassis upper right button
  @FXML
  public void cur_pressed() {
    repeat_chassis_cmd(MovingDirection.NORTHEAST);
  }

  // chassis middle left button
  @FXML
  public void cml_pressed() {
    repeat_chassis_cmd(MovingDirection.WEST);
  }

  // chassis middle middle button
  @FXML
  public void cmm_pressed() {
    String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
    jetson.post(CMD_PATH, cmd);
  }

  // chassis middle right button
  @FXML
  public void cmr_pressed() {
    repeat_chassis_cmd(MovingDirection.EAST);
  }

  // chassis bottom left button
  @FXML
  public void cbl_pressed() {
    repeat_chassis_cmd(MovingDirection.SOUTHWEST);
  }

  // chassis bottom middle button
  @FXML
  public void cbm_pressed() {
    repeat_chassis_cmd(MovingDirection.SOUTH);
  }

  // chassis bottom right button
  @FXML
  public void cbr_pressed() {
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

  private String getParamValue(String parameter, JsonNode result) {
    if (!result.isEmpty()) {
      return result.get(parameter).toString();
    }
    return "";
  }

  private String roundParamValue(JsonNode result) {
    if (!result.isEmpty()) {
      Double num = Double.parseDouble(result.get("v").toString()) / 100;
      DecimalFormat df = new DecimalFormat("##.##");
      return df.format(num);
    }
    return "";
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
  }

  private void repeat_chassis_cmd(MovingDirection direction) {
    if (chassisTimer != null) {
      chassisTimer.cancel();
    }
    chassisTimer = new Timer();
    chassisTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        String cmd = cobraflex.cmd_speed_control(direction);
        jetson.post(CMD_PATH, cmd);
      }
    }, 0, 1000);
  }

  @FXML
  public void chassis_released() {
    if (chassisTimer != null) {
      chassisTimer.cancel();
    }
    String cmd = cobraflex.cmd_speed_control(MovingDirection.STOP);
    jetson.post(CMD_PATH, cmd);
  }

  private void ctrl_cobraflex_led(int brightness) {
    String cmd = cobraflex.ctrl_cobraflex_led(brightness);
    jetson.post(CMD_PATH, cmd);
  }

  private void ctrl_chassis_speed(int speed_level) {
    cobraflex.setSpeed_level(speed_level);
  }

  private void exitApplication() {
    feedbackTimer.cancel();
    chassis_released();
    gimbal_released();
    ctrl_cobraflex_led(0);
  }
}
