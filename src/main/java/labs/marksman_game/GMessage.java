package labs.marksman_game;

import com.google.gson.Gson;

public class GMessage {
    
    protected MessageType GMsgType;
    protected Connect connectData = null;
    protected Reject rejectData = null;
    protected Exit exitData = null;
    protected Ready readyData = null;
    protected Unready unreadyData = null;
    protected GameBegin gameBeginData = null;
    protected Sync syncData = null;
    protected Shoot shootData = null;
    protected ScoreSync scoreSyncData = null;
    protected PlayerWon playerWonData = null;
    protected Pause pauseData = null;
    protected Unpause unpauseData = null;
    protected LeaderBoardSend leaderBoardSendData = null;

    public Connect getConnectData() {
        return connectData;
    }

    public Reject getRejectData() {
        return rejectData;
    }

    public Exit getExitData() {
        return exitData;
    }

    public Ready getReadyData() {
        return readyData;
    }

    public Unready getUnreadyData() {
        return unreadyData;
    }

    public GameBegin getGameBeginData() {
        return gameBeginData;
    }

    public Sync getSyncData() {
        return syncData;
    }

    public Shoot getShootData() {
        return shootData;
    }

    public ScoreSync getScoreSyncData() {
        return scoreSyncData;
    }

    public PlayerWon getPlayerWonData() {
        return playerWonData;
    }

    public Pause getPauseData() {
        return pauseData;
    }

    public Unpause getUnpauseData() {
        return unpauseData;
    }

    
    public LeaderBoardSend getLeaderBoardSendData(){
        return leaderBoardSendData;
    }

    GMessage(Connect connectData){
        this.GMsgType = MessageType.CONNECT;
        this.connectData = connectData;
    }
    GMessage(Reject rejectData){
        this.GMsgType = MessageType.REJECT;
        this.rejectData = rejectData;
    }

    GMessage(GameBegin gameBeginData){
        this.GMsgType = MessageType.GAME_BEGIN;
    }

    GMessage(Exit exitData){
        this.GMsgType = MessageType.EXIT;
        this.exitData = exitData;
    }
    GMessage(Ready readyData){
        this.GMsgType = MessageType.READY;
        this.readyData = readyData;
    }
    GMessage(Unready unreadyData){
        this.GMsgType = MessageType.UNREADY;
        this.unreadyData = unreadyData;
    }
    GMessage(Sync syncData){
        this.GMsgType = MessageType.SYNC;
        this.syncData = syncData;
    }
    GMessage(Shoot shootData){
        this.GMsgType = MessageType.SHOOT;
        this.shootData = shootData;
    }
    GMessage(ScoreSync scoreSyncData){
        this.GMsgType = MessageType.SCORE_SYNC;
        this.scoreSyncData = scoreSyncData;
    }
    GMessage(PlayerWon playerWonData){
        this.GMsgType = MessageType.PLAYER_WON;
        this.playerWonData = playerWonData;
    }
    GMessage(Pause pauseData){
        this.GMsgType = MessageType.PAUSE;
        this.pauseData = pauseData;
    }
    GMessage(Unpause unpauseData){
        this.GMsgType = MessageType.UNPAUSE;
        this.unpauseData = unpauseData;
    }
    
    GMessage(LeaderBoardSend leaderBoardSendData){
        this.GMsgType = MessageType.LEADER_BOARD_SEND;
        this.leaderBoardSendData = leaderBoardSendData;
    }

