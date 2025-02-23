package net.berndreiss.zentodo.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TODO DESCRIBE
 */
@Entity
@Table (name = "lists")
public class TaskList {

    @Id
    @Column (nullable = false)
    private String list;

    @Column (nullable = false)
    private String color ;

    public TaskList(){};
    public TaskList(String color){
        this.color = color;
    }

    public String getColor(){return color;}

    public void setColor(String color){
        this.color = color;
    }

}
