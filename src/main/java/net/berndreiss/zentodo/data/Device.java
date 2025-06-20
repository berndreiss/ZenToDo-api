package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table (name="devices")
public class Device {

    @EmbeddedId
    private DeviceId deviceId;

    @Column
    private Instant expiration;

    public Device() {
    }

    public int getId() {
        return deviceId.getId();
    }

    public void setId(int id) {
        this.deviceId.setId(id);
    }

    public DeviceId getDeviceId() {return this.deviceId;}
    public void setDeviceId(DeviceId deviceId){this.deviceId = deviceId;}

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public User getUser() {
        return deviceId.getUser();
    }

    public void setUser(User user) {
        this.deviceId.setUser(user);
    }
}