    public static class GMessageHandler{
        Gson gson = new Gson();
        public GMessage handleGMessage(String mes, MessageType expect){

            GMessage newMes = gson.fromJson(mes,GMessage.class);
            switch (newMes.GMsgType) {
                case CONNECT:
                    if(expect == MessageType.CONNECT || expect == MessageType.GENERIC){
                        return HandleConnect(newMes);
                    }
                    break;
                case REJECT:
                    if(expect == MessageType.REJECT || expect == MessageType.GENERIC){
                        return HandleReject(newMes);
                    }
                    break;
                case READY:
                    if(expect == MessageType.READY || expect == MessageType.GENERIC){
                        return HandleReady(newMes);
                    }
                    break;
                case UNREADY:
                    if(expect == MessageType.UNREADY || expect == MessageType.GENERIC){
                        return HandleUnready(newMes);
                    }
                    break;
                case GAME_BEGIN:
                    if(expect == MessageType.GAME_BEGIN || expect == MessageType.GENERIC){
                        return HandleGameBegin(newMes);
                    }
                    break;
                case SYNC:
                    if(expect == MessageType.SYNC || expect == MessageType.GENERIC){
                        return HandleSync(newMes);
                    }
                    break;
                case SHOOT:
                    if(expect == MessageType.SHOOT || expect == MessageType.GENERIC){
                        return HandleShoot(newMes);
                    }
                    break;
                case SCORE_SYNC:
                    if(expect == MessageType.SCORE_SYNC || expect == MessageType.GENERIC){
                        return HandleScoreSync(newMes);
                    }
                    break;
                case PLAYER_WON:
                    if(expect == MessageType.PLAYER_WON || expect == MessageType.GENERIC){
                        return HandlePlayerWon(newMes);
                    }
                    break;
                case PAUSE:
                    if(expect == MessageType.PAUSE || expect == MessageType.GENERIC){
                        return HandlePause(newMes);
                    }
                    break;
                case UNPAUSE:
                    if(expect == MessageType.UNPAUSE || expect == MessageType.GENERIC){
                        return HandleUnpause(newMes);
                    }
                    break;
                case EXIT:
                    if(expect == MessageType.EXIT || expect == MessageType.GENERIC){
                        return HandleExit(newMes);
                    }
                    break;
                case LEADER_BOARD_SEND:
                    if(expect == MessageType.LEADER_BOARD_SEND || expect == MessageType.GENERIC){
                        return HandleLeaderBoardSend(newMes);
                    }
                    break;
                default:
                    break;
                } 

            return null;

        }

        public synchronized GMessage HandleConnect(GMessage mes) { return null; }
        public GMessage HandleReject(GMessage mes) { return null; }
        public GMessage HandleExit(GMessage mes) { return null; }
        public GMessage HandleReady(GMessage mes) { return null; }
        public GMessage HandleUnready(GMessage mes) { return null; }
        public GMessage HandleGameBegin(GMessage mes) { return null; }
        public GMessage HandleSync(GMessage mes) { return null; }
        public GMessage HandleShoot(GMessage mes) { return null; }
        public GMessage HandleScoreSync(GMessage mes) { return null; }
        public GMessage HandlePlayerWon(GMessage mes) { return null; }
        public GMessage HandlePause(GMessage mes) { return null; }
        public GMessage HandleUnpause(GMessage mes) { return null; }
        public GMessage HandleLeaderBoardSend(GMessage mes) { return null; }

    }

    public static class Connect{
        protected int slot;
        protected int wins;
        protected String name;

        Connect(int slot,String name,int wins){
            this.slot = slot;
            this.name = name;
            this.wins = wins;
        }
    }
    public static class Reject{
        protected ReasonType reason;

        Reject(ReasonType reason){
            this.reason = reason;
        }
    }
    public static class Exit{
        protected int slot;

        Exit(int slot){
            this.slot = slot;
        }
    }
    public static class Ready{
        protected int slot;

        Ready(int slot){
            this.slot = slot;
        }
        
    }
    public static class Unready{
        protected int slot;

        Unready(int slot){
            this.slot = slot;
        }
    }
    public static class GameBegin{
        
        GameBegin(){}
    }
    public static class Sync{
        protected Utils.ArrowStateArray arrows;
        protected double target1PosY;
        protected double target2PosY;

        Sync(Utils.ArrowStateArray arrows, double target1Pos, double target2Pos) {
        this.arrows = arrows;
        this.target1PosY = target1Pos;
        this.target2PosY = target2Pos;

        }

    }
    public static class Shoot{
        protected int slot;
        
        Shoot(int slot){
            this.slot = slot;
        }
        
    }
    public static class ScoreSync{
        protected int slot;
        protected int score;

        ScoreSync(int slot,int score){
            this.slot = slot;
            this.score = score;
        }
    }
    public static class PlayerWon{
        protected int slot;

        PlayerWon(int slot){
            this.slot = slot;
        }
        
    }
    public static class Pause{
        protected int slot;

        Pause(int slot){
            this.slot = slot;
        }
        
    }
    public static class Unpause{
        protected int slot;

        Unpause(int slot){
            this.slot = slot;
        }
        
    }
    public static class LeaderBoardSend{
        Utils.PlayerWinsArray arr;

        LeaderBoardSend(Utils.PlayerWinsArray arr){
            this.arr = arr;
        }

    }

}
