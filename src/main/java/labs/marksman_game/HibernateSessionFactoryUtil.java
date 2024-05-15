package labs.marksman_game;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateSessionFactoryUtil {

  private static SessionFactory sessionFactory = null;

  private HibernateSessionFactoryUtil() {}

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        Configuration cfg = new Configuration().configure();
        cfg.addAnnotatedClass(PlayerTable.class);
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties());
        sessionFactory = cfg.buildSessionFactory(builder.build());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return sessionFactory;
  }

}
