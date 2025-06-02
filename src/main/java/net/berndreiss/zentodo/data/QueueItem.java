package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import net.berndreiss.zentodo.OperationType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="queue")
public class QueueItem {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String clock;

    @Column
    private OperationType type;

    @Column
    private List<String> arguments;

    @Column
    private Instant timeStamp;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    public long getId(){return id;}


    public String getClock() {return clock;}

    public void setClock(String clock) {this.clock = clock;}

    public QueueItem() {
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {

        this.arguments = new ArrayList<>();

        for (Object o : arguments)
            this.arguments.add(o.toString());
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Instant timeStamp) {
        this.timeStamp = timeStamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
