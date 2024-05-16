package labs.marksman_game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import labs.marksman_game.GMessage.Connect;
import labs.marksman_game.GMessage.Exit;
import labs.marksman_game.GMessage.GMessageHandler;
import labs.marksman_game.GMessage.LeaderBoardSend;
import labs.marksman_game.GMessage.Pause;
import labs.marksman_game.GMessage.PlayerWon;
import labs.marksman_game.GMessage.Ready;
import labs.marksman_game.GMessage.Reject;
import labs.marksman_game.GMessage.ScoreSync;
import labs.marksman_game.GMessage.Shoot;
import labs.marksman_game.GMessage.Sync;
import labs.marksman_game.GMessage.Unpause;
import labs.marksman_game.GMessage.Unready;
import labs.marksman_game.Utils.PlayerWins;
import labs.marksman_game.Utils.PlayerWinsArray;

public class PrimaryController {

  private int port = 0;
  Socket socket;
  int slot = 0;
  PlayerWinsArray playerWinsArray;

  @FXML
  private HBox MainFrame;

  @FXML
  private VBox MainGameFrame;

  @FXML
  private HBox ButtonsFrame;

  @FXML
  private Pane GamePane;

  @FXML
  private Pane PlayersPane;

  @FXML
  private VBox ScoreFrame;

  @FXML
  private Polygon Player1Polygon;

  @FXML
  private Polygon Player2Polygon;

  @FXML
  private Polygon Player3Polygon;

  @FXML
  private Polygon Player4Polygon;


  private Polygon[] PlayerPolygons;

  @FXML
  private Circle Target1Circle;

  @FXML
  private Line Target1Line;

  @FXML
  private Circle Target2Circle;

  @FXML
  private Line Target2Line;

  @FXML
  private Polygon ArrowPoly1;
  @FXML
  private Polygon ArrowPoly2;
  @FXML
  private Polygon ArrowPoly3;
  @FXML
  private Polygon ArrowPoly4;

  private Polygon[] ArrowPolys;


  @FXML
  private VBox ScoreFramePlayer1;

  @FXML
  private VBox ScoreFramePlayer2;

  @FXML
  private VBox ScoreFramePlayer3;

  @FXML
  private VBox ScoreFramePlayer4;

  private VBox[] ScoreFramesPlayer;

  @FXML
  private Button StartGameBtn;

  @FXML
  private Button ReadyBtn;

  @FXML
  private Button ShootBtn;

  @FXML
  private Button PauseBtn;

  @FXML
  private Button ShowLeaderBoardBtn;

  private boolean isPaused = false;

  private boolean isReady = false;

  private boolean isGameGoing = false;

  private ClientMessageHandler clientMessageHandler = new ClientMessageHandler(this);

  private Thread listenThread = null;
  
  DataOutputStream dOut;
  DataInputStream dInp;

  // For server
  public double[][] arrowsPos;
  public double[] target1Pos;
  public double[] target1PosStart;
  public double[] target1PosEnd;
  public double[] target2Pos;
  public double[] target2PosStart;
  public double[] target2PosEnd;
  public double gamePaneWidth;

  class NumberField extends TextField {
    @Override
    public void replaceText(int start, int end, String text) {
      if (text.matches("[0-9]*")) {
        super.replaceText(start, end, text);
      }
    }
  
    @Override
    public void replaceSelection(String text) {
      if (text.matches("[0-9]*")) {
        super.replaceSelection(text);
      }
    }
  }

  public static final void buttonSetTrueVisibility(Button button, boolean value) {
    button.setManaged(value);
    button.setVisible(value);
  }

  private static class ClientMessageHandler extends GMessageHandler {
    private PrimaryController controller;

    ClientMessageHandler(PrimaryController controller) {
      this.controller = controller;
    }

