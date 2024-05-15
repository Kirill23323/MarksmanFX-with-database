package labs.marksman_game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.google.gson.Gson;

import labs.marksman_game.GMessage.Connect;
import labs.marksman_game.GMessage.Exit;
import labs.marksman_game.GMessage.GMessageHandler;
import labs.marksman_game.GMessage.GameBegin;
import labs.marksman_game.GMessage.LeaderBoardSend;
import labs.marksman_game.GMessage.PlayerWon;
import labs.marksman_game.GMessage.Reject;
import labs.marksman_game.GMessage.ScoreSync;

public class MyServer {
  private double[][] arrowsPos;
  private double[] target1Pos;
  private double[] target1PosStart;
  private double[] target1PosEnd;
  private double[] target2Pos;
  private double[] target2PosStart;
  private double[] target2PosEnd;
  private double gamePaneWidth;

  private GMessage.Sync sync;
  private DBManager db;

  public boolean gameIsGoing = false;

  public boolean gameIsPaused = false;

  private ServerSocket serverSocket;

  public ServerMessageHandler messageHandler = new ServerMessageHandler(this);
  public ClientHandler[] clients = new ClientHandler[] {null, null, null, null};
  public Thread gameThread;

  private static class ServerMessageHandler extends GMessageHandler {
    private MyServer server;

    ServerMessageHandler(MyServer server) {
      this.server = server;
    }

    @Override
    public synchronized GMessage HandleConnect(GMessage mes){
      if (server.gameIsGoing) {
        Reject rej = new Reject(ReasonType.GAME_GOING);
        GMessage rejMsg = new GMessage(rej);
        return rejMsg;
      }
      GMessage.Connect conData = mes.getConnectData();
      String name = conData.name;
      int slotsLeft = server.clients.length;
      int freeSlot = 0;
      boolean freeSlotChosen = false;
      for (int i = 0; i < server.clients.length; i++) {
        if (server.clients[i] != null) {
          slotsLeft--;
          if (server.clients[i].name.equals(name)) {
            Reject rej = new Reject(ReasonType.NAME_EXIST);
            GMessage rejMsg = new GMessage(rej);
            return rejMsg;
          }
        } else {
          if (!freeSlotChosen) {
            freeSlot = i;
          }
          if (freeSlot >= conData.slot) {
            freeSlotChosen = true;
          }
        }
      }
      if (slotsLeft == 0) {
        Reject rej = new Reject(ReasonType.GAME_FULL);
        GMessage rejMsg = new GMessage(rej);
        return rejMsg;
      }
      server.db.RegisterPlayer(name);
      int wins = server.db.GetPlayerWins(name);
      Connect con = new Connect(freeSlot, name,wins);
      GMessage conMsg = new GMessage(con);
      return conMsg;
    }

    @Override
    public GMessage HandleExit(GMessage mes) {
      GMessage.Exit exitData = mes.getExitData();
      server.deletePlayer(exitData.slot);
      return null;
    }

    private synchronized void SetReadyUnready(int slot, boolean ready, GMessage msg) {
      if (!server.gameIsGoing) {
        server.clients[slot].ready = ready;
        server.SendToAllPlayers(msg);
        server.TryStartGame();
      }
    }

    @Override
    public GMessage HandleReady(GMessage mes) {
      GMessage.Ready readyData = mes.getReadyData();
      SetReadyUnready(readyData.slot, true, mes);
      return null;
    }

    @Override
    public GMessage HandleUnready(GMessage mes) {
      GMessage.Unready unreadyData = mes.getUnreadyData();
      SetReadyUnready(unreadyData.slot, false, mes);
      return null;
    }

    @Override
    public GMessage HandleShoot(GMessage mes) {
      if (server.gameIsGoing && !server.gameIsPaused) {
        GMessage.Shoot shootData = mes.getShootData();
        server.clients[shootData.slot].shootingState = true;
        server.SendToAllPlayers(mes);
      }
      return null;
    }

    private synchronized void SetPauseUnpause(int slot, boolean action, GMessage mes) {
      if (server.gameIsGoing) {
        server.clients[slot].pause = action;
        server.SendToAllPlayers(mes);
        boolean flag = true;
        for (int i = 0; i < server.clients.length; i++) {
          if (server.clients[i] != null && server.clients[i].pause != false) {
            server.gameIsPaused = true;
            flag = false;
            break;
          }
        }
        if (flag && server.gameIsPaused) {
          server.gameIsPaused = false;
          synchronized (server) {
            server.notify();
          }
        }
      }
    }

    @Override
    public GMessage HandlePause(GMessage mes) {
      GMessage.Pause pauseData = mes.getPauseData();
      SetPauseUnpause(pauseData.slot, true, mes);
      return null;
    }

    @Override
    public GMessage HandleUnpause(GMessage mes) {
      GMessage.Unpause unpauseData = mes.getUnpauseData();
      SetPauseUnpause(unpauseData.slot, false, mes);
      return null;
    }

  }

