package net.berndreiss.zentodo.data;

import java.util.List;

public interface ListManagerI {


    /**
     * Update the list field and the position with the value provided and increment all list items list positions greater than the position.
     * If the old list is not null decrement all old list items positions greater than the old position.
     *
     * @param id the id of the task to be update
     * @param value the value to update with
     * @param position position in which to add the item
     */
    void updateList(long userId, long profile, long entryId, String value, int position);

    List<Entry> getList(long userId, long profile, String list);
    /**
     * Swap entry in list with entry at position.
     *
     * @param id the id of the entry to be moved
     * @param position the position with which to swap
     */
    void swapListEntries(long userId, long profile, long entryId, int position);


    /**
     *
     * Updates the given list with the color provided.
     *
     * @param list the list to change
     * @param color the color to put
     */
    void updateListColor(long userId, long profile, String list, String color);


}