    @Override
    public synchronized GMessage HandleConnect(GMessage mes) {
      Platform.runLater(() -> {
        controller.initialize_prepare();
        Connect conData = mes.getConnectData();
        controller.AddPlayer(conData.slot,conData.name,conData.wins);
      });
      return null;
    }
    @Override
    public GMessage HandleReject(GMessage mes) {
      Platform.runLater(() -> {
        Reject rejData = mes.getRejectData();
        switch (rejData.reason) {
          case ReasonType.NAME_EXIST:
            controller.createInfoPopup(null, "Имя уже существует на сервере");
            break;
          case ReasonType.GAME_FULL:
            controller.createInfoPopup(null, "Сервер заполнен");
            break;
          case ReasonType.GAME_GOING:
          controller.createInfoPopup(null, "Игра уже идёт");
            break;
        }
      });
      return null;
    }

    @Override
    public GMessage HandleExit(GMessage mes) {
      Platform.runLater(() -> {
        Exit exitData = mes.getExitData();
        controller.RemovePlayer(exitData.slot);
      });
      return null;
    }

    @Override
    public GMessage HandleGameBegin(GMessage mes) {
      Platform.runLater(() -> {
        controller.isGameGoing = true;
        controller.initialize_game();
      });
      return null;
    }

    @Override
    public GMessage HandleSync(GMessage mes) {
      Platform.runLater(() -> {
        Sync syncData = mes.getSyncData();
        controller.SyncGame(syncData.arrows.arr, syncData.target1PosY, syncData.target2PosY);
      });
      return null;
    }

    @Override
    public GMessage HandleShoot(GMessage mes) {
      Platform.runLater(() -> {
        Shoot shootData = mes.getShootData();
        controller.IncrementShotsCount(shootData.slot);
      });
      return null;
    }

    @Override
    public GMessage HandleScoreSync(GMessage mes) {
      Platform.runLater(() -> {
        ScoreSync scoreSyncData = mes.getScoreSyncData();
        controller.SetScore(scoreSyncData.slot, scoreSyncData.score);
      });
      return null;
    }

    @Override
    public GMessage HandlePlayerWon(GMessage mes) {
      Platform.runLater(() -> {
        PlayerWon playerWonData = mes.getPlayerWonData();
        controller.DeclareWinner(playerWonData.slot);
      });
      return null;
    }

    @Override
    public GMessage HandleLeaderBoardSend(GMessage mes){
      LeaderBoardSend leaderBoardSendData = mes.getLeaderBoardSendData();
      controller.playerWinsArray = leaderBoardSendData.arr;
      return null;
    }
    
  }

  private void initialize_dynamic_pos() {
    Target1Circle.setTranslateX(ButtonsFrame.getPrefWidth() * 0.7);
    Target1Circle.setTranslateY(GamePane.getPrefHeight() / 2 - Target1Circle.getRadius() / 2);
    Target1Line.setStartX(Target1Circle.getTranslateX());
    Target1Line.setStartY(7);
    Target1Line.setEndX(Target1Circle.getTranslateX());
    Target1Line.setEndY(GamePane.getPrefHeight() - 11);

    Target2Circle.setTranslateX(ButtonsFrame.getPrefWidth() * 0.9);
    Target2Circle.setTranslateY(GamePane.getPrefHeight() / 2 - Target2Circle.getRadius() / 2);
    Target2Line.setStartX(Target2Circle.getTranslateX());
    Target2Line.setStartY(Target1Line.getStartY());
    Target2Line.setEndX(Target2Circle.getTranslateX());
    Target2Line.setEndY(Target1Line.getEndY());

    for (int i = 0; i < ArrowPolys.length; i++) {
      ArrowPolys[i].setTranslateX(0);
      ArrowPolys[i].setVisible(false);
    }
  }

  private void initialize_start() {
    resetScore();
    for (int i = 0; i < PlayerPolygons.length; i++) {
      PlayerPolygons[i].setVisible(false);
      ScoreFramesPlayer[i].setVisible(false);
      ArrowPolys[i].setTranslateX(0);
      ArrowPolys[i].setVisible(false);
    }
    isGameGoing = false;
    isPaused = false;
    isReady = false;
    Target1Circle.setVisible(false);
    Target2Circle.setVisible(false);
    buttonSetTrueVisibility(StartGameBtn, true);
    buttonSetTrueVisibility(ReadyBtn, false);
    buttonSetTrueVisibility(ShootBtn, false);
    buttonSetTrueVisibility(PauseBtn, false);
    buttonSetTrueVisibility(ShowLeaderBoardBtn, false);
  }

