package net.berndreiss.zentodo.data;

import java.util.List;
import java.util.Optional;

public interface ListManagerI {


    /**
     * Update the list field and the position with the value provided and increment all list items list positions greater than the position.
     * If the old list is not null decrement all old list items positions greater than the old position.
     *
     */
    TaskList addList(long id, String name, String color) throws InvalidActionException;
    void addUserProfileToList(long userId, int profile, long list) throws InvalidActionException;
    void removeUserProfileFromList(long userId, int profile, long list);
    void  removeList(long id);
    void updateList(long userId, int profile, long entryId, Long listId);
    Long updateId(long list, long id);

    void updateListName(long listId, String name) throws InvalidActionException;
    void updateListColor(long listId, String color);
    List<Entry> getListEntries(long userId, int profile, Long listId);
    Optional<TaskList> getList(long id);
    Optional<TaskList> getListByName(long userId, int profile, String name);
    List<TaskList> getListsForUser(long userId, int profile);
    List<TaskList> getLists();
    /**
     * Swap entry in list with entry at position.
     *
     * @param position the position with which to swap
     */
    void swapListEntries(long userId, int profile, long list, long entryId, int position) throws PositionOutOfBoundException;



}
