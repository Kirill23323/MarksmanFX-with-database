package labs.marksman_game;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class DBManager {
    private SessionFactory session;

  public void connect() {
    session = HibernateSessionFactoryUtil.getSessionFactory();
    session.openSession().close();
  }

  public void RegisterPlayer(String name) {
    Session s = session.openSession();
    Transaction t = s.beginTransaction();
    try {
      s.save(new PlayerTable(name));
      t.commit();
      s.close();
    } catch (Exception e) {
      t.rollback();
    } finally {
      s.close();
    }
  }

  public int GetPlayerWins(String name) {
    Session s = session.openSession();
    Query<PlayerTable> q = s.createQuery("FROM labs.marksman_game.PlayerTable WHERE name LIKE :name");
    q.setParameter("name", name);
    List<PlayerTable> players = (List<PlayerTable>)q.list();
    s.close();
    return players.get(0).wins;
  }

  public Utils.PlayerWinsArray GetLeaderBoard() {
    Utils.PlayerWinsArray arr = null;
    List<PlayerTable> players = (List<PlayerTable>)session.openSession().createQuery("FROM labs.marksman_game.PlayerTable").list();
    int sz = players.size();
    arr = new Utils.PlayerWinsArray(sz);
    int i = 0;
    for (PlayerTable p : players) {
      arr.arr[i] = new Utils.PlayerWins(p);
      i++;
    }
    return arr;
  }

  public void IncrementPlayerWins(String name) {
    Session s = session.openSession();
    Query<PlayerTable> q = s.createQuery("FROM labs.marksman_game.PlayerTable WHERE name LIKE :name");
    q.setParameter("name", name);
    List<PlayerTable> players = (List<PlayerTable>)q.list();
    PlayerTable p = players.get(0);
    p.wins++;
    Transaction t = s.beginTransaction();
    s.update(p);
    t.commit();
    s.close();
  }
    
}
