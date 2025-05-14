package net.berndreiss.zentodo.data;

import java.util.List;
import java.util.Optional;

public interface EntryManagerI {

    /**
     * Add a list of entries to the database including all the fields in the entry.
     *
     * @param entries the entries to be added
     */
    void post(List<Entry> entries);

    /**
     * TODO DESCRIBE
     * @param entry
     * @param id
     */
    void updateId(Long userId, long profile, long entry, long id);

    /**
     * TODO DESCRIBE
     * @param delay
     */
 ;



    Entry addNewEntry(Long userId, long profile, String task);
    /**
     * Add a single new task to the database.
     * @param id the id of the new task as provided by the server
     * @param task the task to be associated with the entry
     */
    Entry addNewEntry(Long userId, long profile, String task, int position);

    /**
     * Add a single new task to the database.
     * @param id the id of the new task as provided by the server
     * @param task the task to be associated with the entry
     */
    Entry addNewEntry(Long userId, long profile, long id, String task, int position);

    /**
     * Delete entry from database including the local queue(s). All entries with position greater than the deleted
     * entries position are decremented.
     *
     * @param id the id of the entry to be deleted
     */
    void removeEntry(Long userId, long profile, long id);

    /**
     * Swap entry with id with the entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapEntries(Long userId, long profile, long id, int position);

    /**
     * Swap entry in list with entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapListEntries(Long userId, long profile, long id, int position);

    /**
     * Update the task with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateTask(Long userId, long profile, long id, String value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateFocus(Long userId, long profile, long id, int value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateDropped(Long userId, long profile, long id, int value);

    /**
     * Update the list field and the position with the value provided and increment all list items list positions greater than the position.
     * If the old list is not null decrement all old list items positions greater than the old position.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     * @param position position in which to add the item
     */
    void updateList(Long userId, long profile, long id, String value, int position);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateReminderDate(Long userId, long profile, long id, Long value);

    /**
     * Update the field with the value provided. Also needs to update the reminder date.
     *
     * @param id the id of the task to be updated
     * @param reminderDate the reminder date of the task
     * @param value the value to update with
     */
    void updateRecurrence(Long userId, long profile, long id, Long reminderDate, String value);

    /**
     *
     * Updates the given list with the color provided.
     *
     * @param list the list to change
     * @param color the color to put
     */
    void updateListColor(Long userId, long profile, String list, String color);



    Optional<Entry> getEntry(Long userId, long profile, long id);
    List<Entry> getEntries(Long userId, long profile);
}
