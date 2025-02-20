package net.berndreiss.zentodo.api;

import java.util.List;

/**
 * This interface represents methods necessary to communicate with the server. If there are necessary side effects,
 * they are described in the methods documentation below. THESE SIDE EFFECTS ARE NECESSARY TO GUARANTEE CONSISTENCY.
 */
public interface Database {

    /**
     * Add a list of entries to the database including all the fields in the entry.
     *
     * @param entries the entries to be added
     */
    void post(List<Entry> entries);


    /**
     * Add a single new task to the database.
     * @param id the id of the new task as provided by the server
     * @param task the task to be associated with the entry
     */
    void addNewEntry(int id, String task);

    /**
     * Delete entry from database including the local queue(s). All entries with position greater than the deleted
     * entries position are decremented.
     *
     * @param id the id of the entry to be deleted
     */
    void delete(int id);

    /**
     * Swap entry with id with the entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapEntries(int id, int position);

    /**
     * Swap entry in list with entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapListEntries(int id, int position);

    /**
     * Update the task with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateTask(int id, String value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateFocus(int id, int value);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateDropped(int id, int value);

    /**
     * Update the list field and the position with the value provided and increment all list items list positions greater than the position.
     * If the old list is not null decrement all old list items positions greater than the old position.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     * @param position position in which to add the item
     */
    void updateList(int id, String value, int position);

    /**
     * Update the field with the value provided.
     *
     * @param id the id of the task to be updated
     * @param value the value to update with
     */
    void updateReminderDate(int id, Long value);

    /**
     * Update the field with the value provided. Also needs to update the reminder date.
     *
     * @param id the id of the task to be updated
     * @param reminderDate the reminder date of the task
     * @param value the value to update with
     */
    void updateRecurrence(int id, Long reminderDate, String value);

    /**
     *
     * Updates the given list with the color provided.
     *
     * @param list the list to change
     * @param color the color to put
     */
    void updateListColor(String list, String color);

}
