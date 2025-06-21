package net.berndreiss.zentodo.operations;

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
 * This interface represents methods necessary to communicate with the server. If there are necessary side effects,
 * they are described in the methods documentation below. THESE SIDE EFFECTS ARE NECESSARY TO GUARANTEE CONSISTENCY.
 */
public interface OperationHandlerI {



    Task addNewTask(Task task) throws PositionOutOfBoundException, DuplicateIdException, InvalidActionException;

    Task addNewTask(String task);
    Task addNewTask(String task, int position) throws PositionOutOfBoundException;

    void removeTask(long id);

    Optional<Task> getTask(long id);

    List<Task> loadTasks();

    List<Task> loadFocus();

    /**
     * Load all tasks that have been dropped.
     * @return a list of dropped tasks
     */
    List<Task> loadDropped();

    /**
     * Load a task list.
     * @param list the list to load
     * @return the list
     */
    List<Task> loadList(Long list);

    /**
     * Get all task lists.
     * @return a list of lists
     */
    List<TaskList> loadLists();

    /**
     * Retrieve the colors of all lists as a map.
     * @return a map<list, color>
     */
    Map<Long, String> getListColors();

    //TODO How are we handling lists with duplicate names
    /**
     * Get a list by its name.
     * @param name the name to look for
     * @return the list
     */
    Optional<TaskList> getListByName(String name);

    /**
     * Swap the position of two tasks.
     * @param id the id of the task to be moved
     * @param position the position with which to swap
     * @throws PositionOutOfBoundException Thrown if the position is out of bounds.
     */
    void swapTasks(long id, int position) throws PositionOutOfBoundException;

    /**
     * Swap list entries.
     * @param list the list in question
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     * @throws PositionOutOfBoundException Thrown if the position is out of bounds
     */
    void swapListEntries(long list, long id, int position) throws PositionOutOfBoundException;

    /**
     * Update the name of a task.
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateTask(long id, String value);

    /**
     * Update the focus field of a task.
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateFocus(long id, boolean value);

    /**
     * Update the dropped field of a task.
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateDropped(long id, boolean value);

    /**
     * Update the list of a task.
     * @param id the id of the task to be updated
     * @param newId the new list to be set
     */
    void updateList(long id, Long newId);

    /**
     * Update the reminder date of a task.
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateReminderDate(long id, Instant value);

    /**
     * Update the recurrence of a task.
     * @param id the id of the task to be updated
     * @param value the value to update with -> has to be in the form "interval|number".
     *              Valid intervals are 'd', 'w', 'm', and 'y'; valid numbers are any positive integers
     */
    void updateRecurrence(long id, String value);

    /**
     * Update the color of a list.
     * @param list the list to change
     * @param color the color to use
     */
    void updateListColor(long list, String color);

    /**
     * Update the name of the initialized user.
     * @param name the new name for the user
     */
    void updateUserName(String name);

    /**
     * Update the email for the initialized user.
     * @param email new email for the user
     * @throws InvalidActionException thrown if the provided mail address is already in use
     * @throws IOException thrown if there is a problem communicating with the server
     */
    void updateEmail(String email) throws InvalidActionException, IOException, URISyntaxException;

}
