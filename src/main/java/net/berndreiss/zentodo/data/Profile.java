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


    public int getId(){
        return profileId.getId();
    }

    public User getUser(){
        return profileId.getUser();
    }
    public ProfileId getProfileId(){
        return profileId;
    }

    public void setProfileId(ProfileId profileId){
        this.profileId = profileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String  name) {
        this.name = name;
    }
}
