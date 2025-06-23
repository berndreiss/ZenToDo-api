package net.berndreiss.zentodo.data;

import jakarta.persistence.*;
import net.berndreiss.zentodo.operations.OperationType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Queue item for persisting messages that could not be sent.
 */
@Entity
@Table(name="queue")
public class QueueItem {

    /** The id of the queue item */
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;

    /** The vector clock for the item */
    @Column(columnDefinition = "TEXT")
    private String clock;

    /** The type of operation associated with the item */
    @Column
    private OperationType type;

    /** A list of arguments for the operation */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> arguments;

    /** The timestamp for the item */
    @Column
    private Instant timeStamp;

    /** The user that performed the operation */
    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    public QueueItem() {}

    //Getters and Setters
    public long getId(){return id;}
    public String getClock() {return clock;}
    public void setClock(String clock) {this.clock = clock;}
    public OperationType getType() {return type;}
    public void setType(OperationType type) {this.type = type;}
    public List<String> getArguments() {return arguments;}
    public void setArguments(List<Object> arguments) {
        this.arguments = new ArrayList<>();
        for (Object o : arguments)
            this.arguments.add(o == null ? "" : o.toString());
    }
    public Instant getTimeStamp() {return timeStamp;}
    public void setTimeStamp(Instant timeStamp) {this.timeStamp = timeStamp;}
    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
}