  private static class ClientHandler extends Thread {
    public Socket clientSocket;
    private MyServer server;
    private String name;
    private int score = 0;
    private int slot = 0;
    private int wins = 0;
    private boolean ready = false;
    private boolean pause = false;
    public boolean shootingState = false;
    public DataOutputStream dOut;
    public DataInputStream dInp;

    ClientHandler(Socket socket, MyServer s) {
      clientSocket = socket;
      server = s;
    }
 
    public synchronized void SendMessage(GMessage mes) {
      try {
        Gson gson = new Gson();
        String gMsg = gson.toJson(mes);
        dOut.writeUTF(gMsg);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void run() {
      try {
        Gson gson = new Gson();
        dOut = new DataOutputStream(clientSocket.getOutputStream());
        dInp = new DataInputStream(clientSocket.getInputStream());
        String strMsg;
        strMsg = dInp.readUTF();
        GMessage response = server.messageHandler.handleGMessage(strMsg,MessageType.CONNECT);
        if (response == null || response.GMsgType == MessageType.REJECT) {
          if (response != null){
            String newStrMsg = gson.toJson(response);
            dOut.writeUTF(newStrMsg);
          }
          clientSocket.close();
          return;
        }
        GMessage.Connect clientInfo = response.getConnectData();
        slot = clientInfo.slot;
        name = clientInfo.name;
        wins = clientInfo.wins;
        server.addPlayer(slot, this);
        LeaderBoardSend leaderBoardSendData = new LeaderBoardSend(server.db.GetLeaderBoard());
        GMessage leaderBoadSendMsg = new GMessage(leaderBoardSendData);
        server.SendToAllPlayers(leaderBoadSendMsg);
        boolean flag = true;
        while (flag) {
          try {
            strMsg = dInp.readUTF();
            if (strMsg == null) {
              flag = false;
            }
          } catch (IOException e) {
            e.printStackTrace();
            flag = false;
          }
          String updateMsg = MyServer.ResolveSlot(strMsg, slot);
          server.messageHandler.handleGMessage(updateMsg,MessageType.GENERIC);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      server.deletePlayer(slot);
    }
  }

  public void initialize() throws IOException {
    javafx.application.Application.launch(FakeServerApp.class);
    arrowsPos = FakeServerApp.controller.arrowsPos;
    target1Pos = FakeServerApp.controller.target1Pos;
    target1PosStart = FakeServerApp.controller.target1PosStart;
    target1PosEnd = FakeServerApp.controller.target1PosEnd;
    target2Pos = FakeServerApp.controller.target2Pos;
    target2PosStart = FakeServerApp.controller.target2PosStart;
    target2PosEnd = FakeServerApp.controller.target2PosEnd;
    gamePaneWidth = FakeServerApp.controller.gamePaneWidth;
    resetSync();
    serverSocket = new ServerSocket(Config.port);
    db = new DBManager();
    db.connect();
    while (true) {
      new ClientHandler(serverSocket.accept(), this).start();
    }
  }
 
  public void resetSync() {
    sync = new GMessage.Sync(new Utils.ArrowStateArray(clients.length), target1Pos[1], target2Pos[1]);
    for (int i = 0; i < clients.length; i++) {
      sync.arrows.arr[i].posX = arrowsPos[i][0];
    }
  }

  public synchronized void addPlayer(int slot, ClientHandler handler) {
    try {
      clients[slot] = handler;
      DataOutputStream dOut = handler.dOut;
      Gson gson = new Gson();
      for (int i = 0; i < clients.length; i++) {
        if (clients[i] != null) {
          if (i != slot) {
            GMessage.Connect conData = new Connect(handler.slot,handler.name,handler.wins);
            GMessage conMes = new GMessage(conData);
            SendMessage(i, conMes);
          }
          GMessage.Connect conData = new Connect(clients[i].slot,clients[i].name,clients[i].wins);
          GMessage conMes = new GMessage(conData);
          String strMes = gson.toJson(conMes);
          dOut.writeUTF(strMes);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void deletePlayer(int slot) {
    if (clients[slot] != null) {
      try {
        clients[slot].clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      clients[slot] = null;
      Exit exit = new Exit(slot);
      GMessage exitMsg = new GMessage(exit);
      SendToAllPlayers(exitMsg);
    }
  }

  public void SendToAllPlayers(GMessage mes) {
    for (int i = 0; i < clients.length; i++) {
      if (clients[i] != null) {
        clients[i].SendMessage(mes);
      }
    }
  }


  public boolean arePlayersLeft() {
    for (int i = 0; i < clients.length; i++) {
      if (clients[i] != null) {
        return true;
      }
    }
    return false;
  }

  static public final String ResolveSlot(String mes, int slot) {
    Gson gson = new Gson();
    GMessage oldMsg = gson.fromJson(mes, GMessage.class);
    String newMsg = null;
    switch (oldMsg.GMsgType) {
      case MessageType.EXIT:
        oldMsg.getExitData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.READY:
        oldMsg.getReadyData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.UNREADY:
        oldMsg.getUnreadyData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.SHOOT:
        oldMsg.getShootData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.SCORE_SYNC:
        oldMsg.getScoreSyncData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.PLAYER_WON:
        oldMsg.getPlayerWonData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.PAUSE:
        oldMsg.getPauseData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      case MessageType.UNPAUSE:
        oldMsg.getUnpauseData().slot = slot;
        newMsg = gson.toJson(oldMsg);
        return newMsg;
      default:
        return mes;
      }
  }

  public synchronized void PlayerWon(int slot) {
    if (gameIsGoing) {
      resetSync();
      GMessage syncMsg = new GMessage(sync);
      SendToAllPlayers(syncMsg);
      gameIsGoing = false;
      gameIsPaused = false;
      PlayerWon playerWonData = new PlayerWon(slot);
      GMessage playerWonMsg = new GMessage(playerWonData);
      SendToAllPlayers(playerWonMsg);
      if (clients[slot] != null) {
        db.IncrementPlayerWins(clients[slot].name);
        clients[slot].wins++;
      }
      LeaderBoardSend leaderBoardSendData = new LeaderBoardSend(db.GetLeaderBoard());
      GMessage leaderBoadSendMsg = new GMessage(leaderBoardSendData);
      SendToAllPlayers(leaderBoadSendMsg);
      for (int i = 0; i < clients.length; i++) {
        if (clients[i] != null) {
          clients[i].ready = false;
          clients[i].pause = false;
          clients[i].score = 0;
        }
      }
    }
  }

  public void SendMessage(int slot, GMessage mes) {
    clients[slot].SendMessage(mes);
  }

  public synchronized void TryStartGame() {
    if (gameIsGoing) {
      return;
    }
    boolean flag = true;
    for (int i = 0; i < clients.length; i++) {
      if(clients[i] != null){
        if(clients[i].ready == false){
          flag = false;
          break;
        }
      }
    }
    if (!flag) {
      return;
    }
    gameIsGoing = true;
    gameThread = new Thread(() -> {
      while(gameIsGoing) {
        if (gameIsPaused) {
          try {
            synchronized(this) {
              this.wait();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        for (int i = 0; i < clients.length; i++) {
          if (clients[i] != null && clients[i].shootingState) {
            sync.arrows.arr[i].visible = true;
            double dx = sync.arrows.arr[i].posX - target1Pos[0];
            double dy = arrowsPos[i][1] - sync.target1PosY;
            int points = -1;
            if (Math.sqrt(dx*dx + dy*dy) <= Config.arrow_hitbox_radius + Config.target_radius) {
              points = 1;
            }
            dx = sync.arrows.arr[i].posX - target2Pos[0];
            dy = arrowsPos[i][1] - sync.target2PosY;
            if (points == -1 && (Math.sqrt(dx*dx + dy*dy) <= Config.arrow_hitbox_radius + Config.target_radius / 2)) {
              points = 2;
            }
            if (points == -1 && (sync.arrows.arr[i].posX + Config.arrow_hitbox_radius > gamePaneWidth)) {
              points = 0;
            }
            switch (points) {
              case -1:
                sync.arrows.arr[i].posX += Config.arrow_speed;
                break;
              case 0:
                sync.arrows.arr[i].posX = arrowsPos[i][0];
                sync.arrows.arr[i].visible = false;
                clients[i].shootingState = false;
                break;
              default:
                sync.arrows.arr[i].posX = arrowsPos[i][0];
                sync.arrows.arr[i].visible = false;
                clients[i].score += points;
                clients[i].shootingState = false;
                ScoreSync scoreSyncData = new ScoreSync(i,clients[i].score);
                GMessage scoreSyncMsg = new GMessage(scoreSyncData);
                SendToAllPlayers(scoreSyncMsg);
                if (clients[i].score >= Config.final_score) {
                  PlayerWon(i);
                  return;
                }
                break;
            }
          }
        }
        sync.target1PosY += Config.target_speed;
        if (sync.target1PosY > target1PosEnd[1] - Config.target_radius) {
          sync.target1PosY = target1PosStart[1] + Config.target_radius;
        }
        sync.target2PosY += Config.target_speed * 2;
        if (sync.target2PosY > target2PosEnd[1] - Config.target_radius / 2) {
          sync.target2PosY = target2PosStart[1] + Config.target_radius / 2;
        }
        GMessage syncMsg = new GMessage(sync);
        SendToAllPlayers(syncMsg);
        try {
          Thread.sleep(Config.sleep_time);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        gameIsGoing = arePlayersLeft();
      }
      resetSync();
    });
    GameBegin gameBeginData = new GameBegin();
    GMessage gameBeginMsg = new GMessage(gameBeginData);
    SendToAllPlayers(gameBeginMsg);
    gameThread.start();
  }

  public static void main(String[] args) {
    MyServer server = new MyServer();
    try {
      server.initialize();
      server.serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}