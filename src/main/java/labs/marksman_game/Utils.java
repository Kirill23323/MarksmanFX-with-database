package labs.marksman_game;

public class Utils {
    
    public static class ArrowState {
        public boolean visible;
        public double posX;
    
        ArrowState() {
          visible = false;
          posX = 0.0;
        }
    
        ArrowState(boolean visible, double pos) {
          this.visible = visible;
          this.posX = pos;
        }
    }

    public static class ArrowStateArray {
        public ArrowState[] arr;
    
        ArrowStateArray(int n) {
          arr = new ArrowState[n];
          for (int i = 0; i < n; i++) {
            arr[i] = new ArrowState();
          }
        }
    }

    public static class PlayerWins {
      int wins;
      String name;

      public PlayerWins(){}

      public PlayerWins(int wins,String name) {
        this.wins = wins;
        this.name = name;
      }

      PlayerWins(PlayerTable player) {
        this.wins = player.wins;
        this.name = player.name;
      }
    }

    public static class PlayerWinsArray {

      PlayerWins [] arr;

      PlayerWinsArray(int n){
        arr = new PlayerWins[n];
        for(int i = 0;i < n;i++) {
          arr[i] = new PlayerWins();
        }
      }
    }
}