  private void initialize_prepare() {
    Target1Circle.setVisible(false);
    Target2Circle.setVisible(false);
    buttonSetTrueVisibility(StartGameBtn, false);
    buttonSetTrueVisibility(ReadyBtn, true);
    buttonSetTrueVisibility(ShootBtn, false);
    buttonSetTrueVisibility(PauseBtn, false);
    buttonSetTrueVisibility(ShowLeaderBoardBtn, true);
  }

  public void initialize_game() {
    resetScore();
    ReadyBtn.setText("Готов");
    isReady = false;
    PauseBtn.setText("Пауза");
    isPaused = false;
    Target1Circle.setVisible(true);
    Target2Circle.setVisible(true);
    buttonSetTrueVisibility(StartGameBtn, false);
    buttonSetTrueVisibility(ReadyBtn, false);
    buttonSetTrueVisibility(ShootBtn, true);
    buttonSetTrueVisibility(PauseBtn, true);
    buttonSetTrueVisibility(ShowLeaderBoardBtn,true);
  }

  @FXML
  public void initialize() throws IOException {
    ScoreFramesPlayer = new VBox[] { ScoreFramePlayer1, ScoreFramePlayer2, ScoreFramePlayer3, ScoreFramePlayer4 };
    PlayerPolygons = new Polygon[] { Player1Polygon, Player2Polygon, Player3Polygon, Player4Polygon };
    ArrowPolys = new Polygon[] { ArrowPoly1, ArrowPoly2, ArrowPoly3, ArrowPoly4 };

    MainFrame.setPrefWidth(Config.win_w);
    ScoreFrame.setPrefWidth(ScoreFrame.getPrefWidth() * 1.25);
    ButtonsFrame.setPrefHeight(48);
    ButtonsFrame.setPrefWidth(Config.win_w - ScoreFrame.getPrefWidth());
    GamePane.setPrefWidth(ButtonsFrame.getPrefWidth());
    GamePane.setPrefHeight(Config.win_h - ButtonsFrame.getPrefHeight());
    PlayersPane.setPrefWidth(100);
    PlayersPane.setPrefHeight(GamePane.getPrefHeight() - 16);
    PlayersPane.setTranslateX(5);
    PlayersPane.setTranslateY(5);
    String cssTranslate = "-fx-border-color: black;\n" + "-fx-border-insets: 5;\n" + "-fx-border-width: 1;\n";
    String cssPlayersPaneStyle = "-fx-border-color: black;\n" +
                              "-fx-background-color: yellow;" +    
                              "-fx-border-width: 1;\n";     
    MainGameFrame.setStyle(cssTranslate);
    ButtonsFrame.setStyle(cssTranslate);
    GamePane.setStyle(cssTranslate);
    ScoreFrame.setStyle(cssTranslate);
    PlayersPane.setStyle(cssPlayersPaneStyle);

    resetScore();
    for (int i = 0; i < PlayerPolygons.length; i++) {
      PlayerPolygons[i].setTranslateX(30 * 1.5);
      PlayerPolygons[i].setTranslateY(GamePane.getPrefHeight() * (i + 1) / (PlayerPolygons.length * 1.25) - 30 / 2);
    }
   
    Target1Circle.setRadius(Config.target_radius);

    Target2Circle.setRadius(Config.target_radius / 2);

    initialize_dynamic_pos();
    initialize_start();

    for (int i = 0; i < ArrowPolys.length; i++) {
      Double[] arrow_line_start_end = new Double[] {
          PlayerPolygons[i].getTranslateX(), PlayerPolygons[i].getTranslateY(),
          PlayerPolygons[i].getTranslateX() + Config.arrow_length, PlayerPolygons[i].getTranslateY()
      };
      ArrowPolys[i].getPoints().clear();
      ArrowPolys[i].getPoints().addAll(new Double[] {
          arrow_line_start_end[0], arrow_line_start_end[1] - Config.arrow_width / 2,
          arrow_line_start_end[2], arrow_line_start_end[3] - Config.arrow_width / 2,
          // top left corner
          arrow_line_start_end[2], arrow_line_start_end[3] - Config.arrow_hitbox_radius * Math.sqrt(0.75),
          arrow_line_start_end[2] + Config.arrow_hitbox_radius * 1.5, arrow_line_start_end[3],
          // down left corner
          arrow_line_start_end[2], arrow_line_start_end[3] + Config.arrow_hitbox_radius * Math.sqrt(0.75),
          arrow_line_start_end[2], arrow_line_start_end[3] + Config.arrow_width / 2,
          arrow_line_start_end[0], arrow_line_start_end[1] + Config.arrow_width / 2,
      });
    }
    arrowsPos = new double[PlayerPolygons.length][2];
    for (int i = 0; i < PlayerPolygons.length; i++) {
      arrowsPos[i][0] = ArrowPolys[i].getPoints().get(2) + Config.arrow_hitbox_radius / 2;
      arrowsPos[i][1] = PlayerPolygons[i].getTranslateY();
    }
    target1Pos = new double[] { Target1Circle.getTranslateX(), Target1Circle.getTranslateY() };
    target1PosStart = new double[] { Target1Line.getStartX(), Target1Line.getStartY() };
    target1PosEnd = new double[] { Target1Line.getEndX(), Target1Line.getEndY() };

    target2Pos = new double[] { Target2Circle.getTranslateX(), Target2Circle.getTranslateY() };
    target2PosStart = new double[] { Target2Line.getStartX(), Target2Line.getStartY() };
    target2PosEnd = new double[] { Target2Line.getEndX(), Target2Line.getEndY() };

    gamePaneWidth = GamePane.getPrefWidth();
  }

