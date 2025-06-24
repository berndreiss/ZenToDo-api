package net.berndreiss.zentodo.operations;

import com.sun.istack.NotNull;
import net.berndreiss.zentodo.data.Task;
import net.berndreiss.zentodo.data.TaskList;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This interface represents methods for the user profile to interact with the data. All actions are synchronized with the server.
 */
public interface OperationHandlerI {

    /**
     * Add a new task.
     *
     * @param task the task to be added
     * @throws PositionOutOfBoundException thrown if position of task is greater than the number of tasks overall
     * @throws DuplicateIdException        thrown if id already exists
     * @throws InvalidActionException      thrown if id==0
     */
    void addNewTask(Task task) throws PositionOutOfBoundException, DuplicateIdException, InvalidActionException;

    /**
     * Add a new task.
     *
     * @param task the task to be added
     * @return a new Task with a unique id and a position set
     */
    Task addNewTask(String task);

    /**
     * Add a new task at a certain position.
     *
     * @param task     the task to be added
     * @param position the position to add it at
     * @return a new Task with the position set and a unique id
     * @throws PositionOutOfBoundException thrown if the position is not valid
     */
    Task addNewTask(String task, int position) throws PositionOutOfBoundException;

    /**
     * Remove a task.
     *
     * @param id the id of the task to be removed
     */
    void removeTask(long id);

    /**
     * Get a task.
     *
     * @param id the id of the task
     * @return the task, if it exists
     */
    Optional<Task> getTask(long id);

    /**
     * Get all tasks for the user profile.
     *
     * @return a list of all tasks for the profile
     */
    List<Task> loadTasks();

    /**
     * Get all tasks that are selected for the FOCUS mode.
     *
     * @return a list of tasks in FOCUS mode
     */
    List<Task> loadFocus();

    /**
     * Load all tasks that have been dropped.
     *
     * @return a list of dropped tasks
     */
    List<Task> loadDropped();

    /**
     * Load all tasks associated with a list.
     *
     * @param list the list to load
     * @return the list
     */
    List<Task> loadList(Long list);

    /**
     * Get all task lists.
     *
     * @return a list of lists
     */
    List<TaskList> loadLists();

    /**
     * Retrieve the colors of all lists as a map.
     *
     * @return a map<list, color>
     */
    Map<Long, String> getListColors();

    //TODO How are we handling lists with duplicate names

    /**
     * Get a list by its name.
     *
     * @param name the name to look for
     * @return the list
     */
    Optional<TaskList> getListByName(String name);

    /**
     * Swap the position of two tasks.
     *
     * @param task       the id of the task to be moved
     * @param position the position with which to swap
     * @throws PositionOutOfBoundException Thrown if the position is out of bounds.
     */
    void swapTasks(long task, int position) throws PositionOutOfBoundException;

    /**
     * Swap list entries.
     *
     * @param list     the list in question
     * @param task       the id of the entry to be moved
     * @param position the position with which to swap
     * @throws PositionOutOfBoundException Thrown if the position is out of bounds
     */
    void swapListEntries(long list, long task, int position) throws PositionOutOfBoundException;

    /**
     * Update the name of a task.
     *
     * @param task    the id of the task to be updated
     * @param value the value to update with
     */
    void updateTask(long task, String value);

    /**
     * Update the focus field of a task.
     *
     * @param task    the id of the task to be updated
     * @param value the value to update with
     */
    void updateFocus(long task, boolean value);

    /**
     * Update the dropped field of a task.
     *
     * @param task    the id of the task to be updated
     * @param value the value to update with
     */
    void updateDropped(long task, boolean value);

    /**
     * Update the list of a task.
     *   -needs to set the list position to the last positon in the new list also (or null if list is null)
     *   -needs to decrement all other tasks positions in the list with position greater than task
     * @param task    the id of the task to be updated
     * @param newId the new list to be set
     */
    void updateList(long task, Long newId);

    /**
     * Update the reminder date of a task.
     *
     * @param task    the id of the task to be updated
     * @param value the value to update with
     */
    void updateReminderDate(long task, Instant value);

    /**
     * Update the recurrence of a task.
     *
     * @param task    the id of the task to be updated
     * @param value the value to update with -> has to be in the form "interval|number".
     *              Valid intervals are 'd', 'w', 'm', and 'y'; valid numbers are any positive integers
     */
    void updateRecurrence(long task, String value);

    /**
     * Update the color of a list.
     *
     * @param list  the list to change
     * @param color the color to use
     */
    void updateListColor(long list, String color);

    /**
     * Update the name of the initialized user.
     *
     * @param name the new name for the user
     */
    void updateUserName(String name);

    /**
     * Update the email for the initialized user.
     *
     * @param email new email for the user
     * @throws InvalidActionException thrown if the provided mail address is already in use
     * @throws IOException            thrown if there is a problem communicating with the server
     */
    void updateEmail(String email) throws InvalidActionException, IOException, URISyntaxException;

    /**
     * Get the list for the user profile active.
     *
     * @return the list of lists for the user
     */
    List<TaskList> getLists();

    /**
     * Add a new list.
     * @param name the name of the list
     * @param color the optional color of the list
     * @return the new list
     * throws InvalidActionException thrown when name already exists the user profile
     */
    TaskList addNewList(@NotNull String name, String color) throws InvalidActionException, DuplicateIdException;
}
