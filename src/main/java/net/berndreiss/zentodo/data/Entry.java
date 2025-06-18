package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * TODO ADD DESCRIPTION
 */

@Entity
@Table(name = "entries")
public class Entry{

    @Column
    private long userId;

    @Column
    private int profile = 0;

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
    private Long list = null;//a list to which the task belongs to

    @Column
    private Integer listPosition = null;//position in according list

    @Column
    private Instant reminderDate = null;//a date, when the task is due -> "yyyyMMdd"

    @Column
    private String recurrence = null;//consisting of a String in the form of "y/m/w/d0-90-9" where the
                              //two digit number defines the offset in years (y), months(m),
                              //weeks (w) or days(d) when the task is going to reoccur

    public Entry(){}

    public Entry(long userId, int profile, long id, String task, int position){
        //creates a new instance and initializes the fields of the entry
        this.userId = userId;
        this.profile = profile;
        this.id = id;
        this.position = position;
        this.task=task;
    }

    public Entry(long userId, int profile, String task, int position){
        //creates a new instance and initializes the fields of the entry
        this.userId = userId;
        this.profile = profile;
        this.task=task;
        this.position = position;
    }

    public Long getUserId() {return userId;}

    public int getProfile() {return profile;}

    public void setProfile(int profile) {this.profile = profile;}

    public void setUserId(Long userId) {this.userId = userId;}

    public void setId(long id){this.id = id;}

    //the following functions simply return the different fields of the entry
    public long getId(){return this.id;}

    public int getPosition(){return this.position;}

    public String getTask(){return task;}

    public Boolean getFocus(){return focus;}

    public Boolean getDropped(){return dropped;}

    public Long getList(){return list;}

    public Integer getListPosition(){return listPosition;}

    public Instant getReminderDate(){return reminderDate;}

    public String getRecurrence(){return recurrence;}

    //The following functions are to update the different fields
    public void setPosition(int position){this.position = position;}

    public void setTask(String task){this.task=task;}

    public void setFocus(Boolean focus){this.focus = focus;}

    public void setDropped(Boolean dropped){this.dropped = dropped;}

    public void setList(Long list){this.list=list;}

    public void setListPosition(Integer listPosition){this.listPosition=listPosition;}

    public void setReminderDate(Instant reminderDate){this.reminderDate=reminderDate;}

    public void setRecurrence(String recurrence){this.recurrence=recurrence;}

}
