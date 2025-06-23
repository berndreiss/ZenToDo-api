package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a user profile.
 */
@Entity
@Table (name="profiles")
public class Profile {

    /** The composite key containing the user and the profile id */
    @EmbeddedId
    private ProfileId profileId;

    /** An optional name */
    @Column
    private String name = "Default";

    /** Lists associated with the profile */
    @ManyToMany
    @JoinTable(
        name = "profile_list",
        joinColumns = {
        @JoinColumn(name = "profile_id", referencedColumnName = "id"),
        @JoinColumn(name = "profile_user_id", referencedColumnName = "user_id")
        },
            inverseJoinColumns = @JoinColumn (name = "list_id")

    )
    List<TaskList> lists = new ArrayList<>();

    public Profile(){}

    //Getters and Setters
    public int getId(){return profileId.getId();}
    public long getUserId(){return profileId.getUser().getId();}
    public User getUser(){return profileId.getUser();}
    public ProfileId getProfileId(){return profileId;}
    public void setProfileId(ProfileId profileId){this.profileId = profileId;}
    public String getName() {return name;}
    public void setName(String  name) {this.name = name;}
    public List<TaskList> getLists(){return lists;}
}
