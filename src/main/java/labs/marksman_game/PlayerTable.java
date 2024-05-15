package labs.marksman_game;

import javax.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table (name = "Players")
public class PlayerTable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int id;

    @Column(name = "name", unique = true)
    public String name;

    @Column(name = "wins")
    @ColumnDefault("0")
    public int wins = 0;

    public PlayerTable() {}

    public PlayerTable(String name) {
        this.name = name;
    }
}
