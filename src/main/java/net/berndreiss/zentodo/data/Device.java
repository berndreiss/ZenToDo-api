package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Class representing a device.
 */
@Entity
@Table (name="devices")
public class Device {

    //TODO add some form of fingerprint to recognize same devices?
    /** The id for the device */
    @EmbeddedId
    private DeviceId deviceId;

    /** Date when device will expire. */
    @Column
    private Instant expiration;

    public Device() {}

    //Getters and Setters
    public int getId() {return deviceId.getId();}
    public void setId(int id) {this.deviceId.setId(id);}
    public DeviceId getDeviceId() {return this.deviceId;}
    public void setDeviceId(DeviceId deviceId){this.deviceId = deviceId;}
    public Instant getExpiration() {return expiration;}
    public void setExpiration(Instant expiration) {this.expiration = expiration;}
    public User getUser() {return deviceId.getUser();}
    public void setUser(User user) {this.deviceId.setUser(user);}
}
