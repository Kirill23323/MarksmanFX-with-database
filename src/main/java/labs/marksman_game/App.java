package labs.marksman_game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import javafx.application.Platform;

public class App extends Application {

  private static Scene scene;

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("primary" + ".fxml"));
    scene = new Scene(loader.load(), Config.win_w, Config.win_h);
    stage.setScene(scene);
    stage.setTitle("MarksmanFX");
    stage.setResizable(false);
    PrimaryController controller = loader.getController();
    stage.setOnCloseRequest(e -> {
      controller.closeSocket();
      Platform.exit();
      System.exit(0);
    });
    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }

}
