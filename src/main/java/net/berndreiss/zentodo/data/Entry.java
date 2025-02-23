package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * TODO ADD DESCRIPTION
 */

@Entity
@Table(name = "entries")
public class Entry{

    @Column
    private Long user_id = null;

    @Id
    private long id;//id generated in

    @Column (nullable = false)
    private int position;

    @Column (nullable = false)
    private String task;//a description of the task that has to be done

    @Column (nullable = false)
    private Boolean focus = false;//true if task has been chosen today

    @Column (nullable = false)
    private Boolean dropped = true;//true if task has been dropped and not been used in brainstorm and pick

    @Column
    private String list = null;//a list to which the task belongs to

    @Column
    private Integer listPosition = null;//position in according list

    @Column
    private LocalDate reminderDate = null;//a date, when the task is due -> "yyyyMMdd"

    @Column
    private String recurrence = null;//consisting of a String in the form of "y/m/w/d0-90-9" where the
                              //two digit number defines the offset in years (y), months(m),
                              //weeks (w) or days(d) when the task is going to reoccur

    public Entry(){}

    public Entry(long id, int position, String task){
        //creates a new instance and initializes the fields of the entry
        this.id = id;
        this.position = position;
        this.task=task;
    }

    public Entry(int position, String task){
        //creates a new instance and initializes the fields of the entry
        this.position = position;
        this.task=task;
    }

    //the following functions simply return the different fields of the entry
    public long getId(){return this.id;}

    public int getPosition(){return this.position;}

    public String getTask(){return task;}

    public Boolean getFocus(){return focus;}

    public Boolean getDropped(){return dropped;}

    public String getList(){return list;}

    public Integer getListPosition(){return listPosition;}

    public LocalDate getReminderDate(){return reminderDate;}

    public String getRecurrence(){return recurrence;}

    //The following functions are to update the different fields
    public void setPosition(int position){this.position = position;}

    public void setTask(String task){this.task=task;}

    public void setFocus(Boolean focus){this.focus = focus;}

    public void setDropped(Boolean dropped){this.dropped = dropped;}

    public void setList(String list){this.list=list;}

    public void setListPosition(Integer listPosition){this.listPosition=listPosition;}

    public void setReminderDate(LocalDate reminderDate){this.reminderDate=reminderDate;}

    public void setRecurrence(String recurrence){this.recurrence=recurrence;}

}
