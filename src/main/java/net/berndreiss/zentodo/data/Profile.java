package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table (name="profiles")
public class Profile {

    @EmbeddedId
    private ProfileId profileId;

    @Column
    private String name = "Default";

    public Profile(){}

    public Profile(int id, String name, Long user) {
        this.profileId = new ProfileId(id, user == null ? -1 : user);
        this.name = Objects.requireNonNullElseGet(name, () -> "Profile " + id);
    }

    public int getId() {
        return profileId.getId();
    }

    public void setId(int id) {
    }

    public Long getUser() {
        //return profileId.getUserId() == -1 ? null : profileId.getUserId();
        return profileId.getUserId();
    }

    public String getName() {
        return name;
    }

    public void setName(String  name) {
        this.name = name;
    }
}
