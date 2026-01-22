package org.example.cobraflex;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RcApplication extends Application {

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(RcApplication.class.getResource("cobraflex.fxml"));
    Parent parent = fxmlLoader.load();
    UiController uiController = fxmlLoader.getController();
    uiController.setStage(stage);
    Scene scene = new Scene(parent);
    stage.setTitle("CobraFlex RC");
    stage.setScene(scene);
    stage.show();
  }
}
