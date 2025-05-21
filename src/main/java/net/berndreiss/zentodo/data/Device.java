package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table (name="devices")
public class Device {

    @Id
    @Column (nullable = false)
    private int id;

    @Column
    private Instant expiration;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    public Device() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