  private void resetScore() {
    for (int i = 0; i < ScoreFramesPlayer.length; i++) {
      Label playerScoreLabel = (Label)ScoreFramesPlayer[i].getChildren().get(3);
      Label playerShotsLabel = (Label)ScoreFramesPlayer[i].getChildren().get(5);
      playerScoreLabel.setText("0");
      playerShotsLabel.setText("0");
    }
  }

  public void AddPlayer(int slot, String name,int wins) {
    PlayerPolygons[slot].setVisible(true);
    ScoreFramesPlayer[slot].setVisible(true);
    Label playerNameLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(1);
    playerNameLabel.setText(name);
    Label playerWinsLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(7);
    playerWinsLabel.setText("" + wins);
    ArrowPolys[slot].setTranslateX(0);
    ArrowPolys[slot].setVisible(false);
  }

  public void RemovePlayer(int slot) {
    PlayerPolygons[slot].setVisible(false);
    ScoreFramesPlayer[slot].setVisible(false);
    Label playerScoreLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(3);
    Label playerShotsLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(5);
    playerScoreLabel.setText("0");
    playerShotsLabel.setText("0");
    ArrowPolys[slot].setTranslateX(0);
    ArrowPolys[slot].setVisible(false);
  }

  public void SyncGame(Utils.ArrowState[] arrows, double target1Pos, double target2Pos) {
    for (int i = 0; i < arrows.length; i++) {
      ArrowPolys[i].setVisible(arrows[i].visible);
      ArrowPolys[i].setTranslateX(arrows[i].posX - arrowsPos[i][0]); 
    }
    Target1Circle.setTranslateY(target1Pos);
    Target2Circle.setTranslateY(target2Pos);
  }

  public void SetScore(int slot, int score) {
    Label playerScoreLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(3);
    playerScoreLabel.setText("" + score);
  }

  public void IncrementShotsCount(int slot) {
    Label playerShotsLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(5);
    playerShotsLabel.setText("" + (Integer.parseInt(playerShotsLabel.getText()) + 1));
  }

