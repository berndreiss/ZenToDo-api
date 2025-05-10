package net.berndreiss.zentodo.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This interface represents methods necessary to communicate with the server. If there are necessary side effects,
 * they are described in the methods documentation below. THESE SIDE EFFECTS ARE NECESSARY TO GUARANTEE CONSISTENCY.
 */
public interface OperationHandler {



    Entry addNewEntry(Entry entry);

    Entry addNewEntry(String task);
    Entry addNewEntry(String task, int position);

    /**
     * Delete entry from database including the local queue(s). All entries with position greater than the deleted
     * entries position are decremented.
     *
     * @param id the id of the entry to be deleted
     */
    void delete(long id);

    Optional<Entry> getEntry(long id);

    List<Entry> loadEntries();

    List<Entry> loadFocus();
    List<Entry> loadDropped();
    List<Entry> loadList(String list);
    List<String> loadLists();
    Map<String, String> getListColors();

    /**
     * Swap entry with id with the entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapEntries(long id, int position);

    /**
     * Swap entry in list with entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapListEntries(long id, int position);

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
    void updateFocus(long id, int value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateDropped(long id, int value);

    /**
     * Update the list field and the position with the value provided and increment all list items list positions greater than the position.
     * If the old list is not null decrement all old list items positions greater than the old position.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     * @param position position in which to add the item
     */
    void updateList(long id, String value, int position);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateReminderDate(long id, Long value);

    /**
     * Update the field with the value provided. Also needs to update the reminder date.
     *
     * @param id the id of the task to be updated
     * @param reminderDate the reminder date of the task
     * @param value the value to update with
     */
    void updateRecurrence(long id, Long reminderDate, String value);

    /**
     *
     * Updates the given list with the color provided.
     *
     * @param list the list to change
     * @param color the color to put
     */
    void updateListColor(String list, String color);

    /**
     * TODO
     * @param id
     * @param name
     */
    void updateUserName(long id, String name);

    /**
     * TODO
     * @param id
     * @param email
     */
    boolean updateEmail(long id, String email);

}
