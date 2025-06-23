package net.berndreiss.zentodo.data;

import com.sun.istack.NotNull;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;

import java.util.List;
import java.util.Optional;

/**
 * Methods for persisting lists.
 * THERE MAY BE NECESSARY SIDE EFFECTS OF OPERATIONS. IF SO THEY ARE DESCRIBED IN THE COMMENTS.
 */
public interface ListManagerI {


    /**
     * Add a new list.
     *   - the id is globally unique and needs to be retrieved from the server
     * @param id the list id
     * @param name the name of the list
     * @param color an optional color for the list (as HEX)
     * @return a new TaskList
     * @throws InvalidActionException thrown when trying to add a nameless list
     */
    TaskList addList(long id, @NotNull String name, String color) throws InvalidActionException;

    /**
     * Add a user profile to a list.
     * @param user the user id
     * @param profile the profile id
     * @param list the list id
     * @throws InvalidActionException thrown if profile already contains a list with the same name
     */
    void addUserProfileToList(long user, int profile, long list) throws InvalidActionException;

    /**
     * Remove a user profile from a list.
     * @param user the user id
     * @param profile the profile id
     * @param list the list id
     */
    void removeUserProfileFromList(long user, int profile, long list);

    /**
     * Remove a list.
     * @param id the list id
     */
    void  removeList(long id);

    /**
     * Update the list of a task.
     *   - needs to set the appropriate list position to last in the list.
     *   - if the old list is not null decrement all tasks in the old list with positions greater than the old position.
     * @param user the user id
     * @param profile the profile id
     * @param task the task id
     * @param listId the new list id
     */
    void updateList(long user, int profile, long task, Long listId);

    /**
     * Update the id of a list.
     * @param list the list id
     * @param newId the new id
     * throws DuplicateIdException thrown when the new id already exists
     */
    void updateId(long list, long newId) throws DuplicateIdException;

    /**
     * Update the name of a list.
     * @param list the list id
     * @param name the new name
     * @throws InvalidActionException thrown when trying to add a nameless list
     */
    void updateListName(long list, @NotNull String name) throws InvalidActionException;

    /**
     * Update the list color.
     * @param list the list id
     * @param color the new color (in HEX)
     */
    void updateListColor(long list, String color);

    /**
     * Get all tasks for a list.
     * @param user the user id
     * @param profile the profile id
     * @param list the list id
     * @return a list of tasks in the list
     */
    List<Task> getListEntries(long user, int profile, Long list);

    /**
     * Get a task list.
     * @param id the list id
     * @return the task list
     */
    Optional<TaskList> getList(long id);

    /**
     * Get a list by name.
     * @param user the user id
     * @param profile the profile id
     * @param name the name of the list
     * @return the task list, if it exists
     */
    Optional<TaskList> getListByName(long user, int profile, String name);

    /**
     * Get all lists for the user profile.
     * @param user the user id
     * @param profile the profile id
     * @return a list of task lists for the user profile
     */
    List<TaskList> getListsForUser(long user, int profile);

    /**
     * Get all existing lists.
     * @return a list of existing task lists
     */
    List<TaskList> getLists();

    /**
     * Swap entry in list with entry at position.
     * @param user the user id
     * @param profile the profile id
     * @param list the list id
     * @param task the task id to move
     * @param position the position to swap with
     * @throws PositionOutOfBoundException thrown when position is not valid
     */
    void swapListEntries(long user, int profile, long list, long task, int position) throws PositionOutOfBoundException;



}