  public void DeclareWinner(int slot) {
    initialize_prepare();
    Label playerNameLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(1);
    Label playerWinsLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(7);
    playerWinsLabel.setText("" + (Integer.parseInt(playerWinsLabel.getText()) + 1));
    isGameGoing = false;
    createInfoPopup(null, "Игрок " + playerNameLabel.getText() + " победил!");
  }

  @FXML
  private void startGame() {
    port = 0;
    Gson gson = new Gson();
    Stage dialog = new Stage();
    VBox mainBox = new VBox();
    mainBox.setAlignment(Pos.CENTER);

    HBox portBox = new HBox();
    portBox.setAlignment(Pos.CENTER);
    portBox.getChildren().add(new Label("Порт"));
    NumberField portField = new NumberField();
    portField.setText(String.valueOf(Config.port));
    portBox.getChildren().add(portField);
    mainBox.getChildren().add(portBox);

    HBox nameBox = new HBox();
    nameBox.setAlignment(Pos.CENTER);
    nameBox.getChildren().add(new Label("Имя"));
    TextField nameField = new TextField();
    nameField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
        if (nameField.getText().length() > Config.name_max_length) {
          String s = nameField.getText().substring(0, Config.name_max_length);
          nameField.setText(s);
        }
      }
    });
    nameBox.getChildren().add(nameField);
    mainBox.getChildren().add(nameBox);

    HBox posBox = new HBox();
    posBox.setAlignment(Pos.CENTER);
    posBox.getChildren().add(new Label("Позиция"));
    NumberField posField = new NumberField();
    posField.setText("0");
    posBox.getChildren().add(posField);
    mainBox.getChildren().add(posBox);

    HBox btnsBox = new HBox();
    btnsBox.setAlignment(Pos.CENTER);
    Button okayButton = new Button();
    okayButton.setText("Войти");
    okayButton.setOnAction(value -> {
      String name = nameField.getText().trim();
      if (name.length() == 0) {
        createInfoPopup(mainBox.getScene().getWindow(), "Неверное имя");
        return;
      }
      try {
        this.port = Integer.valueOf(portField.getText());
        if (port <= 0) {
          createInfoPopup(MainFrame.getScene().getWindow(), "Неверный номер порта");
          return;
        }
      } catch (NumberFormatException e) {
        createInfoPopup(MainFrame.getScene().getWindow(), "Неверный номер порта");
        return;
      }
      dialog.close();
    });
    btnsBox.getChildren().add(okayButton);
    Button closeButton = new Button();
    closeButton.setText("Закрыть");
    closeButton.setOnAction(value -> {
      dialog.close();
    });
    btnsBox.getChildren().add(closeButton);
    mainBox.getChildren().add(btnsBox);

    Group dialogGroup = new Group();
    dialogGroup.getChildren().add(mainBox);
    dialog.setScene(new Scene(dialogGroup));
    dialog.initOwner(MainFrame.getScene().getWindow());
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.showAndWait();
    if (port == 0) {
      return;
    }
    try {
      try {
      socket = new Socket("127.0.0.1", port);
      } catch (ConnectException e) {
        createInfoPopup(null, "В соединении отказано");
        port = 0;
        return;
      }
      dOut = new DataOutputStream(socket.getOutputStream());
      dInp = new DataInputStream(socket.getInputStream());
      Connect conData = new Connect(Integer.valueOf(posField.getText()),nameField.getText(),0);
      GMessage conMsg = new GMessage(conData);
      String strConMsg = gson.toJson(conMsg);
      dOut.writeUTF(strConMsg);
      listenThread = new Thread(() -> {
        String strMsg = null;
        boolean flag = true;
        while (flag) {
          try {
             strMsg = dInp.readUTF();
          } catch (IOException e) {
            port = 0;
            flag = false;
            Platform.runLater(() -> {
              createInfoPopup(null, "Соединение с сервером оборвано");
              initialize_start();
            });
          }
          clientMessageHandler.handleGMessage(strMsg, MessageType.GENERIC);
        }
      });
      listenThread.start();
    } catch (IOException e) {
      port = 0;
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ee) {}
      }
      e.printStackTrace();
    }
  }

  public void createInfoPopup(Window window, String text) {
    Stage dialog = new Stage();
    VBox mainBox = new VBox();
    mainBox.setAlignment(Pos.CENTER);
    mainBox.getChildren().add(new Label(text));
    Button okButton = new Button();
    okButton.setText("Закрыть");
    okButton.setOnAction(value -> {
      dialog.close();
    });
    mainBox.getChildren().add(okButton);
    Group dialogGroup = new Group();
    dialogGroup.getChildren().add(mainBox);
    dialog.setScene(new Scene(dialogGroup));
    if (window == null)
      dialog.initOwner(MainFrame.getScene().getWindow());
    else
    dialog.initOwner(window);
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.showAndWait();
  }

    public void CreateLeaderBoard(PlayerWinsArray arr) {
    final double nameColumnWidth = Config.name_max_length * 6;
    Stage dialog = new Stage();
    VBox mainBox = new VBox();
    mainBox.setAlignment(Pos.CENTER);
    HBox header = new HBox();
    header.getChildren().add(new Label("Имя"));
    ((Label)header.getChildren().get(0)).setPrefWidth(nameColumnWidth);
    header.getChildren().add(new Separator(Orientation.VERTICAL));
    header.getChildren().add(new Label("Победы"));
    ((Label)header.getChildren().get(2)).setPrefWidth(nameColumnWidth);
    mainBox.getChildren().add(header);
    mainBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
    for (PlayerWins player : arr.arr) {
      HBox row = new HBox();
      row.getChildren().add(new Label(player.name));
      ((Label)row.getChildren().get(0)).setPrefWidth(nameColumnWidth);
      row.getChildren().add(new Separator(Orientation.VERTICAL));
      row.getChildren().add(new Label("" + player.wins));
      ((Label)row.getChildren().get(2)).setPrefWidth(nameColumnWidth);
      mainBox.getChildren().add(row);
      mainBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
    }
    Button okButton = new Button();
    okButton.setText("Закрыть");
    okButton.setOnAction(value -> {
      dialog.close();
    });
    mainBox.getChildren().add(okButton);
    Group dialogGroup = new Group();
    dialogGroup.getChildren().add(mainBox);
    dialog.setScene(new Scene(dialogGroup));
    dialog.initOwner(MainFrame.getScene().getWindow());
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.showAndWait();
  }

  @FXML
  private void showLeaderBoard() {
    if (listenThread == null || port == 0 || playerWinsArray == null) {
      return;
    }
    Platform.runLater(() -> {
      CreateLeaderBoard(playerWinsArray);
    });
  }

  @FXML
  private void ready() {
    if (isReady) {
      Unready unreadyData =  new Unready(0);
      GMessage unreadyMsg = new GMessage(unreadyData);
      SendMessage(unreadyMsg);
      ReadyBtn.setText("Готов");
    } else {
      ReadyBtn.setText("Не готов");
      Ready readyData =  new Ready(0);
      GMessage readyMsg = new GMessage(readyData);
      SendMessage(readyMsg);
    }
    isReady = !isReady;
  }

  @FXML
  private void shoot() {
    Shoot shootData = new Shoot(0);
    GMessage shootMsg = new GMessage(shootData);
    SendMessage(shootMsg);
  }

  @FXML
  private void pauseGame() {
    if (isPaused) {
      Unpause unpauseData = new Unpause(0);
      GMessage unpauseMsg = new GMessage(unpauseData);
      SendMessage(unpauseMsg);
      PauseBtn.setText("Пауза");
    } else {
      Pause pauseData = new Pause(0);
      GMessage pauseMsg = new GMessage(pauseData);
      SendMessage(pauseMsg);
      PauseBtn.setText("Продолжить");
    }
    isPaused = !isPaused;
  }

  public synchronized void SendMessage(GMessage mes) {
    Gson gson = new Gson();
    try {
      String strMsg = gson.toJson(mes); 
      dOut.writeUTF(strMsg);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void closeSocket() {
    if (socket != null && socket.isConnected()) {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
