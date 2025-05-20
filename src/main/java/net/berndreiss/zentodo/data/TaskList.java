package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO DESCRIBE
 */
@Entity
@Table (name = "lists")
public class TaskList {

    @Id
    long id;

    @Column
    private String name;

    @Column
    private String color;

    @ManyToMany(mappedBy = "lists")
    List<Profile> profiles = new ArrayList<>();

    public TaskList() {
    }

    public TaskList(long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }
}
