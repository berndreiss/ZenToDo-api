package net.berndreiss.zentodo.api;

/**
 * TODO DESCRIBE
 */
public class TaskList {

    private String color;

    public TaskList(String color){
        this.color = color;
    }

    public String getColor(){return color;}

    public void setColor(String color){
        this.color = color;
    }

}
