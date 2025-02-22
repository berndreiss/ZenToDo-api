package net.berndreiss.zentodo.data;

import java.time.LocalDate;

/**
 * TODO ADD DESCRIPTION
 */

public class Entry{

    private final int id;//id generated in
    private int position;
    private String task;//a description of the task that has to be done
    private Boolean focus;//true if task has been chosen today
    private Boolean dropped;//true if task has been dropped and not been used in brainstorm and pick
    private String list;//a list to which the task belongs to
    private Integer listPosition;//position in according list
    private LocalDate reminderDate;//a date, when the task is due -> "yyyyMMdd"
    private String recurrence;//consisting of a String in the form of "y/m/w/d0-90-9" where the
                              //two digit number defines the offset in years (y), months(m),
                              //weeks (w) or days(d) when the task is going to reoccur

    public Entry(int id, int position, String task){
        //creates a new instance and initializes the fields of the entry
        this.id = id;
        this.position = position;
        this.task=task;
        this.dropped = true;
        this.focus = false;
        this.listPosition=null;
        this.reminderDate = null;
        this.recurrence = null;
    }

    //the following functions simply return the different fields of the entry
    public int getId(){return this.id;}

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
