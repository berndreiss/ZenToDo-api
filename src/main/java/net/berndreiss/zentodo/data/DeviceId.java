package net.berndreiss.zentodo.data;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.io.Serializable;
import java.util.Objects;

/**
 * A helper class representing a composite device id.
 */
@Embeddable
public class DeviceId implements Serializable {
    /**
     * The device id
     */
    private int id;

    /**
     * The associated user
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public DeviceId() {
    }

    public DeviceId(int id, User user) {
        this.id = id;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceId that)) return false;

        return id == that.id && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, id);
    }

}
