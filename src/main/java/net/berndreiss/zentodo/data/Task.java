package net.berndreiss.zentodo.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Class representing a task. A task is first dropped and can then be selected via PICK to be in FOCUS mode.
 * It can also be recurring and, therefore, automatically be added to the FOCUS mode.
 * Tasks will also have a unique position, meaning they can be sorted by the order of being added. These positions can
 * change by swapping them (see TaskManagerI.java). The same goes for list positions (see ListManagerI.java).
 */

@Entity
@Table(name = "tasks")
public class Task {

    /**
     * The user for the task
     */
    @Column
    private long userId;

    /**
     * The user profile for the task
     */
    @Column
    private int profile = 0;

    /**
     * The id for the task
     */
    @Id
    private long id;//id generated in

    /**
     * The position of the task (unique value for all tasks in a profile)
     */
    @Column(nullable = false)
    private int position;

    /**
     * The literal task
     */
    @Column(nullable = false)
    private String task;//a description of the task that has to be done

    /**
     * True, when task is in FOCUS mode
     */
    @Column(nullable = false)
    private Boolean focus = false;//true if task has been chosen today

    /**
     * True, if task has been dropped
     */
    @Column(nullable = false)
    private Boolean dropped = true;//true if task has been dropped and not been used in brainstorm and pick

    /**
     * Optional list of the task
     */
    @Column
    private Long list = null;//a list to which the task belongs to

    /**
     * If list is set, the position in the list (unique for all tasks in a list)
     */
    @Column
    private Integer listPosition = null;//position in according list

    /**
     * The reminder date for the task
     */
    @Column
    private Instant reminderDate = null;//a date, when the task is due -> "yyyyMMdd"

    /**
     * The recurrence for a task consisting of a String in the form of "y/m/w/d0-90-9" where the two digit number defines
     * the offset in years (y), months(m), weeks (w) or days(d) when the task is going to reoccur
     */
    @Column
    private String recurrence = null;

    public Task() {
    }

    public Task(long userId, int profile, long id, String task, int position) {
        //creates a new instance and initializes the fields of the entry
        this.userId = userId;
        this.profile = profile;
        this.id = id;
        this.position = position;
        this.task = task;
    }

    public Task(long userId, int profile, String task, int position) {
        //creates a new instance and initializes the fields of the entry
        this.userId = userId;
        this.profile = profile;
        this.task = task;
        this.position = position;
    }

    //Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getProfile() {
        return profile;
    }

    public void setProfile(int profile) {
        this.profile = profile;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Boolean getFocus() {
        return focus;
    }

    public void setFocus(Boolean focus) {
        this.focus = focus;
    }

    public Boolean getDropped() {
        return dropped;
    }

    public void setDropped(Boolean dropped) {
        this.dropped = dropped;
    }

    public Long getList() {
        return list;
    }

    public void setList(Long list) {
        this.list = list;
    }

    public Integer getListPosition() {
        return listPosition;
    }

    public void setListPosition(Integer listPosition) {
        this.listPosition = listPosition;
    }

    public Instant getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(Instant reminderDate) {
        this.reminderDate = reminderDate;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }
}
