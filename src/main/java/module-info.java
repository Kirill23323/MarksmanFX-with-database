module labs.marksman_game {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires javafx.base;
  requires com.google.gson;
  requires org.hibernate.commons.annotations;
  requires org.hibernate.orm.core;
  requires java.persistence;
  requires java.naming;
  requires java.sql;
  requires sqlite.dialect;

  opens labs.marksman_game to javafx.fxml,com.google.gson,org.hibernate.orm.core,org.hibernate.commons.annotations;

  exports labs.marksman_game;
}
