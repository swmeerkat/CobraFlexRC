package org.example.cobraflex;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.cobraflex.clients.ESP32S3Client;
import org.example.cobraflex.clients.JetsonOrinNanoClient;
import org.example.cobraflex.clients.MovingDirection;
import org.example.cobraflex.clients.SpeedLevel;

@Slf4j
public class RcController {

  @FXML
  private TextField bf_odl;
  @FXML
  private TextField bf_odr;
  @FXML
  private TextField bf_voltage;
  @FXML
  private TextArea console;

  @Setter
  private Stage stage;
  private ESP32S3Client cobra;
  private JetsonOrinNanoClient jetson;
  private KeyboardController keyboardController;
  private Timer gimbalTimer = null;
  private Timer chassisTimer = null;

  @FXML
  private void initialize() {
    Properties properties = loadProperties();
    String cobra_host = properties.get("COBRA.host").toString();
    log.info("cobra host: {}", cobra_host);
    cobra = initCobraClient(cobra_host);
    String jetson_host = properties.get("Jetson.host").toString();
    log.info("jetson host: {}", jetson_host);
    jetson = new JetsonOrinNanoClient(jetson_host);
    keyboardController = new KeyboardController(cobra);
    Timer feedbackTimer = new Timer();
    feedbackTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        getBaseFeedback();
      }
    }, 0, 10000);
    Platform.runLater(() -> stage.setOnCloseRequest(_ -> feedbackTimer.cancel()));
    log.info("CobraFlex RC initialized");
  }

  @FXML
  private void getBaseFeedback() {
    JsonNode result = cobra.cmd_base_feedback();
    bf_odl.setText(getParamValue("odl", result));
    bf_odr.setText(getParamValue("odr", result));
    bf_voltage.setText(roundParamValue("v", result));
  }

  @FXML
  private void getImuData() {
    JsonNode result = cobra.get_IMU_data();
    console.appendText(result.toPrettyString() + "\n");
  }

  // gimbal upper left button
  @FXML
  private void gul_pressed() {
    repeat_gimbal_cmd(-1, 1);
  }

  // gimbal upper middle button
  @FXML
  private void gum_pressed() {
    repeat_gimbal_cmd(0, 1);
  }

  // gimbal upper right button
  @FXML
  private void gur_pressed() {
    repeat_gimbal_cmd(1, 1);
  }

  // gimbal middle left button
  @FXML
  private void gml_pressed() {
    repeat_gimbal_cmd(-1, 0);
  }

  // gimbal middle middle button
  @FXML
  private void gmm_pressed() {
    cobra.cmd_gimbal_ctrl_simple(0, 0);
  }

  // gimbal middle right button
  @FXML
  private void gmr_pressed() {
    repeat_gimbal_cmd(1, 0);
  }

  // gimbal bottom left button
  @FXML
  private void gbl_pressed() {
    repeat_gimbal_cmd(-1, -1);
  }

  // gimbal bottom middle button
  @FXML
  private void gbm_pressed() {
    repeat_gimbal_cmd(0, -1);
  }

  // gimbal bottom right button
  @FXML
  private void gbr_pressed() {
    repeat_gimbal_cmd(1, -1);
  }

  // chassis upper left button
  @FXML
  private void cul_pressed() {
    repeat_chassis_cmd(MovingDirection.NORTHWEST);
  }

  // chassis upper middle button
  @FXML
  private void cum_pressed() {
    repeat_chassis_cmd(MovingDirection.NORTH);
  }

  // chassis upper right button
  @FXML
  private void cur_pressed() {
    repeat_chassis_cmd(MovingDirection.NORTHEAST);
  }

  // chassis middle left button
  @FXML
  private void cml_pressed() {
    repeat_chassis_cmd(MovingDirection.WEST);
  }

  // chassis middle middle button
  @FXML
  private void cmm_pressed() {
    cobra.cmd_speed_control(MovingDirection.STOP);
  }

  // chassis middle right button
  @FXML
  private void cmr_pressed() {
    repeat_chassis_cmd(MovingDirection.EAST);
  }

  // chassis bottom left button
  @FXML
  private void cbl_pressed() {
    repeat_chassis_cmd(MovingDirection.SOUTHWEST);
  }

  // chassis bottom middle button
  @FXML
  private void cbm_pressed() {
    repeat_chassis_cmd(MovingDirection.SOUTH);
  }

  // chassis bottom right button
  @FXML
  private void cbr_pressed() {
    repeat_chassis_cmd(MovingDirection.SOUTHEAST);
  }

  @FXML
  private void radio_button4() {
    cobra.setSpeed(SpeedLevel.LEVEL_FOUR);
  }

  @FXML
  private void radio_button3() {
    cobra.setSpeed(SpeedLevel.LEVEL_THREE);
  }

  @FXML
  private void radio_button2() {
    cobra.setSpeed(SpeedLevel.LEVEL_TWO);
  }

  @FXML
  private void radio_button1() {
    cobra.setSpeed(SpeedLevel.LEVEL_ONE);
  }

  @FXML
  protected void keyPressed(KeyEvent event) {
    keyboardController.keyPressed(event);
  }

  @FXML
  protected void keyReleased(KeyEvent event) {
    keyboardController.keyReleased(event);
  }

  @FXML
  protected void enterKeyboardControl() {
    // focus on button is sufficient
  }

  private ESP32S3Client initCobraClient(String host) {
    ESP32S3Client cobra = new ESP32S3Client(host);
    //cobra.cmd_gimbal_ctrl_simple(0, 0);
    return cobra;
  }

  private Properties loadProperties() {
    Properties properties = new Properties();
    InputStream stream =
        Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("application.properties");
    try {
      properties.load(stream);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException(e);
    }
    return properties;
  }

  private String getParamValue(String parameter, JsonNode result) {
    if (!result.isEmpty()) {
      return result.get(parameter).toString();
    }
    return "";
  }

  private String roundParamValue(String parameter, JsonNode result) {
    if (!result.isEmpty()) {
      Double num = Double.parseDouble(result.get(parameter).toString()) / 100;
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
        cobra.gimbal_step(delta_pan, delta_tilt);
      }
    }, 0, 50);
  }

  @FXML
  private void gimbal_released() {
    gimbalTimer.cancel();
  }

  private void repeat_chassis_cmd(MovingDirection direction) {
    if (chassisTimer != null) {
      chassisTimer.cancel();
    }
    chassisTimer = new Timer();
    chassisTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        cobra.cmd_speed_control(direction);
      }
    }, 0, 2900);
  }

  @FXML
  private void chassis_released() {
    cobra.cmd_speed_control(MovingDirection.STOP);
    chassisTimer.cancel();
  }
}
