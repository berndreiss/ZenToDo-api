package net.berndreiss.zentodo.data;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProfileId implements Serializable {
    private int id;
    private long userId;
    public ProfileId() {}
    public ProfileId(int id, long userId){
        this.id = id;
        this.userId = userId;
    }
    public int getId(){return id;}
    public long getUserId(){return userId;}
    public void setId(int id){this.id = id;}
    public void setUserId(long userId){this.userId = userId;}

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof ProfileId)) return false;

        ProfileId that = (ProfileId) o;
        return id == that.id && userId == that.userId;
    }
    @Override
    public int hashCode(){
        return Objects.hash(userId, id);
    }

}
