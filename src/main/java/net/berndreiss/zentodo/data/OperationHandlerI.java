package net.berndreiss.zentodo.data;

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



    Entry addNewEntry(Entry entry);

    Entry addNewEntry(String task) throws PositionOutOfBoundException;
    Entry addNewEntry(String task, int position) throws PositionOutOfBoundException;

    /**
     * Delete entry from database including the local queue(s). All entries with position greater than the deleted
     * entries position are decremented.
     *
     * @param id the id of the entry to be deleted
     */
    void removeEntry(long id);

    Optional<Entry> getEntry(long id);

    List<Entry> loadEntries();

    List<Entry> loadFocus();
    List<Entry> loadDropped();
    List<Entry> loadList(Long list);
    List<TaskList> loadLists();
    Map<Long, String> getListColors();
    Optional<TaskList> getListByName(String name);

    /**
     * Swap entry with id with the entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapEntries(long id, int position) throws PositionOutOfBoundException;

    /**
     * Swap entry in list with entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapListEntries(long list, long id, int position) throws PositionOutOfBoundException;

    /**
     * Update the task with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateTask(long id, String value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateFocus(long id, boolean value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateDropped(long id, boolean value);

    /**
     * Update the list field and the position with the value provided and increment all list items list positions greater than the position.
     * If the old list is not null decrement all old list items positions greater than the old position.
     *
     * @param id the id of the task to be updated
     */
    void updateList(long id, Long newId);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateReminderDate(long id, Instant value);

    /**
     * Update the field with the value provided. Also needs to update the reminder date.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateRecurrence(long id, String value);

    /**
     *
     * Updates the given list with the color provided.
     *
     * @param list the list to change
     * @param color the color to put
     */
    void updateListColor(long list, String color);

    /**
     * TODO
     * @param name
     */
    void updateUserName(String name);

    /**
     * TODO
     * @param email
     */
    void updateEmail(String email) throws InvalidActionException, IOException, URISyntaxException;

}
