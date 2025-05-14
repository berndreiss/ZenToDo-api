package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO DESCRIBE
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String userName = null;

    @Column
    private boolean enabled = false;

    @Column (nullable = false)
    private long device;

    @Column (nullable = false)
    private int profile;

    @Column
    private String clock;

    @Column
    private String password;

    @OneToMany (mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true,  fetch = FetchType.EAGER)
    private List<Device> devices = new ArrayList<>();

    @OneToMany (mappedBy = "profileId.user", cascade = CascadeType.REMOVE, orphanRemoval = true,  fetch = FetchType.EAGER)
    private List<Profile> profiles = new ArrayList<>();

    @OneToMany (mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true,  fetch = FetchType.EAGER)
    private List<QueueItem> queueItems = new ArrayList<>();

    public User(){}

    public User(String email){
        this.email = email;
    }

    public User(String email, String userName){
        this.email = email;
        this.userName = userName;
    }

    public User(String email, long device){
        this.email = email;
        this.device = device;
    }
    public User(String email, String username, long device){
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

    public boolean getEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    public long getDevice(){return device;}

    public void setDevice(long device){this.device = device;}

    public int getProfile(){return profile;}

    public void setProfile(int profile){this.profile = profile;}

    public boolean isEnabled() {return enabled;}

    public String getClock() {return clock;}

    public void setClock(String clock) {this.clock = clock;}

    public String getPassword() {return password;}

    public void setPassword(String password) {this.password = password;}

    public List<Device> getDevices() {return devices;}

    public void setDevices(List<Device> devices) {this.devices = devices;}

    public List<Profile> getProfiles() {return profiles;}

    public void setProfiles(List<Profile> profiles) {this.profiles = profiles;}

    public List<QueueItem> getQueueItems() {return queueItems;}

    public void setQueueItems(List<QueueItem> queueItems) {this.queueItems = queueItems;}
}
