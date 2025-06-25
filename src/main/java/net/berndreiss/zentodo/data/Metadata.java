package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A class representing metadata.
 */
@Entity
@Table(name = "metadata")
public class Metadata {

    @Id
    @GeneratedValue
    private int id;

    @Column
    private long lastUser;

    @Column
    private long timeDelay;

    @Column
    private String version;

    public Metadata(){}


    public long getLastUser() {
        return lastUser;
    }

    public void setLastUser(long lastUser) {
        this.lastUser = lastUser;
    }

    public long getTimeDelay() {
        return timeDelay;
    }

    public void setTimeDelay(long timeDelay) {
        this.timeDelay = timeDelay;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
