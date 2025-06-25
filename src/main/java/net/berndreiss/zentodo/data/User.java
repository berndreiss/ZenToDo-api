package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a user.
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * The user id
     */
    @Id
    private Long id;

    /**
     * The users email address
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * The username
     */
    @Column
    private String userName = null;

    /**
     * True if the user has been enabled (they confirmed their email address)
     */
    @Column
    private boolean enabled = false;

    /**
     * Client side: the device assigned by the server.
     * Server side: the last active device.
     */
    @Column
    private Integer device;

    /**
     * The active profile
     */
    @Column(nullable = false)
    private int profile;

    /**
     * The current vector clock
     */
    @Column(columnDefinition = "TEXT")
    private String clock;

    /**
     * The password hash
     */
    @Column
    private String passwordHash;

    /**
     * The devices for the user
     */
    @OneToMany(mappedBy = "deviceId.user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Device> devices = new ArrayList<>();

    /**
     * The profiles for the user
     */
    @OneToMany(mappedBy = "profileId.user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Profile> profiles = new ArrayList<>();

    /**
     * The queue items for the user
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QueueItem> queueItems = new ArrayList<>();

    public User() {
    }

    public User(String email) {
        this.email = email;
    }

    public User(String email, String userName) {
        this.email = email;
        this.userName = userName;
    }

    public User(String email, Integer device) {
        this.email = email;
        this.device = device;
    }

    public User(String email, String username, Integer device) {
        this.email = email;
        this.userName = username;
        this.device = device;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public Integer getDevice() {
        return device;
    }

    public void setDevice(Integer device) {
        this.device = device;
    }

    public int getProfile() {
        return profile;
    }

    public void setProfile(int profile) {
        this.profile = profile;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClock() {
        return clock;
    }

    public void setClock(String clock) {
        this.clock = clock;
    }

    public String getPasswordHash(){return passwordHash;}
    public void setPasswordHash(String passwordHash){this.passwordHash = passwordHash;}

    //TODO do we want to solve this via the java class?
    //public List<Device> getDevices() {return devices;}
    //public void setDevices(List<Device> devices) {this.devices = devices;}
    //public List<Profile> getProfiles() {return profiles;}
    //public void setProfiles(List<Profile> profiles) {this.profiles = profiles;}
    public List<QueueItem> getQueueItems() {return queueItems;}
    //public void setQueueItems(List<QueueItem> queueItems) {this.queueItems = queueItems;}
}
