package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a task list.
 */
@Entity
@Table(name = "lists")
public class TaskList {

    /**
     * The (globally unique) id of the list
     */
    @Id
    long id;
    /**
     * The profiles associated with the list
     */
    @ManyToMany(mappedBy = "lists")
    List<Profile> profiles = new ArrayList<>();
    /**
     * The name of the list
     */
    @Column(nullable = false)
    private String name;
    /**
     * The optional color of the list
     */
    @Column
    private String color;

    public TaskList() {
    }

    public TaskList(long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    //Getters and Setters
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }
}
